package com.team.expensetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.team.expensetracker.data.local.converter.Converters
import com.team.expensetracker.data.local.dao.MyUpiIdDao
import com.team.expensetracker.data.local.dao.PayeeRuleDao
import com.team.expensetracker.data.local.dao.TransactionDao
import com.team.expensetracker.data.local.entity.MyUpiIdEntity
import com.team.expensetracker.data.local.entity.PayeeRuleEntity
import com.team.expensetracker.data.local.entity.TransactionEntity

/**
 * Main Room database for the expense tracker application.
 *
 * Privacy Invariant:
 * Strictly stores structured transaction metadata, payee category rules, and user-owned UPI IDs.
 * Does NOT contain any raw SMS message body or sensitive unparsed strings.
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
        MyUpiIdEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun payeeRuleDao(): PayeeRuleDao
    abstract fun myUpiIdDao(): MyUpiIdDao

    companion object {
        const val DATABASE_NAME = "expense_tracker.db"

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
         * Destructive migration is deliberately NOT enabled to prevent accidental data loss.
         */
        private fun buildDatabase(
            context: Context,
            openHelperFactory: SupportSQLiteOpenHelper.Factory?
        ): AppDatabase {
            val builder = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )

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
