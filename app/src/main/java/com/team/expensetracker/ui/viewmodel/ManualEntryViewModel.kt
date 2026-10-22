package com.team.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType
import com.team.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Manual Expense Entry screen (Task 7).
 *
 * Supports manually adding expenses: amount, note, category, date, optional UPI ID.
 */
class ManualEntryViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submissionSuccess = MutableStateFlow(false)
    val submissionSuccess: StateFlow<Boolean> = _submissionSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Submits a new manually logged expense.
     *
     * @param amountPaise Monetary amount in paise (1 INR = 100 paise). Long, never Float/Double.
     * @param note Short description or merchant note.
     * @param category The assigned expense category.
     * @param date Epoch millisecond timestamp (defaults to current time).
     * @param upiId Optional UPI identifier.
     * @param onResult Optional callback returning success flag.
     */
    fun addExpense(
        amountPaise: Long,
        note: String,
        category: String,
        date: Long = System.currentTimeMillis(),
        upiId: String? = null,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (amountPaise <= 0L) {
            _errorMessage.value = "Amount must be greater than zero"
            onResult(false)
            return
        }

        val trimmedCategory = category.trim()
        if (trimmedCategory.isEmpty()) {
            _errorMessage.value = "Category cannot be blank"
            onResult(false)
            return
        }

        val trimmedNote = note.trim()
        val transaction = Transaction(
            id = 0L,
            amountPaise = amountPaise,
            type = TransactionType.DEBIT,
            merchant = if (trimmedNote.isEmpty()) null else trimmedNote,
            upiId = if (upiId.isNullOrBlank()) null else upiId.trim(),
            refNumber = null, // Manual entries have no bank SMS UTR
            date = date,
            bank = "Manual",
            category = trimmedCategory
        )

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            try {
                val inserted = transactionRepository.insert(transaction)
                _submissionSuccess.value = inserted
                if (!inserted) {
                    _errorMessage.value = "Failed to record manual transaction"
                }
                onResult(inserted)
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                onResult(false)
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    /**
     * Resets submission status for new entries.
     */
    fun resetState() {
        _submissionSuccess.value = false
        _errorMessage.value = null
    }
}
