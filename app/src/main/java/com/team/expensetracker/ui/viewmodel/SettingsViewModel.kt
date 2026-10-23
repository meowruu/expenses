package com.team.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.expensetracker.data.local.dao.MyUpiIdDao
import com.team.expensetracker.data.local.dao.PayeeRuleDao
import com.team.expensetracker.data.local.dao.SplitShareDao
import com.team.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings and Privacy screen (Task 27).
 *
 * Implements the backend logic for the "Delete all data" wipe routine and
 * privacy disclosure features.
 */
class SettingsViewModel(
    private val transactionRepository: TransactionRepository,
    private val payeeRuleDao: PayeeRuleDao? = null,
    private val myUpiIdDao: MyUpiIdDao? = null,
    private val splitShareDao: SplitShareDao? = null
) : ViewModel() {

    private val _isWiping = MutableStateFlow(false)
    val isWiping: StateFlow<Boolean> = _isWiping.asStateFlow()

    private val _wipeCompleted = MutableStateFlow(false)
    val wipeCompleted: StateFlow<Boolean> = _wipeCompleted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Wipes all persisted data from the local database:
     * clears transactions, payee_rules, and my_upi_ids tables in one place.
     * Fulfills B4's data-wipe requirement (Task 27).
     *
     * @param onComplete Optional callback invoked upon completion.
     */
    fun deleteAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isWiping.value = true
            _errorMessage.value = null
            try {
                transactionRepository.deleteAll()
                payeeRuleDao?.deleteAll()
                myUpiIdDao?.deleteAll()
                splitShareDao?.deleteAll()
                _wipeCompleted.value = true
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to wipe data: ${e.message}"
            } finally {
                _isWiping.value = false
            }
        }
    }

    /**
     * Resets the wipe completed state flag.
     */
    fun resetWipeState() {
        _wipeCompleted.value = false
        _errorMessage.value = null
    }
}
