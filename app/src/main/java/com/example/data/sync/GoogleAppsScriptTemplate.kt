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
 *    and POS batch sync actions.
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
    if (body.type === "INVENTORY") {
      var invSheetSingle = getOrCreateInventorySheet(ss);
      var barcode = body.barcode ? body.barcode.toString().trim() : "";
      var name = body.productName || body.name || "";
      var category = body.category || "General";
      var costPrice = Number(body.costPrice) || 0.0;
      var retailPrice = Number(body.retailPrice) || 0.0;
      var stock = Number(body.stockQuantity) || 0;
      var nowIso = new Date().toISOString();

      var rowIdx = findProductRowByBarcode(invSheetSingle, barcode);
      if (rowIdx > 0) {
        invSheetSingle.getRange(rowIdx, 1, 1, 7).setValues([[
          nowIso, barcode, name, category, stock, costPrice, retailPrice
        ]]);
      } else {
        invSheetSingle.appendRow([nowIso, barcode, name, category, stock, costPrice, retailPrice]);
      }

      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        message: "Inventory recorded"
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // 3. Transactions sync endpoint (Standard /api/v1/sync/transactions spec & ASTILLA POS batch sync)
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
    var stockCell = inventorySheet.getRange(record.row, 5);
    var currentStockVal = Number(stockCell.getValue()) || record.currentStock;
    var newStock = currentStockVal - quantitySold;

    stockCell.setValue(newStock);
    inventorySheet.getRange(record.row, 1).setValue(nowIso);

    record.currentStock = newStock;
    record.lastUpdated = nowIso;
  } else {
    var productName = itemData.productName || itemData.name || "Item " + barcode;
    var category = itemData.category || "General";
    var costPrice = Number(itemData.unitCost || itemData.costPrice) || 0.0;
    var retailPrice = Number(itemData.unitPrice || itemData.retailPrice) || 0.0;
    var newStock = -quantitySold;

    inventorySheet.appendRow([
      nowIso,
      barcode,
      productName,
      category,
      newStock,
      costPrice,
      retailPrice
    ]);

    var newRow = inventorySheet.getLastRow();
    inventoryMap[barcode] = {
      row: newRow,
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
        stockQuantity: Number(row[4]) || 0,
        costPrice: Number(row[5]) || 0.0,
        retailPrice: Number(row[6]) || 0.0,
        lastUpdated: (rawUpdated instanceof Date) ? rawUpdated.toISOString() : (rawUpdated ? rawUpdated.toString() : "")
      });
    }
  }
  return results;
}

function syncInventorySnapshot(inventorySheet, products) {
  var existingMap = buildInventoryMap(inventorySheet);
  var nowIso = new Date().toISOString();

  for (var i = 0; i < products.length; i++) {
    var p = products[i];
    var barcode = (p.barcode || "").toString().trim();
    if (!barcode) continue;

    var name = p.name || p.productName || "";
    var category = p.category || "General";
    var cost = Number(p.costPrice) || 0.0;
    var retail = Number(p.retailPrice) || 0.0;
    var stock = Number(p.stockQuantity) || 0;

    if (existingMap[barcode]) {
      var rowIdx = existingMap[barcode].row;
      inventorySheet.getRange(rowIdx, 1, 1, 7).setValues([[
        nowIso, barcode, name, category, stock, cost, retail
      ]]);
    } else {
      inventorySheet.appendRow([nowIso, barcode, name, category, stock, cost, retail]);
      existingMap[barcode] = { row: inventorySheet.getLastRow() };
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
  }
  return sheet;
}
"""
}
