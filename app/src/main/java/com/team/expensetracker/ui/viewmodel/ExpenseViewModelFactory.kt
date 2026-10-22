package com.team.expensetracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.team.expensetracker.data.di.DataLayerContainer

/**
 * ViewModelProvider Factory that instantiates ViewModels using dependencies from [DataLayerContainer].
 */
class ExpenseViewModelFactory(
    private val container: DataLayerContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(container.transactionRepository) as T
            }
            modelClass.isAssignableFrom(TransactionDetailViewModel::class.java) -> {
                TransactionDetailViewModel(
                    container.transactionRepository,
                    container.payeeRuleRepository
                ) as T
            }
            modelClass.isAssignableFrom(ManualEntryViewModel::class.java) -> {
                ManualEntryViewModel(container.transactionRepository) as T
            }
            modelClass.isAssignableFrom(MyUpiIdsViewModel::class.java) -> {
                MyUpiIdsViewModel(container.myUpiIdRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    transactionRepository = container.transactionRepository,
                    payeeRuleDao = container.database.payeeRuleDao(),
                    myUpiIdDao = container.database.myUpiIdDao()
                ) as T
            }
            modelClass.isAssignableFrom(ExpenseViewModel::class.java) -> {
                ExpenseViewModel(
                    container.transactionRepository,
                    container.payeeRuleRepository,
                    container.myUpiIdRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        /**
         * Convenience factory creator taking an Android [Context].
         */
        fun create(context: Context): ExpenseViewModelFactory {
            return ExpenseViewModelFactory(DataLayerContainer.getInstance(context))
        }
    }
}
