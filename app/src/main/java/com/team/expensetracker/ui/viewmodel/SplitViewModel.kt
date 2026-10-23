package com.team.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.expensetracker.data.model.SplitShare
import com.team.expensetracker.data.repository.SplitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Split Expense feature.
 *
 * Exposes:
 * - [owedPerPerson]: StateFlow containing each debtor's name and their total unsettled debt in paise.
 * - [splitEqually]: Splits a transaction amount equally across a list of names.
 * - [markSettled]: Toggles/marks a split share as settled.
 * - [currentTransactionShares]: StateFlow of shares for the currently inspected transaction.
 */
class SplitViewModel(
    private val splitRepository: SplitRepository
) : ViewModel() {

    /**
     * Map of personName to total unsettled paise owed by that individual.
     * Reactively updates whenever shares are inserted or settled.
     */
    val owedPerPerson: StateFlow<Map<String, Long>> = splitRepository.observeOwedPerPerson()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _currentTransactionShares = MutableStateFlow<List<SplitShare>>(emptyList())
    val currentTransactionShares: StateFlow<List<SplitShare>> = _currentTransactionShares.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Observes shares for a specific transaction and keeps [currentTransactionShares] updated.
     */
    fun loadSharesForTransaction(transactionId: Long) {
        viewModelScope.launch {
            splitRepository.observeSharesForTransaction(transactionId).collect { shares ->
                _currentTransactionShares.value = shares
            }
        }
    }

    /**
     * Splits an expense of [totalPaise] equally among [people].
     *
     * Remainder paise is distributed across participants so that
     * the sum of shares matches [totalPaise] exactly.
     *
     * @param transactionId ID of the transaction to split.
     * @param totalPaise Total expense amount in paise.
     * @param people List of people splitting the expense.
     * @param onComplete Optional callback returning the created [SplitShare] list.
     */
    fun splitEqually(
        transactionId: Long,
        totalPaise: Long,
        people: List<String>,
        onComplete: (List<SplitShare>) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            try {
                val shares = splitRepository.splitEqually(transactionId, totalPaise, people)
                _currentTransactionShares.value = shares
                onComplete(shares)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to split expense: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Updates the settled status of a specific share.
     *
     * @param shareId Primary key of the share to update.
     * @param settled True if paid/settled, false if still owed.
     * @param onComplete Callback invoked upon database update.
     */
    fun markSettled(shareId: Long, settled: Boolean = true, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                splitRepository.markSettled(shareId, settled)
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update settlement: ${e.message}"
            }
        }
    }
}
