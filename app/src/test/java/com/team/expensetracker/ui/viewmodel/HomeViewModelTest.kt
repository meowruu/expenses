package com.team.expensetracker.ui.viewmodel

import com.team.expensetracker.data.fake.FakeMyUpiIdDao
import com.team.expensetracker.data.fake.FakeTransactionDao
import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType
import com.team.expensetracker.data.repository.TransactionRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [HomeViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeTransactionDao
    private lateinit var repository: TransactionRepositoryImpl
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeTransactionDao(FakeMyUpiIdDao())
        repository = TransactionRepositoryImpl(fakeDao)
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialDashboardStateIsEmpty() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.transactions.first().size)
        assertEquals(0L, viewModel.monthTotalPaise.first())
        assertEquals(0, viewModel.categorySummary.first().size)
    }

    @Test
    fun testDashboardReflectsInsertedTransactions() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.transactions.collect()
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.monthTotalPaise.collect()
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categorySummary.collect()
        }

        val now = System.currentTimeMillis()
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = 15000L,
                type = TransactionType.DEBIT,
                merchant = "Grocery Store",
                upiId = null,
                refNumber = "REF100",
                date = now,
                bank = "HDFC",
                category = "Groceries"
            )
        )

        testScheduler.advanceUntilIdle()

        val txList = viewModel.transactions.value
        assertEquals(1, txList.size)
        assertEquals("Grocery Store", txList[0].merchant)

        val total = viewModel.monthTotalPaise.value
        assertEquals(15000L, total)

        val categories = viewModel.categorySummary.value
        assertEquals(15000L, categories["Groceries"])
    }
}
