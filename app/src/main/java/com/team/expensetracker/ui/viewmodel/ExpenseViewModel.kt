package com.team.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.expensetracker.data.model.MyUpiId
import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType
import com.team.expensetracker.data.repository.MyUpiIdRepository
import com.team.expensetracker.data.repository.PayeeRuleRepository
import com.team.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Unified ViewModel providing all core data streams and operations for Compose UI screens.
 *
 * Exposes reactive state for:
 * - Dashboard: Month total, category summary, recent transaction list.
 * - Transactions: Category editing with automatic payee rule learning.
 * - Manual Entry: Logging new transactions directly.
 * - Privacy: "Delete all data" wipe routine.
 * - Self-Transfers: Managing user's own UPI IDs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(
    private val transactionRepository: TransactionRepository,
    private val payeeRuleRepository: PayeeRuleRepository,
    private val myUpiIdRepository: MyUpiIdRepository
) : ViewModel() {

    private val _monthWindow = MutableStateFlow(
        Pair(DateUtils.getCurrentMonthStart(), DateUtils.getCurrentMonthEnd())
    )

    /**
     * Observable stream of all transactions ordered newest first.
     */
    val transactions: StateFlow<List<Transaction>> = transactionRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Observable total expenditure (in paise) for current month window.
     * Automatically excludes credits and self-transfers.
     */
    val monthTotalPaise: StateFlow<Long> = _monthWindow
        .flatMapLatest { (start, end) ->
            transactionRepository.observeMonthTotal(start, end)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0L
        )

    /**
     * Observable category-wise spending breakdown map (Category -> Amount in Paise).
     */
    val categorySummary: StateFlow<Map<String, Long>> = _monthWindow
        .flatMapLatest { (start, end) ->
            transactionRepository.observeCategorySummary(start, end)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    /**
     * Observable stream of user-owned UPI IDs.
     */
    val myUpiIds: StateFlow<List<MyUpiId>> = myUpiIdRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    /**
     * Sets the active month range for expenditure calculation.
     */
    fun setMonthRange(start: Long, end: Long) {
        _monthWindow.value = Pair(start, end)
    }

    /**
     * Inserts a transaction into storage.
     *
     * @param tx The parsed transaction.
     * @param onResult Callback receiving true if saved, false if duplicate.
     */
    fun insertTransaction(tx: Transaction, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = transactionRepository.insert(tx)
            onResult(success)
        }
    }

    /**
     * Adds a manual expense.
     */
    fun addManualExpense(
        amountPaise: Long,
        note: String,
        category: String,
        date: Long = System.currentTimeMillis(),
        upiId: String? = null,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (amountPaise <= 0L || category.isBlank()) {
            _statusMessage.value = "Invalid amount or category"
            onResult(false)
            return
        }

        val tx = Transaction(
            id = 0L,
            amountPaise = amountPaise,
            type = TransactionType.DEBIT,
            merchant = note.trim().ifEmpty { null },
            upiId = upiId?.trim()?.ifEmpty { null },
            refNumber = null,
            date = date,
            bank = "Manual",
            category = category.trim()
        )

        viewModelScope.launch {
            val inserted = transactionRepository.insert(tx)
            if (inserted) {
                _statusMessage.value = "Expense logged"
            }
            onResult(inserted)
        }
    }

    /**
     * Updates transaction category and optionally saves payee rule.
     */
    fun updateCategory(
        id: Long,
        category: String,
        upiId: String? = null,
        saveAsRule: Boolean = false
    ) {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            transactionRepository.updateCategory(id, trimmed)
            if (saveAsRule && !upiId.isNullOrBlank()) {
                payeeRuleRepository.saveRule(upiId.trim(), trimmed)
            }
            _statusMessage.value = "Category updated"
        }
    }

    /**
     * Adds a user-owned UPI ID for self-transfer exclusion.
     */
    fun addMyUpiId(upiId: String) {
        val trimmed = upiId.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            myUpiIdRepository.add(trimmed)
            _statusMessage.value = "Added own UPI ID"
        }
    }

    /**
     * Removes a user-owned UPI ID.
     */
    fun removeMyUpiId(upiId: String) {
        val trimmed = upiId.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            myUpiIdRepository.remove(trimmed)
            _statusMessage.value = "Removed UPI ID"
        }
    }

    /**
     * Wipes all transaction data (Task 27 data-wipe).
     */
    fun deleteAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            transactionRepository.deleteAll()
            _statusMessage.value = "All data wiped"
            onComplete()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
