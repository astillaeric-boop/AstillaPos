package com.example

import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.util.CreditReceiptHelper
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditReceiptHelperTest {

    private val settings = AppSettingsEntity(
        storeName = "Astilla POS Store",
        phoneNumber = "09123456789",
        businessAddress = "123 Main St, Manila"
    )

    private val customer = CustomerEntity(
        id = 1L,
        name = "Aling Nena",
        phoneNumber = "09171234567",
        messengerContact = "aling.nena",
        currentBalance = 450.00
    )

    @Test
    fun testBuildBorrowTransactionReceipt() {
        val transaction = CreditTransactionEntity(
            customerId = 1L,
            transactionType = "BORROW",
            amount = 250.00,
            itemSummary = "2x Arabica Coffee (₱250)",
            remainingBalance = 700.00
        )

        val receipt = CreditReceiptHelper.buildTransactionReceipt(settings, customer, transaction)

        assertTrue(receipt.contains("Borrow Notice"))
        assertTrue(receipt.contains("Hi Aling Nena, you borrowed:"))
        assertTrue(receipt.contains("- Arabica Coffee x2 (₱250)"))
        assertTrue(receipt.contains("Total Borrowed: ₱250.00"))
        assertTrue(receipt.contains("Updated Total Balance: ₱700.00"))
        assertTrue(receipt.contains("Thank you!"))
    }

    @Test
    fun testMultiItemBorrowReceipt() {
        val transaction = CreditTransactionEntity(
            customerId = 1L,
            transactionType = "BORROW",
            amount = 155.00,
            itemSummary = "2x Milk (₱120), 1x Soy Sauce (₱35)",
            remainingBalance = 605.00
        )

        val receipt = CreditReceiptHelper.buildTransactionReceipt(settings, customer, transaction)

        assertTrue(receipt.contains("[Astilla Store] Borrow Notice"))
        assertTrue(receipt.contains("Hi Aling Nena, you borrowed:"))
        assertTrue(receipt.contains("- Milk x2 (₱120)"))
        assertTrue(receipt.contains("- Soy Sauce x1 (₱35)"))
        assertTrue(receipt.contains("Total Borrowed: ₱155.00"))
        assertTrue(receipt.contains("Updated Total Balance: ₱605.00"))
        assertTrue(receipt.contains("Thank you!"))
    }

    @Test
    fun testParseBorrowItemLine() {
        val line1 = CreditReceiptHelper.parseBorrowItemLine("2x Milk (₱120)")
        val line2 = CreditReceiptHelper.parseBorrowItemLine("1x Soy Sauce (₱35)")
        val line3 = CreditReceiptHelper.parseBorrowItemLine("Bread x3 (₱75)")

        assertTrue(line1 == "- Milk x2 (₱120)")
        assertTrue(line2 == "- Soy Sauce x1 (₱35)")
        assertTrue(line3 == "- Bread x3 (₱75)")
    }

    @Test
    fun testBuildPaymentTransactionReceipt() {
        val transaction = CreditTransactionEntity(
            customerId = 1L,
            transactionType = "PAYMENT",
            amount = 200.00,
            itemSummary = "Paid via Cash",
            remainingBalance = 250.00
        )

        val receipt = CreditReceiptHelper.buildTransactionReceipt(settings, customer, transaction)

        assertTrue(receipt.contains("OFFICIAL PAYMENT RECEIPT"))
        assertTrue(receipt.contains("Aling Nena"))
        assertTrue(receipt.contains("Paid via Cash"))
        assertTrue(receipt.contains("250.00"))
    }

    @Test
    fun testBuildStatementReminder() {
        val reminder = CreditReceiptHelper.buildStatementReminder(settings, customer)

        assertTrue(reminder.contains("Outstanding Utang Statement"))
        assertTrue(reminder.contains("Aling Nena"))
        assertTrue(reminder.contains("450.00"))
    }
}
