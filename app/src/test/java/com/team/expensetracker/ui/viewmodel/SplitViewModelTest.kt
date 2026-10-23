package com.team.expensetracker.ui.viewmodel

import com.team.expensetracker.data.fake.FakeSplitShareDao
import com.team.expensetracker.data.repository.SplitRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [SplitViewModel] reactive StateFlows and actions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplitViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeSplitShareDao
    private lateinit var repository: SplitRepositoryImpl
    private lateinit var viewModel: SplitViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeSplitShareDao()
        repository = SplitRepositoryImpl(fakeDao)
        viewModel = SplitViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSplitEquallyUpdatesCurrentTransactionSharesAndOwedPerPerson() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.owedPerPerson.collect()
        }

        viewModel.splitEqually(
            transactionId = 42L,
            totalPaise = 10000L,
            people = listOf("Alice", "Bob", "Charlie")
        )

        testScheduler.advanceUntilIdle()

        val shares = viewModel.currentTransactionShares.value
        assertEquals(3, shares.size)
        assertEquals(10000L, shares.sumOf { it.sharePaise })

        val owed = viewModel.owedPerPerson.value
        assertEquals(3334L, owed["Alice"])
        assertEquals(3333L, owed["Bob"])
        assertEquals(3333L, owed["Charlie"])
    }

    @Test
    fun testMarkSettledReflectsInOwedPerPerson() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.owedPerPerson.collect()
        }

        var shareIdToSettle = 0L
        viewModel.splitEqually(
            transactionId = 10L,
            totalPaise = 6000L,
            people = listOf("Alice", "Bob")
        ) { createdShares ->
            shareIdToSettle = createdShares.first { it.personName == "Alice" }.id
        }

        testScheduler.advanceUntilIdle()
        assertEquals(3000L, viewModel.owedPerPerson.value["Alice"])

        viewModel.markSettled(shareIdToSettle, settled = true)
        testScheduler.advanceUntilIdle()

        val updatedOwed = viewModel.owedPerPerson.value
        assertTrue("Alice should no longer be in owedPerPerson once settled", !updatedOwed.containsKey("Alice"))
        assertEquals(3000L, updatedOwed["Bob"])
    }
}
