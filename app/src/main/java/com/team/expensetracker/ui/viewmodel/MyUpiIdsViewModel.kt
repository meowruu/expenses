package com.team.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.expensetracker.data.model.MyUpiId
import com.team.expensetracker.data.repository.MyUpiIdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing user's own UPI IDs (Task 18).
 *
 * Transfers involving these IDs are classified as self-transfers and excluded
 * from spending totals.
 */
class MyUpiIdsViewModel(
    private val myUpiIdRepository: MyUpiIdRepository
) : ViewModel() {

    /**
     * All registered user-owned UPI IDs.
     */
    val upiIds: StateFlow<List<MyUpiId>> = myUpiIdRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * Adds a new UPI ID to the user's self-accounts list.
     *
     * @param upiId The UPI ID string (e.g. "myname@okhdfcbank").
     */
    fun addUpiId(upiId: String) {
        val trimmed = upiId.trim()
        if (trimmed.isEmpty()) {
            _message.value = "UPI ID cannot be blank"
            return
        }

        viewModelScope.launch {
            try {
                myUpiIdRepository.add(trimmed)
                _message.value = "UPI ID added"
            } catch (e: Exception) {
                _message.value = "Failed to add UPI ID: ${e.message}"
            }
        }
    }

    /**
     * Removes a UPI ID from the user's self-accounts list.
     *
     * @param upiId The UPI ID string to remove.
     */
    fun removeUpiId(upiId: String) {
        val trimmed = upiId.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            try {
                myUpiIdRepository.remove(trimmed)
                _message.value = "UPI ID removed"
            } catch (e: Exception) {
                _message.value = "Failed to remove UPI ID: ${e.message}"
            }
        }
    }

    /**
     * Clears user feedback message.
     */
    fun clearMessage() {
        _message.value = null
    }
}
