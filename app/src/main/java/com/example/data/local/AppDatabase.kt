package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AppSettingsDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.CreditTransactionDao
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.DeliveryDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.SaleDao
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.DeliveryItemEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        DeliveryEntity::class,
        DeliveryItemEntity::class,
        AppSettingsEntity::class,
        CustomerEntity::class,
        CreditTransactionEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleDao(): SaleDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun customerDao(): CustomerDao
    abstract fun creditTransactionDao(): CreditTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create categories table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                """.trimIndent())

                // 2. Add category column with default 'General' to products table
                try {
                    db.execSQL("ALTER TABLE `products` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'General'")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `sales` ADD COLUMN `cashTendered` REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `sales` ADD COLUMN `changeDue` REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_1_7 = object : Migration(1, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                """.trimIndent())
                try {
                    db.execSQL("ALTER TABLE `products` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'General'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `sales` ADD COLUMN `cashTendered` REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `sales` ADD COLUMN `changeDue` REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                """.trimIndent())
                try {
                    db.execSQL("ALTER TABLE `products` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'General'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `sales` ADD COLUMN `cashTendered` REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `sales` ADD COLUMN `changeDue` REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `products` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `products` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `customers` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `credit_transactions` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "astilla_pos.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_1_7,
                        MIGRATION_5_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
