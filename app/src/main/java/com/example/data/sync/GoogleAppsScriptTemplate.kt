package com.example.data.sync

object GoogleAppsScriptTemplate {
    const val SCRIPT_CODE = """/**
 * ASTILLA POS - GOOGLE APPS SCRIPT WEB APP SYNC CONNECTOR
 * 
 * INSTRUCTIONS:
 * 1. Open your Google Sheet.
 * 2. Click Extensions > Apps Script.
 * 3. Delete existing code and paste this entire script.
 * 4. Click 'Deploy' > 'New deployment'.
 * 5. Select type: 'Web app'.
 * 6. Set Description: 'Astilla POS Sync'.
 * 7. Set 'Execute as': 'Me'.
 * 8. Set 'Who has access': 'Anyone' (IMPORTANT for native app access).
 * 9. Click 'Deploy', authorize permissions, and copy the 'Web app URL'.
 * 10. Paste the Web app URL in Astilla POS Settings > Google Sheet Sync.
 */

function doPost(e) {
  try {
    var rawData = e.postData.contents;
    var data = JSON.parse(rawData);
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    // If client requested a PULL action via POST
    if (data.action === "pull") {
      return pullDataResponse(ss);
    }

    // 1. Process Sales Tab
    if (data.sales && data.sales.length > 0) {
      var salesSheet = getOrCreateSheet(ss, "POS_Sales", [
        "Sale ID", "Date & Time", "Total Amount", "Tax Amount", "Payment Type", "Items Summary", "Synced At"
      ]);
      var now = new Date();
      data.sales.forEach(function(s) {
        salesSheet.appendRow([
          s.id,
          s.dateString || new Date(s.timestamp).toLocaleString(),
          s.totalAmount,
          s.taxAmount,
          s.paymentType,
          s.itemsSummary,
          now
        ]);
      });
    }

    // 2. Process Deliveries / Restock Tab
    if (data.deliveries && data.deliveries.length > 0) {
      var delSheet = getOrCreateSheet(ss, "POS_Deliveries", [
        "Delivery ID", "Date & Time", "Supplier Name", "Total Cost", "Notes", "Synced At"
      ]);
      var now = new Date();
      data.deliveries.forEach(function(d) {
        delSheet.appendRow([
          d.id,
          d.dateString || new Date(d.timestamp).toLocaleString(),
          d.supplierName,
          d.totalCost,
          d.notes,
          now
        ]);
      });
    }

    // 3. Process Live Inventory Snapshot
    if (data.inventory && data.inventory.length > 0) {
      var invSheet = ss.getSheetByName("POS_Inventory");
      if (!invSheet) {
        invSheet = ss.insertSheet("POS_Inventory");
      }
      invSheet.clear();
      invSheet.appendRow(["Product ID", "Product Name", "Barcode", "Cost Price", "Retail Price", "Current Stock", "Last Updated"]);
      invSheet.getRange(1, 1, 1, 7).setFontWeight("bold").setBackground("#f1f5f9");
      
      var now = new Date();
      data.inventory.forEach(function(p) {
        invSheet.appendRow([
          p.id,
          p.name,
          p.barcode,
          p.costPrice,
          p.retailPrice,
          p.stockQuantity,
          now
        ]);
      });
    }

    return ContentService
      .createTextOutput(JSON.stringify({
        status: "success",
        message: "Synchronized successfully with Astilla POS",
        syncedSales: data.sales ? data.sales.length : 0,
        syncedDeliveries: data.deliveries ? data.deliveries.length : 0
      }))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({
        status: "error",
        message: err.toString()
      }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var action = (e && e.parameter && e.parameter.action) ? e.parameter.action : "";
  if (action === "pull" || action === "inventory") {
    return pullDataResponse(ss);
  }

  return ContentService
    .createTextOutput(JSON.stringify({
      status: "online",
      service: "Astilla POS Google Sheets Sync Engine",
      timestamp: new Date().toISOString()
    }))
    .setMimeType(ContentService.MimeType.JSON);
}

function pullDataResponse(ss) {
  try {
    var invSheet = ss.getSheetByName("POS_Inventory");
    var products = [];
    if (invSheet) {
      var data = invSheet.getDataRange().getValues();
      // Skip header row
      for (var i = 1; i < data.length; i++) {
        var row = data[i];
        if (row[1] && row[1].toString().trim().length > 0) {
          products.push({
            id: Number(row[0]) || 0,
            name: row[1].toString().trim(),
            barcode: row[2] ? row[2].toString().trim() : "",
            costPrice: Number(row[3]) || 0.0,
            retailPrice: Number(row[4]) || 0.0,
            stockQuantity: parseInt(row[5], 10) || 0
          });
        }
      }
    }
    return ContentService
      .createTextOutput(JSON.stringify({
        status: "success",
        products: products,
        totalProducts: products.length
      }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({
        status: "error",
        message: err.toString()
      }))
      .setMimeType(ContentService.MimeType.JSON);
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
