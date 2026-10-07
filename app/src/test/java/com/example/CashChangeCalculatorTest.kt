package com.example

import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.util.PrinterManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CashChangeCalculatorTest {

    @Test
    fun testRealTimeChangeCalculationExact() {
        val totalAmount = 350.0
        val cashReceived = 350.0
        val changeDue = cashReceived - totalAmount

        assertEquals(0.0, changeDue, 0.001)
        assertTrue(cashReceived >= totalAmount)
    }

    @Test
    fun testRealTimeChangeCalculationWithChange() {
        val totalAmount = 350.0
        val cashReceived = 500.0
        val changeDue = cashReceived - totalAmount

        assertEquals(150.0, changeDue, 0.001)
        assertTrue(cashReceived >= totalAmount)
    }

    @Test
    fun testInsufficientCashCalculation() {
        val totalAmount = 350.0
        val cashReceived = 200.0
        val isSufficient = cashReceived >= totalAmount
        val remaining = totalAmount - cashReceived

        assertEquals(false, isSufficient)
        assertEquals(150.0, remaining, 0.001)
    }

    @Test
    fun testSaleEntityStoresCashTenderedAndChangeDue() {
        val sale = SaleEntity(
            id = 42L,
            totalAmount = 350.0,
            taxAmount = 0.0,
            paymentType = "Cash",
            cashTendered = 500.0,
            changeDue = 150.0
        )

        assertEquals(500.0, sale.cashTendered, 0.001)
        assertEquals(150.0, sale.changeDue, 0.001)
    }

    @Test
    fun testHtmlReceiptIncludesCashReceivedAndChangeDue() {
        val settings = AppSettingsEntity(
            storeName = "Astilla Store",
            currencySymbol = "₱"
        )
        val sale = SaleEntity(
            id = 101L,
            totalAmount = 350.0,
            taxAmount = 0.0,
            paymentType = "Cash",
            cashTendered = 500.0,
            changeDue = 150.0
        )
        val items = listOf(
            SaleItemEntity(id = 1L, saleId = 101L, productId = 10L, productName = "Coffee Beans", quantity = 1, unitCost = 200.0, unitPrice = 350.0)
        )

        val html = PrinterManager.generateReceiptHtml(settings, sale, items)
        assertTrue(html.contains("Cash Received:"))
        assertTrue(html.contains("500.00"))
        assertTrue(html.contains("CHANGE DUE:"))
        assertTrue(html.contains("150.00"))
    }

    @Test
    fun testEscPosReceiptBytesIncludeCashReceivedAndChangeDue() {
        val settings = AppSettingsEntity(
            storeName = "Astilla Store",
            currencySymbol = "₱"
        )
        val sale = SaleEntity(
            id = 102L,
            totalAmount = 350.0,
            taxAmount = 0.0,
            paymentType = "Cash",
            cashTendered = 500.0,
            changeDue = 150.0
        )
        val items = listOf(
            SaleItemEntity(id = 1L, saleId = 102L, productId = 10L, productName = "Snack Pack", quantity = 1, unitCost = 200.0, unitPrice = 350.0)
        )

        val bytes = PrinterManager.buildEscPosReceiptBytes(settings, sale, items)
        val receiptText = String(bytes, Charsets.UTF_8)

        assertTrue(receiptText.contains("Cash Received: ₱500.00"))
        assertTrue(receiptText.contains("CHANGE DUE: ₱150.00"))
    }
}
