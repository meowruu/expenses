package com.team.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * UI state for the Home Dashboard screen (Task 8).
 */
data class HomeUiState(
    val monthStart: Long,
    val monthEnd: Long
)

/**
 * ViewModel powering the Home Dashboard (Task 8).
 *
 * Exposes reactive [StateFlow] streams for:
 * - This month's total expenditure (paise)
 * - Category-wise spending breakdown
 * - Chronological recent transactions list
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _dateRange = MutableStateFlow(
        HomeUiState(
            monthStart = DateUtils.getCurrentMonthStart(),
            monthEnd = DateUtils.getCurrentMonthEnd()
        )
    )
    val dateRange: StateFlow<HomeUiState> = _dateRange.asStateFlow()

    /**
     * All recorded transactions ordered newest first.
     */
    val transactions: StateFlow<List<Transaction>> = transactionRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Total expenditure in paise for the selected month window.
     * Automatically excludes credits and self-transfers involving user-owned UPI IDs.
     */
    val monthTotalPaise: StateFlow<Long> = _dateRange
        .flatMapLatest { range ->
            transactionRepository.observeMonthTotal(range.monthStart, range.monthEnd)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0L
        )

    /**
     * Category-wise spending breakdown map (category -> totalPaise) for the selected month window.
     * Automatically excludes credits and self-transfers.
     */
    val categorySummary: StateFlow<Map<String, Long>> = _dateRange
        .flatMapLatest { range ->
            transactionRepository.observeCategorySummary(range.monthStart, range.monthEnd)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    /**
     * Updates the date window observed by this dashboard.
     *
     * @param monthStart Epoch timestamp in milliseconds.
     * @param monthEnd Epoch timestamp in milliseconds.
     */
    fun setDateRange(monthStart: Long, monthEnd: Long) {
        _dateRange.value = HomeUiState(monthStart = monthStart, monthEnd = monthEnd)
    }

    /**
     * Resets date window to the current calendar month.
     */
    fun resetToCurrentMonth() {
        setDateRange(
            monthStart = DateUtils.getCurrentMonthStart(),
            monthEnd = DateUtils.getCurrentMonthEnd()
        )
    }
}
