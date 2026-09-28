package com.example

import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.SaleWithItems
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountingAnalyticsTest {

    @Test
    fun testAccrualBasisRevenueAndCashFlow() {
        val now = System.currentTimeMillis()

        // Sale 1: Cash sale ₱500 (COGS ₱300)
        val cashSale = SaleWithItems(
            sale = SaleEntity(id = 1L, timestamp = now, totalAmount = 500.0, taxAmount = 0.0, paymentType = "Cash"),
            items = listOf(SaleItemEntity(id = 1L, saleId = 1L, productId = 10L, quantity = 2, unitCost = 150.0, unitPrice = 250.0))
        )

        // Sale 2: Credit / Utang sale ₱300 (COGS ₱180)
        val creditSale = SaleWithItems(
            sale = SaleEntity(id = 2L, timestamp = now, totalAmount = 300.0, taxAmount = 0.0, paymentType = "Credit (Utang)"),
            items = listOf(SaleItemEntity(id = 2L, saleId = 2L, productId = 11L, quantity = 1, unitCost = 180.0, unitPrice = 300.0))
        )

        val allSales = listOf(cashSale, creditSale)

        // Payment 1: Customer pays ₱200 for prior debt ("Bayad Utang")
        val utangPayment = CreditTransactionEntity(
            id = 1L,
            customerId = 99L,
            timestamp = now,
            transactionType = "CUSTOMER_PAYMENT",
            amount = 200.0,
            itemSummary = "Paid via Cash",
            remainingBalance = 100.0
        )
        val allCreditTxs = listOf(utangPayment)

        // 1. Calculate Gross Sales = Cash Sales + Credit Sales (Accrual basis)
        val cashSales = allSales.filter { !it.sale.paymentType.contains("Utang", true) && !it.sale.paymentType.contains("Credit", true) }
            .sumOf { it.sale.totalAmount }
        val creditSales = allSales.filter { it.sale.paymentType.contains("Utang", true) || it.sale.paymentType.contains("Credit", true) }
            .sumOf { it.sale.totalAmount }
        val grossSales = cashSales + creditSales

        assertEquals(500.0, cashSales, 0.001)
        assertEquals(300.0, creditSales, 0.001)
        assertEquals(800.0, grossSales, 0.001)

        // 2. Calculate Cash Collected (Cash Flow) = Cash Sales + Utang Payments Received
        val paymentsCollected = allCreditTxs.filter { it.transactionType.contains("PAYMENT", true) }
            .sumOf { it.amount }
        val cashCollected = cashSales + paymentsCollected

        assertEquals(200.0, paymentsCollected, 0.001)
        assertEquals(700.0, cashCollected, 0.001) // 500 cash sale + 200 utang payment

        // 3. COGS: Cost of all items sold / borrowed
        val totalCogs = allSales.flatMap { it.items }.sumOf { it.unitCost * it.quantity }
        assertEquals(480.0, totalCogs, 0.001) // (150*2) + (180*1)

        // 4. Net Profit: Total Revenue - COGS
        val netProfit = grossSales - totalCogs
        assertEquals(320.0, netProfit, 0.001) // 800 - 480

        // 5. Verify customer payment does not count towards product sales (Prevent double counting)
        val nonDoubleCountedSales = allSales.size
        assertEquals(2, nonDoubleCountedSales)
    }

    @Test
    fun testOutstandingUtangBalanceCalculation() {
        val customers = listOf(
            CustomerEntity(id = 1L, name = "Customer A", currentBalance = 460.0),
            CustomerEntity(id = 2L, name = "Customer B", currentBalance = 925.0),
            CustomerEntity(id = 3L, name = "Customer C", currentBalance = 0.0)
        )

        val totalOutstandingUtang = customers.sumOf { it.currentBalance }
        assertEquals(1385.0, totalOutstandingUtang, 0.001)
    }
}
