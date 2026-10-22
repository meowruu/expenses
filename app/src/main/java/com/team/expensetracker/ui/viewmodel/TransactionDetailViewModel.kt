package com.team.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.expensetracker.data.repository.PayeeRuleRepository
import com.team.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Transaction Detail screen (Task 9).
 *
 * Allows viewing and editing transaction categories and propagating custom category
 * preferences to [PayeeRuleRepository].
 */
class TransactionDetailViewModel(
    private val transactionRepository: TransactionRepository,
    private val payeeRuleRepository: PayeeRuleRepository
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * Updates the category of a transaction.
     *
     * @param id Transaction unique ID.
     * @param newCategory The new category name.
     * @param upiId Optional payee UPI ID. If provided and [saveAsRule] is true,
     *              persists a payee rule to remember this category for future transactions.
     * @param saveAsRule If true, saves the category mapping in [PayeeRuleRepository].
     */
    fun updateCategory(
        id: Long,
        newCategory: String,
        upiId: String? = null,
        saveAsRule: Boolean = false
    ) {
        val trimmedCategory = newCategory.trim()
        if (trimmedCategory.isEmpty()) {
            _message.value = "Category cannot be blank"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                transactionRepository.updateCategory(id = id, category = trimmedCategory)

                if (saveAsRule && !upiId.isNullOrBlank()) {
                    payeeRuleRepository.saveRule(upiId = upiId.trim(), category = trimmedCategory)
                }

                _message.value = "Category updated successfully"
            } catch (e: Exception) {
                _message.value = "Failed to update category: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Clears user feedback messages.
     */
    fun clearMessage() {
        _message.value = null
    }
}
