package com.team.expensetracker.data.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.team.expensetracker.data.local.AppDatabase
import com.team.expensetracker.data.repository.MyUpiIdRepository
import com.team.expensetracker.data.repository.MyUpiIdRepositoryImpl
import com.team.expensetracker.data.repository.PayeeRuleRepository
import com.team.expensetracker.data.repository.PayeeRuleRepositoryImpl
import com.team.expensetracker.data.repository.TransactionRepository
import com.team.expensetracker.data.repository.TransactionRepositoryImpl

/**
 * Lightweight, framework-free service locator for data layer dependencies.
 *
 * Provides thread-safe, singleton access to repositories and the Room database.
 * Supports injecting an optional [SupportSQLiteOpenHelper.Factory] for B4's SQLCipher encryption.
 */
class DataLayerContainer(
    context: Context,
    openHelperFactory: SupportSQLiteOpenHelper.Factory? = null
) {
    val database: AppDatabase = AppDatabase.getInstance(
        context = context.applicationContext,
        openHelperFactory = openHelperFactory
    )

    val transactionRepository: TransactionRepository = TransactionRepositoryImpl(
        dao = database.transactionDao()
    )

    val payeeRuleRepository: PayeeRuleRepository = PayeeRuleRepositoryImpl(
        dao = database.payeeRuleDao()
    )

    val myUpiIdRepository: MyUpiIdRepository = MyUpiIdRepositoryImpl(
        dao = database.myUpiIdDao()
    )

    companion object {
        @Volatile
        private var INSTANCE: DataLayerContainer? = null

        /**
         * Obtains the global [DataLayerContainer] instance.
         */
        fun getInstance(
            context: Context,
            openHelperFactory: SupportSQLiteOpenHelper.Factory? = null
        ): DataLayerContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataLayerContainer(
                    context = context.applicationContext,
                    openHelperFactory = openHelperFactory
                ).also { INSTANCE = it }
            }
        }

        @androidx.annotation.VisibleForTesting
        fun reset() {
            synchronized(this) {
                AppDatabase.resetInstance()
                INSTANCE = null
            }
        }
    }
}
