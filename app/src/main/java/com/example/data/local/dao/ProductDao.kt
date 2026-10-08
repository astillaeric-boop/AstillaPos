package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

data class CategorySalesSummary(
    val category: String,
    val unitsSold: Int,
    val totalRevenue: Double
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT DISTINCT category FROM products ORDER BY category ASC")
    fun getAllDistinctProductCategories(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category AND (name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProductsWithCategory(query: String, category: String): Flow<List<ProductEntity>>

    /**
     * Sales revenue & units grouped by product category for completed sales in timeframe.
     */
    @Query("""
        SELECT 
            COALESCE(p.category, 'General') AS category,
            COALESCE(SUM(si.quantity), 0) AS unitsSold,
            COALESCE(SUM(si.unitPrice * si.quantity), 0.0) AS totalRevenue
        FROM sale_items si
        INNER JOIN sales s ON si.saleId = s.id
        LEFT JOIN products p ON si.productId = p.id
        WHERE s.timestamp >= :startDate AND s.timestamp <= :endDate
        GROUP BY COALESCE(p.category, 'General')
        ORDER BY totalRevenue DESC
    """)
    fun getSalesByCategory(startDate: Long, endDate: Long): Flow<List<CategorySalesSummary>>

    @Query("""
        SELECT 
            COALESCE(p.category, 'General') AS category,
            COALESCE(SUM(si.quantity), 0) AS unitsSold,
            COALESCE(SUM(si.unitPrice * si.quantity), 0.0) AS totalRevenue
        FROM sale_items si
        INNER JOIN sales s ON si.saleId = s.id
        LEFT JOIN products p ON si.productId = p.id
        WHERE s.timestamp >= :startDate AND s.timestamp <= :endDate
        GROUP BY COALESCE(p.category, 'General')
        ORDER BY totalRevenue DESC
    """)
    suspend fun getSalesByCategoryDirect(startDate: Long, endDate: Long): List<CategorySalesSummary>

    @Query("SELECT COALESCE(SUM(stockQuantity * costPrice), 0.0) FROM products WHERE stockQuantity > 0")
    fun getTotalInventoryValue(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = :newStock WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Int)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :quantity WHERE id = :productId")
    suspend fun decreaseStock(productId: Long, quantity: Int)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :quantity WHERE id = :productId")
    suspend fun increaseStock(productId: Long, quantity: Int)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}
