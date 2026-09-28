package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CreditReceiptHelper {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    private val shortDateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    /**
     * Parses an item from itemSummary into the standardized "- {Item} x{Qty} (₱{Price})" format.
     */
    fun parseBorrowItemLine(raw: String): String {
        val trimmed = raw.trim().removePrefix("-").removePrefix("•").trim()
        if (trimmed.isBlank()) return ""
        // Matches e.g. "2x Milk (₱120)" or "2x Milk (₱120.00)" or "2x Arabica Coffee"
        val match = Regex("""^(\d+)x\s+(.+?)(?:\s*\((?:₱|PHP)?([\d,]+(?:\.\d+)?)\))?$""").find(trimmed)
        if (match != null) {
            val qty = match.groupValues[1]
            val name = match.groupValues[2].trim()
            val priceStr = match.groupValues[3]
            return if (priceStr.isNotBlank()) {
                "- $name x$qty (₱$priceStr)"
            } else {
                "- $name x$qty"
            }
        }
        // Matches e.g. "Milk x2 (₱120)"
        val match2 = Regex("""^(.+?)\s+x(\d+)(?:\s*\((?:₱|PHP)?([\d,]+(?:\.\d+)?)\))?$""").find(trimmed)
        if (match2 != null) {
            val name = match2.groupValues[1].trim()
            val qty = match2.groupValues[2]
            val priceStr = match2.groupValues[3]
            return if (priceStr.isNotBlank()) {
                "- $name x$qty (₱$priceStr)"
            } else {
                "- $name x$qty"
            }
        }
        return "- $trimmed"
    }

    /**
     * Builds receipt text for a specific credit transaction (Borrow or Payment).
     */
    fun buildTransactionReceipt(
        storeSettings: AppSettingsEntity,
        customer: CustomerEntity,
        transaction: CreditTransactionEntity
    ): String {
        val isBorrow = transaction.transactionType.equals("BORROW", ignoreCase = true)
        val storeTitle = if (storeSettings.storeName.isBlank() || storeSettings.storeName == "Astilla POS Store") "Astilla Store" else storeSettings.storeName

        if (isBorrow) {
            val timestamp = if (transaction.timestamp > 0) transaction.timestamp else System.currentTimeMillis()
            val dateStr = dateFormatter.format(Date(timestamp))
            val sb = StringBuilder()
            sb.appendLine("[$storeTitle] Borrow Notice")
            sb.appendLine()
            sb.appendLine("Date: $dateStr")
            sb.appendLine("Hi ${customer.name}, you borrowed:")
            if (transaction.itemSummary.isNotBlank()) {
                transaction.itemSummary.split("\n", ", ").forEach { rawItem ->
                    val line = parseBorrowItemLine(rawItem)
                    if (line.isNotBlank()) {
                        sb.appendLine(line)
                    }
                }
            } else {
                sb.appendLine("- Store Items x1 (₱${String.format(Locale.US, "%,.2f", transaction.amount)})")
            }
            sb.appendLine()
            sb.appendLine("Total Borrowed: ₱${String.format(Locale.US, "%,.2f", transaction.amount)}")
            sb.appendLine("Updated Total Balance: ₱${String.format(Locale.US, "%,.2f", transaction.remainingBalance)}")
            sb.appendLine()
            sb.appendLine("Thank you!")
            return sb.toString().trim()
        }

        val storeName = storeSettings.storeName.ifBlank { "ASTILLA POS" }
        val storePhone = storeSettings.phoneNumber
        val storeAddress = storeSettings.businessAddress
        val dateStr = dateFormatter.format(Date(transaction.timestamp))

        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine(storeName.uppercase())
        if (storeAddress.isNotBlank()) sb.appendLine(storeAddress)
        if (storePhone.isNotBlank()) sb.appendLine("Tel: $storePhone")
        sb.appendLine("================================")
        sb.appendLine("OFFICIAL PAYMENT RECEIPT")
        sb.appendLine("Date: $dateStr")
        sb.appendLine("Customer: ${customer.name}")
        if (customer.phoneNumber.isNotBlank()) sb.appendLine("Mobile: ${customer.phoneNumber}")
        sb.appendLine("--------------------------------")
        sb.appendLine("TRANSACTION: PAYMENT (BAYAD)")
        if (transaction.itemSummary.isNotBlank()) {
            sb.appendLine("Notes: ${transaction.itemSummary}")
        }
        sb.appendLine("Amount Paid: ₱${String.format(Locale.US, "%,.2f", transaction.amount)}")
        sb.appendLine("--------------------------------")
        sb.appendLine("OUTSTANDING BALANCE: ₱${String.format(Locale.US, "%,.2f", transaction.remainingBalance)}")
        sb.appendLine("================================")
        if (transaction.remainingBalance <= 0) {
            sb.appendLine("🎉 UTANG FULLY PAID! THANK YOU!")
        } else {
            sb.appendLine("Thank you for your partial payment!")
        }
        return sb.toString().trim()
    }

    /**
     * Builds a Statement of Account reminder message for a customer with an active balance.
     */
    fun buildStatementReminder(
        storeSettings: AppSettingsEntity,
        customer: CustomerEntity
    ): String {
        val storeName = storeSettings.storeName.ifBlank { "ASTILLA POS" }
        val storePhone = storeSettings.phoneNumber
        val dateStr = shortDateFormatter.format(Date())

        val sb = StringBuilder()
        sb.appendLine("REMINDER: Outstanding Utang Statement")
        sb.appendLine("Store: $storeName")
        sb.appendLine("Date: $dateStr")
        sb.appendLine("Customer: ${customer.name}")
        sb.appendLine("--------------------------------")
        sb.appendLine("Current Total Utang: ₱${String.format(Locale.US, "%,.2f", customer.currentBalance)}")
        sb.appendLine("Last Activity: ${shortDateFormatter.format(Date(customer.lastUpdated))}")
        sb.appendLine("--------------------------------")
        sb.appendLine("Kindly settle your balance at our store via Cash, GCash, or Maya.")
        if (storePhone.isNotBlank()) sb.appendLine("For inquiries, contact: $storePhone")
        sb.appendLine("Thank you for your prompt payment!")
        return sb.toString().trim()
    }

    /**
     * Launches the device's native SIM SMS application with recipient and prefilled message.
     */
    fun sendNativeSms(context: Context, phoneNumber: String, messageText: String) {
        val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
        try {
            val uri = Uri.parse("smsto:$cleanPhone")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", messageText)
                putExtra(Intent.EXTRA_TEXT, messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: Copy to clipboard and open generic SMS / share
            copyToClipboard(context, "Credit Receipt", messageText)
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$cleanPhone")
                    putExtra("sms_body", messageText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Receipt copied to clipboard! (No SMS app found)", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Opens Facebook Messenger chat via m.me link with copied receipt text.
     */
    fun openMessengerReceipt(context: Context, messengerContact: String, messageText: String) {
        // Copy receipt text to clipboard so merchant can paste directly into the chat
        copyToClipboard(context, "Messenger Receipt", messageText)
        Toast.makeText(
            context,
            "Receipt copied! Opening Messenger... Paste text into chat.",
            Toast.LENGTH_LONG
        ).show()

        val cleanHandle = cleanMessengerContact(messengerContact)
        val url = if (cleanHandle.isNotBlank()) {
            "https://m.me/$cleanHandle"
        } else {
            "https://m.me"
        }

        try {
            val messengerIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                // If Facebook Messenger app is installed, try targeting it
                setPackage("com.facebook.orca")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(messengerIntent)
        } catch (e: Exception) {
            // If Messenger package not directly found, open via web browser / system default
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not open Messenger. Receipt is copied to clipboard.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Generic share text to any installed app (Viber, WhatsApp, Telegram, etc.)
     */
    fun shareReceipt(context: Context, subject: String, messageText: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Send Receipt via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            copyToClipboard(context, subject, messageText)
            Toast.makeText(context, "Receipt text copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies text to the Android Clipboard.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    private fun cleanMessengerContact(contact: String): String {
        var clean = contact.trim()
        if (clean.startsWith("@")) clean = clean.substring(1)
        if (clean.startsWith("https://m.me/")) clean = clean.removePrefix("https://m.me/")
        if (clean.startsWith("http://m.me/")) clean = clean.removePrefix("http://m.me/")
        if (clean.startsWith("m.me/")) clean = clean.removePrefix("m.me/")
        if (clean.startsWith("https://www.facebook.com/")) clean = clean.removePrefix("https://www.facebook.com/")
        if (clean.startsWith("https://facebook.com/")) clean = clean.removePrefix("https://facebook.com/")
        return clean.trimEnd('/')
    }
}
