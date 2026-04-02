package com.please.stop.app.core.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.CategoryEntity
import com.please.stop.app.core.db.entity.ExpenseEntity
import com.please.stop.app.core.db.entity.UserProfileEntity

@Database(
    entities = [UserProfileEntity::class, CategoryEntity::class, ExpenseEntity::class],
    version = 3,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        const val NAME = "plzstop.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE IF EXISTS `placeholder`")
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_profile` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`displayName` TEXT, " +
                        "`currencyCode` TEXT NOT NULL, " +
                        "`monthlyBudget` INTEGER NOT NULL, " +
                        "`onboardingCompleted` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `category` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`iconKey` TEXT NOT NULL, " +
                        "`isDefault` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `expense` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`amountMinorUnits` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`categoryId` INTEGER NOT NULL, " +
                        "`dateEpochMillis` INTEGER NOT NULL, " +
                        "`notes` TEXT, " +
                        "`createdAtEpochMillis` INTEGER NOT NULL, " +
                        "`isDeleted` INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(`categoryId`) REFERENCES `category`(`id`) ON DELETE NO ACTION)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_expense_categoryId` ON `expense` (`categoryId`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_expense_dateEpochMillis` ON `expense` (`dateEpochMillis`)"
                )
            }
        }
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
