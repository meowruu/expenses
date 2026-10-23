package com.team.expensetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.team.expensetracker.data.local.converter.Converters
import com.team.expensetracker.data.local.dao.MyUpiIdDao
import com.team.expensetracker.data.local.dao.PayeeRuleDao
import com.team.expensetracker.data.local.dao.SplitShareDao
import com.team.expensetracker.data.local.dao.TransactionDao
import com.team.expensetracker.data.local.entity.MyUpiIdEntity
import com.team.expensetracker.data.local.entity.PayeeRuleEntity
import com.team.expensetracker.data.local.entity.SplitShareEntity
import com.team.expensetracker.data.local.entity.TransactionEntity

/**
 * Main Room database for the expense tracker application.
 *
 * Privacy Invariant:
 * Strictly stores structured transaction metadata, payee category rules, user-owned UPI IDs,
 * and local split shares. Does NOT contain any raw SMS message body or sensitive unparsed strings.
 *
 * Encryption Integration (Task 25):
 * Prepared for B4's SQLCipher database encryption. Callers may supply an optional
 * [SupportSQLiteOpenHelper.Factory] (e.g. SQLCipher SupportOpenHelperFactory) to
 * [getInstance].
 */
@Database(
    entities = [
        TransactionEntity::class,
        PayeeRuleEntity::class,
        MyUpiIdEntity::class,
        SplitShareEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun payeeRuleDao(): PayeeRuleDao
    abstract fun myUpiIdDao(): MyUpiIdDao
    abstract fun splitShareDao(): SplitShareDao

    companion object {
        const val DATABASE_NAME = "expense_tracker.db"

        /**
         * Migration from version 1 to 2:
         * Adds the `split_shares` table and associated indices for the local split expense feature.
         * Existing data in `transactions`, `payee_rules`, and `my_upi_ids` is completely preserved.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `split_shares` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transactionId` INTEGER NOT NULL,
                        `personName` TEXT NOT NULL,
                        `sharePaise` INTEGER NOT NULL,
                        `settled` INTEGER NOT NULL,
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_shares_transactionId` ON `split_shares` (`transactionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_shares_personName` ON `split_shares` (`personName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_shares_settled` ON `split_shares` (`settled`)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the thread-safe singleton instance of [AppDatabase].
         *
         * @param context Application or activity context.
         * @param openHelperFactory Optional SQLite open helper factory, allowing B4 to
         *                          inject SQLCipher encryption seamlessly.
         */
        fun getInstance(
            context: Context,
            openHelperFactory: SupportSQLiteOpenHelper.Factory? = null
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext, openHelperFactory).also {
                    INSTANCE = it
                }
            }
        }

        /**
         * Internal builder for database creation.
         * Registers [MIGRATION_1_2] so existing data is preserved across schema upgrades.
         * Destructive migration is deliberately NOT enabled.
         */
        private fun buildDatabase(
            context: Context,
            openHelperFactory: SupportSQLiteOpenHelper.Factory?
        ): AppDatabase {
            val builder = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(MIGRATION_1_2)

            if (openHelperFactory != null) {
                builder.openHelperFactory(openHelperFactory)
            }

            return builder.build()
        }

        /**
         * Resets the singleton instance (primarily for testing purposes).
         */
        @androidx.annotation.VisibleForTesting
        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
