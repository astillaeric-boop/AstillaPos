package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrinterManager {

    /**
     * Prints an HTML receipt or report using Android's native PrintManager.
     */
    fun printHtmlDocument(context: Context, jobName: String, htmlContent: String) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pos_print", "POS Print", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager?.print(jobName, printAdapter, printAttributes)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    /**
     * Generates a clean, professional HTML receipt.
     */
    fun generateReceiptHtml(
        settings: AppSettingsEntity,
        sale: SaleEntity,
        items: List<SaleItemEntity>
    ): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date(sale.timestamp))
        val currency = settings.currencySymbol

        val itemsHtml = StringBuilder()
        var subtotal = 0.0
        for (item in items) {
            val lineTotal = item.unitPrice * item.quantity
            subtotal += lineTotal
            itemsHtml.append(
                """
                <tr>
                    <td style="padding: 4px 0; text-align: left;">
                        <strong>${escapeHtml(item.productName)}</strong><br/>
                        <span style="font-size: 11px; color: #555;">${item.quantity} x $currency${String.format(Locale.US, "%.2f", item.unitPrice)}</span>
                    </td>
                    <td style="padding: 4px 0; text-align: right; vertical-align: top;">
                        $currency${String.format(Locale.US, "%.2f", lineTotal)}
                    </td>
                </tr>
                """.trimIndent()
            )
        }

        val addressLine = if (settings.businessAddress.isNotBlank()) {
            "<div style=\"font-size: 11px;\">${escapeHtml(settings.businessAddress)}${if (settings.zipCode.isNotBlank()) " " + escapeHtml(settings.zipCode) else ""}</div>"
        } else ""
        val phoneLine = if (settings.phoneNumber.isNotBlank()) {
            "<div style=\"font-size: 11px;\">Tel: ${escapeHtml(settings.phoneNumber)}</div>"
        } else ""

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {
                    font-family: 'Courier New', Courier, monospace;
                    font-size: 13px;
                    color: #000;
                    margin: 0;
                    padding: 16px;
                    width: 300px;
                    background: #fff;
                }
                .text-center { text-align: center; }
                .text-right { text-align: right; }
                .header { margin-bottom: 12px; }
                .store-name { font-size: 18px; font-weight: bold; margin-bottom: 4px; text-transform: uppercase; }
                .divider { border-top: 1px dashed #000; margin: 8px 0; }
                table { width: 100%; border-collapse: collapse; }
                .total-row { font-weight: bold; font-size: 15px; }
                .footer { margin-top: 16px; font-size: 11px; text-align: center; color: #444; }
            </style>
        </head>
        <body>
            <div class="header text-center">
                <div class="store-name">${escapeHtml(settings.storeName)}</div>
                $addressLine
                $phoneLine
                <div style="font-weight: bold; margin-top: 4px;">OFFICIAL RECEIPT</div>
                <div style="font-size: 11px;">$dateString</div>
                <div style="font-size: 11px;">Receipt #${sale.id}</div>
            </div>
            
            <div class="divider"></div>
            
            <table>
                <tbody>
                    $itemsHtml
                </tbody>
            </table>
            
            <div class="divider"></div>
            
            <table>
                <tr>
                    <td>Subtotal:</td>
                    <td class="text-right">$currency${String.format(Locale.US, "%.2f", subtotal)}</td>
                </tr>
                ${
            if (settings.taxEnabled) """
                <tr>
                    <td>Tax (${settings.taxRate}%):</td>
                    <td class="text-right">$currency${String.format(Locale.US, "%.2f", sale.taxAmount)}</td>
                </tr>
                """ else ""
        }
                <tr class="total-row">
                    <td style="padding-top: 6px;">TOTAL:</td>
                    <td class="text-right" style="padding-top: 6px;">$currency${String.format(Locale.US, "%.2f", sale.totalAmount)}</td>
                </tr>
                <tr>
                    <td style="font-size: 11px; color: #555;">Payment Type:</td>
                    <td class="text-right" style="font-size: 11px; color: #555;">${escapeHtml(sale.paymentType)}</td>
                </tr>
            </table>
            
            <div class="divider"></div>
            
            <div class="footer">
                Thank you for your business!<br/>
                Please keep this receipt for your records.<br/>
                Powered by Astilla POS
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Generates an Accounting-Style Full Store Financial Report HTML for native PrintManager.
     */
    fun generateStoreReportHtml(
        settings: AppSettingsEntity,
        periodName: String,
        totalRevenue: Double,
        totalCogs: Double,
        grossProfit: Double,
        totalTax: Double,
        totalSalesCount: Int,
        totalRestockExpenditure: Double,
        topSellingItems: List<Pair<String, Int>> = emptyList(),
        cashCollected: Double = 0.0,
        cashSales: Double = 0.0,
        creditSales: Double = 0.0,
        utangPaymentsCollected: Double = 0.0,
        netProfit: Double = totalRevenue - totalCogs,
        totalOutstandingUtang: Double = 0.0
    ): String {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault())
        val generatedDate = dateFormat.format(Date())
        val currency = settings.currencySymbol
        val marginPercent = if (totalRevenue > 0) (netProfit / totalRevenue) * 100.0 else 0.0

        val topItemsHtml = StringBuilder()
        if (topSellingItems.isNotEmpty()) {
            topItemsHtml.append("<div style='margin-top: 20px; font-weight: bold;'>Top Performing Items:</div>")
            topItemsHtml.append("<table style='width: 100%; border-collapse: collapse; margin-top: 8px;'>")
            topItemsHtml.append("<tr style='background: #f1f5f9;'><th style='text-align: left; padding: 6px;'>Product</th><th style='text-align: right; padding: 6px;'>Qty Sold</th></tr>")
            topSellingItems.take(5).forEach { (name, qty) ->
                topItemsHtml.append("<tr><td style='padding: 6px; border-bottom: 1px solid #e2e8f0;'>${escapeHtml(name)}</td><td style='padding: 6px; border-bottom: 1px solid #e2e8f0; text-align: right;'>$qty</td></tr>")
            }
            topItemsHtml.append("</table>")
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    color: #1e293b;
                    margin: 0;
                    padding: 32px;
                    background: #fff;
                    font-size: 14px;
                }
                .report-header {
                    border-bottom: 2px solid #0f172a;
                    padding-bottom: 16px;
                    margin-bottom: 24px;
                }
                .store-title {
                    font-size: 24px;
                    font-weight: 800;
                    color: #0f172a;
                }
                .report-subtitle {
                    font-size: 16px;
                    color: #475569;
                    margin-top: 4px;
                }
                .meta-badge {
                    display: inline-block;
                    background: #f1f5f9;
                    padding: 4px 10px;
                    border-radius: 4px;
                    font-size: 12px;
                    color: #334155;
                    margin-top: 8px;
                }
                .metrics-grid {
                    display: flex;
                    gap: 16px;
                    margin-bottom: 24px;
                }
                .metric-card {
                    flex: 1;
                    padding: 16px;
                    background: #f8fafc;
                    border: 1px solid #e2e8f0;
                    border-radius: 8px;
                }
                .metric-label { font-size: 12px; color: #64748b; font-weight: 600; text-transform: uppercase; }
                .metric-val { font-size: 20px; font-weight: 700; color: #0f172a; margin-top: 4px; }
                table.accounting {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 16px;
                }
                table.accounting th, table.accounting td {
                    padding: 10px 12px;
                    border-bottom: 1px solid #cbd5e1;
                }
                table.accounting th {
                    background: #0f172a;
                    color: #fff;
                    text-align: left;
                    font-weight: 600;
                }
                .highlight-row {
                    background: #f0fdf4;
                    font-weight: bold;
                }
            </style>
        </head>
        <body>
            <div class="report-header">
                <div class="store-title">${escapeHtml(settings.storeName)}</div>
                ${if (settings.businessAddress.isNotBlank()) "<div style='font-size: 13px; color: #475569;'>${escapeHtml(settings.businessAddress)}${if (settings.zipCode.isNotBlank()) " " + escapeHtml(settings.zipCode) else ""}</div>" else ""}
                ${if (settings.phoneNumber.isNotBlank()) "<div style='font-size: 13px; color: #475569;'>Phone: ${escapeHtml(settings.phoneNumber)}</div>" else ""}
                <div class="report-subtitle">Store Financial Performance & Accounting Report</div>
                <div class="meta-badge">Period: <strong>$periodName</strong> | Generated: $generatedDate</div>
            </div>

            <table class="accounting">
                <thead>
                    <tr>
                        <th>Financial Metric</th>
                        <th style="text-align: right;">Amount</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Total Sales Transactions</td>
                        <td style="text-align: right;"><strong>$totalSalesCount</strong> orders</td>
                    </tr>
                    <tr>
                        <td>Gross Sales (Total Revenue)</td>
                        <td style="text-align: right;"><strong>$currency${String.format(Locale.US, "%,.2f", totalRevenue)}</strong></td>
                    </tr>
                    ${if (cashSales > 0 || creditSales > 0) """
                    <tr style="font-size: 12px; color: #64748b;">
                        <td style="padding-left: 20px;">↳ Cash / Direct Sales</td>
                        <td style="text-align: right;">$currency${String.format(Locale.US, "%,.2f", cashSales)}</td>
                    </tr>
                    <tr style="font-size: 12px; color: #64748b;">
                        <td style="padding-left: 20px;">↳ Credit (Utang) Sales</td>
                        <td style="text-align: right;">$currency${String.format(Locale.US, "%,.2f", creditSales)}</td>
                    </tr>
                    """ else ""}
                    <tr style="background: #f0f9ff;">
                        <td><strong>Cash Collected (Cash Flow)</strong></td>
                        <td style="text-align: right; color: #0284c7;"><strong>$currency${String.format(Locale.US, "%,.2f", cashCollected)}</strong></td>
                    </tr>
                    ${if (utangPaymentsCollected > 0) """
                    <tr style="font-size: 12px; color: #64748b;">
                        <td style="padding-left: 20px;">↳ Utang Payments Received</td>
                        <td style="text-align: right;">$currency${String.format(Locale.US, "%,.2f", utangPaymentsCollected)}</td>
                    </tr>
                    """ else ""}
                    <tr>
                        <td>Cost of Goods Sold (COGS)</td>
                        <td style="text-align: right; color: #dc2626;">-$currency${String.format(Locale.US, "%,.2f", totalCogs)}</td>
                    </tr>
                    <tr class="highlight-row">
                        <td><strong>Net Profit (Revenue - COGS)</strong></td>
                        <td style="text-align: right; color: #16a34a;"><strong>$currency${String.format(Locale.US, "%,.2f", netProfit)}</strong></td>
                    </tr>
                    <tr>
                        <td>Profit Margin</td>
                        <td style="text-align: right;"><strong>${String.format(Locale.US, "%.1f", marginPercent)}%</strong></td>
                    </tr>
                    <tr>
                        <td>Total Outstanding Customer Utang</td>
                        <td style="text-align: right; color: #7c3aed;"><strong>$currency${String.format(Locale.US, "%,.2f", totalOutstandingUtang)}</strong></td>
                    </tr>
                    <tr>
                        <td>Sales Tax Collected (${settings.taxRate}%)</td>
                        <td style="text-align: right;">$currency${String.format(Locale.US, "%,.2f", totalTax)}</td>
                    </tr>
                    <tr>
                        <td>Inventory Delivery & Restock Spend</td>
                        <td style="text-align: right; color: #d97706;">$currency${String.format(Locale.US, "%,.2f", totalRestockExpenditure)}</td>
                    </tr>
                </tbody>
            </table>

            $topItemsHtml

            <div style="margin-top: 40px; border-top: 1px solid #e2e8f0; padding-top: 12px; font-size: 11px; color: #94a3b8; text-align: center;">
                Astilla POS Native Financial Accounting Engine • Confidential Internal Store Document
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Builds ESC/POS standard byte command stream for 58mm / 80mm Bluetooth thermal printers.
     */
    fun buildEscPosReceiptBytes(
        settings: AppSettingsEntity,
        sale: SaleEntity,
        items: List<SaleItemEntity>
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val ESC: Byte = 0x1B
        val GS: Byte = 0x1D

        // Initialize printer: ESC @
        out.write(byteArrayOf(ESC, 0x40))

        // Center align: ESC a 1
        out.write(byteArrayOf(ESC, 0x61, 1))

        // Double height & width for store name: GS ! 0x11
        out.write(byteArrayOf(GS, 0x21, 0x11))
        out.write("${settings.storeName}\n".toByteArray(Charsets.US_ASCII))

        // Normal text size: GS ! 0x00
        out.write(byteArrayOf(GS, 0x21, 0x00))
        if (settings.businessAddress.isNotBlank()) {
            val addr = "${settings.businessAddress}${if (settings.zipCode.isNotBlank()) " " + settings.zipCode else ""}\n"
            out.write(addr.toByteArray(Charsets.US_ASCII))
        }
        if (settings.phoneNumber.isNotBlank()) {
            out.write("Tel: ${settings.phoneNumber}\n".toByteArray(Charsets.US_ASCII))
        }
        out.write("OFFICIAL RECEIPT\n".toByteArray(Charsets.US_ASCII))
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        out.write("Date: ${dateFormat.format(Date(sale.timestamp))}\n".toByteArray(Charsets.US_ASCII))
        out.write("Receipt #${sale.id}\n".toByteArray(Charsets.US_ASCII))
        out.write("--------------------------------\n".toByteArray(Charsets.US_ASCII))

        // Left align: ESC a 0
        out.write(byteArrayOf(ESC, 0x61, 0))

        // Items
        for (item in items) {
            val line1 = "${item.productName}\n"
            val qtyPrice = "${item.quantity} x ${settings.currencySymbol}${String.format(Locale.US, "%.2f", item.unitPrice)}"
            val lineTotal = "${settings.currencySymbol}${String.format(Locale.US, "%.2f", item.unitPrice * item.quantity)}"
            val spaceCount = maxOf(1, 32 - qtyPrice.length - lineTotal.length)
            val line2 = qtyPrice + " ".repeat(spaceCount) + lineTotal + "\n"
            out.write(line1.toByteArray(Charsets.US_ASCII))
            out.write(line2.toByteArray(Charsets.US_ASCII))
        }

        out.write("--------------------------------\n".toByteArray(Charsets.US_ASCII))

        // Totals (Right align: ESC a 2)
        out.write(byteArrayOf(ESC, 0x61, 2))
        if (settings.taxEnabled) {
            out.write("Tax: ${settings.currencySymbol}${String.format(Locale.US, "%.2f", sale.taxAmount)}\n".toByteArray(Charsets.US_ASCII))
        }
        // Bold total: ESC E 1
        out.write(byteArrayOf(ESC, 0x45, 1))
        out.write("TOTAL: ${settings.currencySymbol}${String.format(Locale.US, "%.2f", sale.totalAmount)}\n".toByteArray(Charsets.US_ASCII))
        out.write(byteArrayOf(ESC, 0x45, 0)) // Bold off
        out.write("Paid via: ${sale.paymentType}\n".toByteArray(Charsets.US_ASCII))

        // Center align: ESC a 1
        out.write(byteArrayOf(ESC, 0x61, 1))
        out.write("--------------------------------\n".toByteArray(Charsets.US_ASCII))
        out.write("Thank you for your business!\n\n\n".toByteArray(Charsets.US_ASCII))

        // Cut paper: GS V 66 0
        out.write(byteArrayOf(GS, 0x56, 66, 0))

        return out.toByteArray()
    }

    /**
     * Shares raw receipt text or thermal commands via Android Intent.
     */
    fun shareReceiptText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Receipt")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Receipt / Send to Bluetooth Printer"))
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
