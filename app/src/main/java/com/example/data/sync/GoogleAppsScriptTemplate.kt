package com.example.data.sync

object GoogleAppsScriptTemplate {
    const val SCRIPT_CODE = """/**
 * ASTILLA POS - GOOGLE APPS SCRIPT WEB APP BACKEND (Code.gs)
 * 
 * FEATURES:
 * 1. Idempotency Check: Logs transaction_id (UUID) in 'Processed_Transactions' sheet.
 *    Duplicate transaction IDs are skipped to avoid duplicate revenue & inventory deductions,
 *    and returned in processed_transaction_ids.
 * 2. Atomic Inventory Deductions: Decrements product stock dynamically on 'Inventory' sheet
 *    (S_new = S_current - Q_sold) using barcode/sku match.
 * 3. Concurrency Protection: LockService.getScriptLock() prevents race conditions.
 * 4. Delta Catalog Sync: Fetches items modified/updated after last_synced_at.
 * 5. Multi-format support: Supports both POST transaction sync (/api/v1/sync/transactions format)
 *    and legacy POS sync actions.
 * 
 * SETUP INSTRUCTIONS:
 * 1. In your Google Sheet, open Extensions > Apps Script.
 * 2. Replace all code in Code.gs with this script.
 * 3. Click 'Deploy' > 'New deployment'.
 * 4. Select type 'Web app'.
 * 5. Set 'Execute as' -> 'Me', 'Who has access' -> 'Anyone'.
 * 6. Authorize permissions and copy the Web App URL into Astilla POS Settings.
 */

function doPost(e) {
  var lock = LockService.getScriptLock();
  var hasLock = false;
  try {
    hasLock = lock.tryLock(30000);
    if (!hasLock) {
      return ContentService.createTextOutput(JSON.stringify({
        status: "error",
        message: "Server busy, could not acquire sync lock. Please retry."
      })).setMimeType(ContentService.MimeType.JSON);
    }

    var rawData = (e && e.postData && e.postData.contents) ? e.postData.contents : "{}";
    var body = {};
    try {
      body = JSON.parse(rawData);
    } catch (parseErr) {
      body = {};
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    if (!ss) {
      return ContentService.createTextOutput(JSON.stringify({
        status: "error",
        message: "No active spreadsheet linked to this script. Ensure the script is bound to a Google Sheet."
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // 1. Direct pull request handling
    if (body.action === "pull") {
      return pullDataResponse(ss);
    }

    // 2. Direct single INVENTORY append/update event
    if (body.type === "INVENTORY" || body.action === "SYNC_INVENTORY") {
      var invSheetSingle = getOrCreateInventorySheet(ss);
      upsertProductRow(invSheetSingle, {
        barcode: body.barcode || "",
        name: body.productName || body.name || "",
        category: body.category || "General",
        costPrice: Number(body.costPrice) || 0.0,
        retailPrice: Number(body.retailPrice) || 0.0,
        stockQuantity: Number(body.stockQuantity) || 0,
        updatedAt: body.updatedAt || new Date().toISOString()
      });

      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        message: "Inventory recorded"
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // 3. Utang (Credit Ledger) Actions
    var action = (body.action || body.type || "").toString().trim().toUpperCase();
    if (action === "SYNC_CUSTOMER") {
      syncCustomer(ss, body.customer || body);
      return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS" })).setMimeType(ContentService.MimeType.JSON);
    }

    if (action === "LOG_UTANG_TRANSACTION") {
      logUtangTransaction(ss, body);
      return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS" })).setMimeType(ContentService.MimeType.JSON);
    }

    if (action === "LOG_UTANG_PAYMENT") {
      logUtangPayment(ss, body);
      return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS" })).setMimeType(ContentService.MimeType.JSON);
    }

    // 4. Transactions sync endpoint (Standard /api/v1/sync/transactions spec & ASTILLA POS batch sync)
    var deviceId = body.device_id || body.deviceId || "pos-device";
    var lastSyncedAt = body.last_synced_at || body.lastSyncedAt || null;
    var rawTransactions = body.transactions || body.sales || [];

    // Ensure core sheets exist
    var processedSheet = getOrCreateProcessedTransactionsSheet(ss);
    var inventorySheet = getOrCreateInventorySheet(ss);
    var salesLogSheet = getOrCreateSheet(ss, "POS_Sales", [
      "Transaction ID", "Date & Time", "Total Amount", "Tax Amount", "Payment Type", "Items Summary", "Device ID", "Synced At"
    ]);

    // Cache existing processed UUIDs for quick O(1) lookup
    var existingProcessedIds = getExistingProcessedTransactionIds(processedSheet);

    var processedTransactionIds = [];
    var newTransactionsApplied = 0;
    var currentServerTime = new Date().toISOString();

    // Inventory stock map for atomic deduction: barcode -> { row, currentStock }
    var inventoryMap = buildInventoryMap(inventorySheet);

    for (var i = 0; i < rawTransactions.length; i++) {
      var tx = rawTransactions[i];
      // Normalize transaction ID (transaction_id, transactionId, id, or fallback UUID)
      var txId = (tx.transaction_id || tx.transactionId || tx.id || "").toString().trim();
      if (!txId) {
        txId = Utilities.getUuid();
      }

      // 1. Idempotency Check
      if (existingProcessedIds[txId]) {
        // Already processed, skip deductions and sale append to avoid duplicate revenue/stock loss
        if (processedTransactionIds.indexOf(txId) === -1) {
          processedTransactionIds.push(txId);
        }
        continue;
      }

      // 2. Mark transaction as processed
      existingProcessedIds[txId] = true;
      processedTransactionIds.push(txId);
      newTransactionsApplied++;

      // Log to Processed_Transactions sheet
      processedSheet.appendRow([
        txId,
        currentServerTime,
        deviceId,
        Number(tx.total_amount || tx.totalAmount) || 0.0
      ]);

      // 3. Deduct sold quantities from inventory atomically (S_new = S_current - Q_sold)
      var items = tx.items || [];
      // Handle items summary string or structured array
      if (Array.isArray(items) && items.length > 0) {
        for (var j = 0; j < items.length; j++) {
          var item = items[j];
          var barcode = (item.barcode || item.sku || "").toString().trim();
          var qSold = Number(item.quantity || item.quantity_sold || item.qty) || 0;
          if (barcode && qSold > 0) {
            deductInventoryStock(inventorySheet, inventoryMap, barcode, qSold, item);
          }
        }
      }

      // 4. Record sale in POS_Sales log
      salesLogSheet.appendRow([
        txId,
        tx.timestamp ? (new Date(tx.timestamp).toISOString()) : currentServerTime,
        Number(tx.total_amount || tx.totalAmount) || 0.0,
        Number(tx.tax_amount || tx.taxAmount) || 0.0,
        tx.payment_type || tx.paymentType || "Cash",
        tx.itemsSummary || (Array.isArray(items) ? items.map(function(it) {
          return (it.quantity || 1) + "x " + (it.productName || it.name || it.barcode);
        }).join("; ") : ""),
        deviceId,
        currentServerTime
      ]);
    }

    // Process Deliveries / Restock Tab if provided
    if (body.deliveries && body.deliveries.length > 0) {
      var delSheet = getOrCreateSheet(ss, "POS_Deliveries", [
        "Delivery ID", "Date & Time", "Supplier Name", "Total Cost", "Notes", "Synced At"
      ]);
      body.deliveries.forEach(function(d) {
        delSheet.appendRow([
          d.id || Utilities.getUuid(),
          d.dateString || (d.timestamp ? new Date(d.timestamp).toLocaleString() : currentServerTime),
          d.supplierName || "",
          Number(d.totalCost) || 0.0,
          d.notes || "",
          currentServerTime
        ]);
      });
    }

    // Process full inventory snapshot upload if provided
    if (body.inventory && body.inventory.length > 0) {
      syncInventorySnapshot(inventorySheet, body.inventory);
    }

    // Process Customers Master Summary if provided
    if (body.customers && body.customers.length > 0) {
      var customerSheet = getOrCreateSheet(ss, "Customers", [
        "Customer ID", "Customer Name", "Phone Number", "Current Balance", "Last Updated"
      ]);
      for (var cIdx = 0; cIdx < body.customers.length; cIdx++) {
        upsertCustomerRow(customerSheet, body.customers[cIdx]);
      }
    }

    // Process Utang Transactions (Borrowing History) if provided
    var rawUtangTxs = body.utangTransactions || body.utang_transactions || [];
    if (rawUtangTxs.length > 0) {
      var utangSheet = getOrCreateSheet(ss, "Utang_Transactions", [
        "Customer Name", "Transaction Number", "Goods Borrowed", "Total Amount", "Date & Time"
      ]);
      for (var uIdx = 0; uIdx < rawUtangTxs.length; uIdx++) {
        var uTx = rawUtangTxs[uIdx];
        var custName = uTx.customerName || uTx.name || "Customer";
        var txNum = uTx.transactionNumber || uTx.transactionId || uTx.id || ("TX-" + Utilities.getUuid());
        var gBorrowed = uTx.goodsBorrowed || uTx.itemsSummary || uTx.itemSummary || "N/A";
        var totAmt = Number(uTx.totalAmount || uTx.amountBorrowed || uTx.amount) || 0.0;
        var txTime = uTx.timestamp || currentServerTime;

        utangSheet.appendRow([
          custName,
          txNum,
          gBorrowed,
          totAmt,
          txTime
        ]);
        if (uTx.customerId && uTx.remainingBalance !== undefined && uTx.remainingBalance !== null) {
          syncCustomer(ss, {
            customerId: uTx.customerId,
            name: custName,
            phone: uTx.phoneNumber || uTx.phone || "",
            currentBalance: Number(uTx.remainingBalance),
            updatedAt: txTime
          });
        }
      }
    }

    // Process Payment Logs (Repayment History) if provided
    var rawPaymentLogs = body.utangPayments || body.utang_payments || [];
    if (rawPaymentLogs.length > 0) {
      var paymentSheet = getOrCreateSheet(ss, "Payment_Logs", [
        "Customer Name", "Payment ID", "Amount Paid", "Remaining Balance", "Date & Time"
      ]);
      for (var pIdx = 0; pIdx < rawPaymentLogs.length; pIdx++) {
        var pLog = rawPaymentLogs[pIdx];
        var pCustName = pLog.customerName || pLog.name || "Customer";
        var pId = pLog.paymentId || pLog.paymentNumber || pLog.id || ("PAY-" + Utilities.getUuid());
        var pAmt = Number(pLog.amountPaid || pLog.totalAmount || pLog.amount) || 0.0;
        var remBal = (pLog.remainingBalance !== undefined && pLog.remainingBalance !== null) ? Number(pLog.remainingBalance) : 0.0;
        var pTime = pLog.timestamp || currentServerTime;

        paymentSheet.appendRow([
          pCustName,
          pId,
          pAmt,
          remBal,
          pTime
        ]);
        if (pLog.customerId) {
          syncCustomer(ss, {
            customerId: pLog.customerId,
            name: pCustName,
            phone: pLog.phoneNumber || pLog.phone || "",
            currentBalance: remBal,
            updatedAt: pTime
          });
        }
      }
    }

    // 5. Delta Catalog Fetch: Query items updated or modified after last_synced_at
    var updatedCatalog = getDeltaCatalog(inventorySheet, lastSyncedAt);

    var responsePayload = {
      status: "success",
      synced_at: currentServerTime,
      processed_transaction_ids: processedTransactionIds,
      new_transactions_count: newTransactionsApplied,
      updated_catalog: updatedCatalog
    };

    return ContentService.createTextOutput(JSON.stringify(responsePayload))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: err.toString(),
      stack: err.stack
    })).setMimeType(ContentService.MimeType.JSON);
  } finally {
    if (hasLock) {
      try {
        lock.releaseLock();
      } catch (_) {}
    }
  }
}

function doGet(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    if (!ss) {
      return ContentService.createTextOutput(JSON.stringify({
        status: "error",
        message: "No active spreadsheet linked to this script. Ensure the script is bound to a Google Sheet."
      })).setMimeType(ContentService.MimeType.JSON);
    }
    var params = (e && e.parameter) ? e.parameter : {};
    var action = params.action || "";

    if (action === "pull" || action === "inventory" || action === "catalog") {
      var lastSyncedAt = params.last_synced_at || params.lastSyncedAt || null;
      var invSheet = getOrCreateInventorySheet(ss);
      var catalog = getDeltaCatalog(invSheet, lastSyncedAt);
      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        synced_at: new Date().toISOString(),
        products: catalog,
        updated_catalog: catalog,
        totalProducts: catalog.length
      })).setMimeType(ContentService.MimeType.JSON);
    }

    return ContentService.createTextOutput(JSON.stringify({
      status: "online",
      service: "Astilla POS Google Sheets Sync Engine",
      version: "2.0-atomic-idempotent",
      timestamp: new Date().toISOString()
    })).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

// -------------------------------------------------------------
// HELPER FUNCTIONS & ATOMIC INVENTORY LOGIC
// -------------------------------------------------------------

function getOrCreateProcessedTransactionsSheet(ss) {
  var sheet = ss.getSheetByName("Processed_Transactions");
  if (!sheet) {
    sheet = ss.insertSheet("Processed_Transactions");
    sheet.appendRow(["Transaction ID (UUID)", "Processed At", "Device ID", "Total Amount"]);
    sheet.getRange(1, 1, 1, 4).setFontWeight("bold").setBackground("#e2e8f0");
  }
  return sheet;
}

function getExistingProcessedTransactionIds(processedSheet) {
  var map = {};
  var lastRow = processedSheet.getLastRow();
  if (lastRow > 1) {
    var values = processedSheet.getRange(2, 1, lastRow - 1, 1).getValues();
    for (var i = 0; i < values.length; i++) {
      var id = (values[i][0] || "").toString().trim();
      if (id) {
        map[id] = true;
      }
    }
  }
  return map;
}

function getOrCreateInventorySheet(ss) {
  var sheet = ss.getSheetByName("Inventory");
  if (!sheet) {
    // If POS_Inventory exists from earlier versions, reuse or create Inventory
    var oldSheet = ss.getSheetByName("POS_Inventory");
    if (oldSheet) {
      sheet = oldSheet;
      sheet.setName("Inventory");
    } else {
      sheet = ss.insertSheet("Inventory");
    }
  }
  if (sheet.getLastRow() === 0) {
    sheet.appendRow([
      "Last Updated",
      "Barcode / SKU",
      "Product Name",
      "Category",
      "Stock Quantity",
      "Cost Price",
      "Retail Price"
    ]);
    sheet.getRange(1, 1, 1, 7).setFontWeight("bold").setBackground("#f1f5f9");
  }
  return sheet;
}

function buildInventoryMap(inventorySheet) {
  var map = {};
  var lastRow = inventorySheet.getLastRow();
  if (lastRow > 1) {
    var values = inventorySheet.getRange(2, 1, lastRow - 1, 7).getValues();
    for (var i = 0; i < values.length; i++) {
      var rowNumber = i + 2;
      var barcode = (values[i][1] || "").toString().trim();
      if (barcode) {
        map[barcode] = {
          row: rowNumber,
          lastUpdated: values[i][0],
          barcode: barcode,
          productName: values[i][2],
          category: values[i][3],
          currentStock: Number(values[i][4]) || 0,
          costPrice: Number(values[i][5]) || 0.0,
          retailPrice: Number(values[i][6]) || 0.0
        };
      }
    }
  }
  return map;
}

function deductInventoryStock(inventorySheet, inventoryMap, barcode, quantitySold, itemData) {
  var record = inventoryMap[barcode];
  var nowIso = new Date().toISOString();

  if (record) {
    // Read current stock cell dynamically to maintain accuracy
    var stockCell = inventorySheet.getRange(record.row, 5);
    var currentStockVal = Number(stockCell.getValue()) || record.currentStock;
    // Prevent negative stock: Math.max(0, currentStockVal - quantitySold)
    var newStock = Math.max(0, currentStockVal - quantitySold);

    // S_new = max(0, S_current - Q_sold)
    stockCell.setValue(newStock);
    // Update Last Updated timestamp (Col 1)
    inventorySheet.getRange(record.row, 1).setValue(nowIso);

    record.currentStock = newStock;
    record.lastUpdated = nowIso;
  } else {
    // Product not yet in Google Sheet; upsert it with 0 stock (non-negative)
    var productName = itemData.productName || itemData.name || "Item " + barcode;
    var category = itemData.category || "General";
    var costPrice = Number(itemData.unitCost || itemData.costPrice) || 0.0;
    var retailPrice = Number(itemData.unitPrice || itemData.retailPrice) || 0.0;
    var newStock = 0; // Prevent negative stock for untracked products

    upsertProductRow(inventorySheet, {
      barcode: barcode,
      name: productName,
      category: category,
      stockQuantity: newStock,
      costPrice: costPrice,
      retailPrice: retailPrice,
      updatedAt: nowIso
    });

    var rowNumber = findProductRowByBarcode(inventorySheet, barcode);
    inventoryMap[barcode] = {
      row: rowNumber > 0 ? rowNumber : inventorySheet.getLastRow(),
      lastUpdated: nowIso,
      barcode: barcode,
      productName: productName,
      category: category,
      currentStock: newStock,
      costPrice: costPrice,
      retailPrice: retailPrice
    };
  }
}

function getDeltaCatalog(inventorySheet, lastSyncedAt) {
  var results = [];
  var lastRow = inventorySheet.getLastRow();
  if (lastRow <= 1) return results;

  var lastSyncDate = null;
  if (lastSyncedAt) {
    var parsed = new Date(lastSyncedAt);
    if (!isNaN(parsed.getTime())) {
      lastSyncDate = parsed;
    }
  }

  var data = inventorySheet.getRange(2, 1, lastRow - 1, 7).getValues();
  for (var i = 0; i < data.length; i++) {
    var row = data[i];
    var rawUpdated = row[0];
    var barcode = (row[1] || "").toString().trim();
    var productName = (row[2] || "").toString().trim();

    if (!barcode && !productName) continue;

    var isDelta = true;
    if (lastSyncDate && rawUpdated) {
      var rowDate = new Date(rawUpdated);
      if (!isNaN(rowDate.getTime()) && rowDate <= lastSyncDate) {
        isDelta = false;
      }
    }

    if (isDelta) {
      results.push({
        barcode: barcode,
        productName: productName,
        category: (row[3] || "General").toString().trim(),
        stockQuantity: Math.max(0, Number(row[4]) || 0),
        costPrice: Number(row[5]) || 0.0,
        retailPrice: Number(row[6]) || 0.0,
        lastUpdated: (rawUpdated instanceof Date) ? rawUpdated.toISOString() : (rawUpdated ? rawUpdated.toString() : "")
      });
    }
  }
  return results;
}

/**
 * Upsert product row by barcode (Column B, 0-indexed column 1)
 * Updates existing row or appends new row.
 * Guarantees non-negative stock: Math.max(0, Number(product.stockQuantity))
 * Preserves individual product modification timestamps (product.updatedAt).
 */
function upsertProductRow(sheet, product) {
  if (!product || !product.barcode) return;
  var barcodeStr = String(product.barcode).trim();
  if (!barcodeStr) return;

  var data = sheet.getDataRange().getValues();
  var barcodeColIndex = 1; // Column B (0-indexed)
  var timestamp = product.updatedAt ? String(product.updatedAt) : new Date().toISOString();
  var name = product.productName || product.name || "";
  var category = product.category || "General";
  var stock = Math.max(0, Number(product.stockQuantity) || 0);
  var cost = Number(product.costPrice) || 0.0;
  var retail = Number(product.retailPrice) || 0.0;

  // Search for existing product by barcode (row index 1+ to skip header)
  for (var i = 1; i < data.length; i++) {
    if (String(data[i][barcodeColIndex]).trim() === barcodeStr) {
      var rowNum = i + 1;
      sheet.getRange(rowNum, 1).setValue(timestamp); // Last Updated
      sheet.getRange(rowNum, 3).setValue(name);      // Name
      sheet.getRange(rowNum, 4).setValue(category);  // Category
      sheet.getRange(rowNum, 5).setValue(stock);     // Stock (Non-Negative)
      sheet.getRange(rowNum, 6).setValue(cost);      // Cost Price
      sheet.getRange(rowNum, 7).setValue(retail);    // Retail Price
      return;
    }
  }

  // If barcode is not found, append a new row
  sheet.appendRow([
    timestamp,
    barcodeStr,
    name,
    category,
    stock,
    cost,
    retail
  ]);
}

function syncInventorySnapshot(inventorySheet, products) {
  if (!products || !Array.isArray(products)) return;
  for (var i = 0; i < products.length; i++) {
    var p = products[i];
    if (p && (p.barcode || p.sku)) {
      upsertProductRow(inventorySheet, {
        barcode: p.barcode || p.sku,
        name: p.productName || p.name || "",
        category: p.category || "General",
        stockQuantity: p.stockQuantity,
        costPrice: p.costPrice,
        retailPrice: p.retailPrice,
        updatedAt: p.updatedAt
      });
    }
  }
}

function findProductRowByBarcode(sheet, barcode) {
  if (!barcode) return -1;
  var lastRow = sheet.getLastRow();
  if (lastRow <= 1) return -1;

  var barcodes = sheet.getRange(2, 2, lastRow - 1, 1).getValues();
  for (var i = 0; i < barcodes.length; i++) {
    if (barcodes[i][0] && barcodes[i][0].toString().trim() === barcode) {
      return i + 2;
    }
  }
  return -1;
}

function pullDataResponse(ss) {
  try {
    var invSheet = getOrCreateInventorySheet(ss);
    var catalog = getDeltaCatalog(invSheet, null);
    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      products: catalog,
      totalProducts: catalog.length
    })).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function getOrCreateSheet(ss, sheetName, headers) {
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    sheet.appendRow(headers);
    sheet.getRange(1, 1, 1, headers.length).setFontWeight("bold").setBackground("#f1f5f9");
  } else if (sheet.getLastRow() === 0) {
    sheet.appendRow(headers);
    sheet.getRange(1, 1, 1, headers.length).setFontWeight("bold").setBackground("#f1f5f9");
  }
  return sheet;
}

// -------------------------------------------------------------
// UTANG (CUSTOMER CREDIT LEDGER) SYNC HANDLERS & HELPERS
// -------------------------------------------------------------

/**
 * 1. Auto-create & Upsert 'Customers' Sheet
 * Locates row in Customers sheet by customerId (Column A).
 * Upserts row: [customerId, customerName, phoneNumber, currentBalance, lastUpdated]
 */
function syncCustomer(ss, customer) {
  if (!customer) return;
  var sheet = getOrCreateSheet(ss, "Customers", [
    "Customer ID", "Customer Name", "Phone Number", "Current Balance", "Last Updated"
  ]);

  var cId = String(customer.customerId || customer.id || "").trim();
  if (!cId) return;

  var name = customer.name || customer.customerName || "";
  var phone = customer.phone || customer.phoneNumber || "N/A";
  var balance = Number(customer.currentBalance) || 0.0;
  var updatedAt = customer.updatedAt || customer.lastUpdated || new Date().toISOString();

  var lastRow = sheet.getLastRow();
  if (lastRow > 1) {
    var data = sheet.getRange(2, 1, lastRow - 1, 5).getValues();
    for (var i = 0; i < data.length; i++) {
      if (String(data[i][0]).trim() === cId) {
        var rowNum = i + 2;
        sheet.getRange(rowNum, 2).setValue(name);
        sheet.getRange(rowNum, 3).setValue(phone);
        sheet.getRange(rowNum, 4).setValue(balance);
        sheet.getRange(rowNum, 5).setValue(updatedAt);
        return;
      }
    }
  }

  sheet.appendRow([
    cId,
    name,
    phone,
    balance,
    updatedAt
  ]);
}

/**
 * 2. Auto-create & Append to 'Utang_Transactions' Sheet
 * Appends row: [Customer Name, Transaction Number, Goods Borrowed, Total Amount, Date & Time]
 * Also automatically updates master customer balance on the summary sheet.
 */
function logUtangTransaction(ss, data) {
  if (!data) return;
  var sheet = getOrCreateSheet(ss, "Utang_Transactions", [
    "Customer Name", "Transaction Number", "Goods Borrowed", "Total Amount", "Date & Time"
  ]);

  var customerName = data.customerName || data.name || "Customer";
  var transactionNumber = data.transactionNumber || data.transactionId || data.id || ("TX-" + Utilities.getUuid());
  var goodsBorrowed = data.goodsBorrowed || data.itemsSummary || data.itemSummary || "N/A";
  var totalAmount = Number(data.totalAmount || data.amountBorrowed || data.amount) || 0.0;
  var timestamp = data.timestamp || new Date().toISOString();

  sheet.appendRow([
    customerName,
    transactionNumber,
    goodsBorrowed,
    totalAmount,
    timestamp
  ]);

  // Also update master customer balance on summary sheet
  if (data.customerId) {
    var newBalance = (data.newBalance !== undefined && data.newBalance !== null)
      ? Number(data.newBalance)
      : (data.remainingBalance !== undefined ? Number(data.remainingBalance) : (getCustomerBalance(ss, data.customerId) + totalAmount));

    syncCustomer(ss, {
      customerId: data.customerId,
      name: customerName,
      phone: data.phone || data.phoneNumber || "N/A",
      currentBalance: newBalance,
      updatedAt: timestamp
    });
  }
}

/**
 * 3. Auto-create & Append to 'Payment_Logs' Sheet
 * Appends row: [Customer Name, Payment ID, Amount Paid, Remaining Balance, Date & Time]
 * Also automatically updates master customer balance on summary sheet.
 */
function logUtangPayment(ss, data) {
  if (!data) return;
  var sheet = getOrCreateSheet(ss, "Payment_Logs", [
    "Customer Name", "Payment ID", "Amount Paid", "Remaining Balance", "Date & Time"
  ]);

  var customerName = data.customerName || data.name || "Customer";
  var paymentId = data.paymentId || data.paymentNumber || data.id || ("PAY-" + Utilities.getUuid());
  var amountPaid = Number(data.amountPaid || data.totalAmount || data.amount) || 0.0;
  var timestamp = data.timestamp || new Date().toISOString();

  var remainingBalance = (data.remainingBalance !== undefined && data.remainingBalance !== null)
    ? Number(data.remainingBalance)
    : (data.newBalance !== undefined ? Number(data.newBalance) : Math.max(0, getCustomerBalance(ss, data.customerId) - amountPaid));

  sheet.appendRow([
    customerName,
    paymentId,
    amountPaid,
    remainingBalance,
    timestamp
  ]);

  if (data.customerId) {
    syncCustomer(ss, {
      customerId: data.customerId,
      name: customerName,
      phone: data.phone || data.phoneNumber || "N/A",
      currentBalance: remainingBalance,
      updatedAt: timestamp
    });
  }
}

/**
 * Upsert Customer Row in Customers Sheet (Backward-compatible helper)
 */
function upsertCustomerRow(sheetOrSs, customer) {
  var ss = (typeof sheetOrSs.getParent === "function") ? sheetOrSs.getParent() : SpreadsheetApp.getActiveSpreadsheet();
  syncCustomer(ss, customer);
}

/**
 * Get current customer balance from Customers Sheet
 */
function getCustomerBalance(ssOrSheet, customerId) {
  if (!customerId) return 0.0;
  var ss = (typeof ssOrSheet.getSheetByName === "function") ? ssOrSheet : SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Customers");
  if (!sheet) return 0.0;

  var data = sheet.getDataRange().getValues();
  for (var i = 1; i < data.length; i++) {
    if (String(data[i][0]).trim() === String(customerId).trim()) {
      return Number(data[i][3]) || 0.0;
    }
  }
  return 0.0;
}


"""
}
