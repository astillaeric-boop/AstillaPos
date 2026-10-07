package com.example

import com.example.data.local.dao.CategorySalesSummary
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.SaleWithItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductCategorySystemTest {

    @Test
    fun testCategoryEntityCreationAndDefaultProductCategory() {
        val category = CategoryEntity(id = 1L, name = "Beverages")
        assertEquals(1L, category.id)
        assertEquals("Beverages", category.name)

        // Verify ProductEntity has default category "General"
        val defaultProduct = ProductEntity(
            id = 10L,
            name = "Test Item",
            barcode = "12345678"
        )
        assertEquals("General", defaultProduct.category)

        val customProduct = ProductEntity(
            id = 11L,
            name = "Coca Cola 330ml",
            barcode = "8881234",
            category = "Beverages",
            costPrice = 18.0,
            retailPrice = 25.0,
            stockQuantity = 50
        )
        assertEquals("Beverages", customProduct.category)
    }

    @Test
    fun testCategoryBasedSalesAggregation() {
        val productCategoryMap = mapOf(
            1L to "Beverages",
            2L to "Snacks",
            3L to "Beverages",
            4L to "Toiletries"
        )

        val saleItems = listOf(
            SaleItemEntity(id = 1L, saleId = 100L, productId = 1L, quantity = 3, unitCost = 15.0, unitPrice = 25.0), // Beverages: 3 units, ₱75
            SaleItemEntity(id = 2L, saleId = 100L, productId = 2L, quantity = 2, unitCost = 10.0, unitPrice = 20.0), // Snacks: 2 units, ₱40
            SaleItemEntity(id = 3L, saleId = 101L, productId = 3L, quantity = 1, unitCost = 30.0, unitPrice = 45.0), // Beverages: 1 unit, ₱45
            SaleItemEntity(id = 4L, saleId = 102L, productId = 4L, quantity = 5, unitCost = 8.0, unitPrice = 15.0)   // Toiletries: 5 units, ₱75
        )

        val categoryStats = mutableMapOf<String, Pair<Int, Double>>()
        for (item in saleItems) {
            val cat = productCategoryMap[item.productId] ?: "General"
            val current = categoryStats[cat] ?: Pair(0, 0.0)
            categoryStats[cat] = Pair(
                current.first + item.quantity,
                current.second + (item.unitPrice * item.quantity)
            )
        }

        val summaries = categoryStats.map { (cat, pair) ->
            CategorySalesSummary(category = cat, unitsSold = pair.first, totalRevenue = pair.second)
        }.sortedByDescending { it.totalRevenue }

        // Beverages total: ₱75 + ₱45 = ₱120, 4 units
        val beverageSummary = summaries.firstOrNull { it.category == "Beverages" }
        assertEquals(4, beverageSummary?.unitsSold)
        assertEquals(120.0, beverageSummary?.totalRevenue ?: 0.0, 0.001)

        // Toiletries total: ₱75, 5 units
        val toiletriesSummary = summaries.firstOrNull { it.category == "Toiletries" }
        assertEquals(5, toiletriesSummary?.unitsSold)
        assertEquals(75.0, toiletriesSummary?.totalRevenue ?: 0.0, 0.001)

        // Snacks total: ₱40, 2 units
        val snacksSummary = summaries.firstOrNull { it.category == "Snacks" }
        assertEquals(2, snacksSummary?.unitsSold)
        assertEquals(40.0, snacksSummary?.totalRevenue ?: 0.0, 0.001)
    }

    @Test
    fun testCategoryFilteringLogic() {
        val products = listOf(
            ProductEntity(id = 1L, name = "Coke", barcode = "11", category = "Beverages"),
            ProductEntity(id = 2L, name = "Chips", barcode = "22", category = "Snacks"),
            ProductEntity(id = 3L, name = "Sprite", barcode = "33", category = "Beverages"),
            ProductEntity(id = 4L, name = "Soap", barcode = "44", category = "General")
        )

        val allFilter = "All"
        val filteredAll = if (allFilter == "All") products else products.filter { it.category.equals(allFilter, true) }
        assertEquals(4, filteredAll.size)

        val bevFilter = "Beverages"
        val filteredBev = products.filter { it.category.equals(bevFilter, true) }
        assertEquals(2, filteredBev.size)
        assertTrue(filteredBev.all { it.category == "Beverages" })
    }
}
