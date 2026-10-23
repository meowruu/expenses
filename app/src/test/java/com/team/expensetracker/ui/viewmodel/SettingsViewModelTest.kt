package com.team.expensetracker.ui.viewmodel

import com.team.expensetracker.data.fake.FakeMyUpiIdDao
import com.team.expensetracker.data.fake.FakePayeeRuleDao
import com.team.expensetracker.data.fake.FakeSplitShareDao
import com.team.expensetracker.data.fake.FakeTransactionDao
import com.team.expensetracker.data.local.entity.MyUpiIdEntity
import com.team.expensetracker.data.local.entity.PayeeRuleEntity
import com.team.expensetracker.data.local.entity.SplitShareEntity
import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType
import com.team.expensetracker.data.repository.TransactionRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [SettingsViewModel] data wipe behavior across all tables.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeMyUpiIdDao: FakeMyUpiIdDao
    private lateinit var fakePayeeRuleDao: FakePayeeRuleDao
    private lateinit var fakeSplitShareDao: FakeSplitShareDao
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var repository: TransactionRepositoryImpl
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeMyUpiIdDao = FakeMyUpiIdDao()
        fakePayeeRuleDao = FakePayeeRuleDao()
        fakeSplitShareDao = FakeSplitShareDao()
        fakeTransactionDao = FakeTransactionDao(fakeMyUpiIdDao)
        repository = TransactionRepositoryImpl(
            dao = fakeTransactionDao,
            payeeRuleDao = fakePayeeRuleDao,
            myUpiIdDao = fakeMyUpiIdDao,
            splitShareDao = fakeSplitShareDao
        )
        viewModel = SettingsViewModel(
            transactionRepository = repository,
            payeeRuleDao = fakePayeeRuleDao,
            myUpiIdDao = fakeMyUpiIdDao,
            splitShareDao = fakeSplitShareDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDeleteAllDataClearsAllFourTablesAndSummariesResetToZero() = runTest {
        val now = System.currentTimeMillis()

        // 1. Insert transaction
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = 32000L,
                type = TransactionType.DEBIT,
                merchant = "Retail Mart",
                upiId = "retail@upi",
                refNumber = "REF_SETT_1",
                date = now,
                bank = "HDFC",
                category = "Shopping"
            )
        )

        // 2. Insert payee rule
        fakePayeeRuleDao.saveRule(PayeeRuleEntity(upiId = "retail@upi", category = "Shopping"))

        // 3. Insert own UPI ID
        fakeMyUpiIdDao.add(MyUpiIdEntity(upiId = "self@okhdfcbank"))

        // 4. Insert split share
        fakeSplitShareDao.insertShare(
            SplitShareEntity(
                transactionId = 1L,
                personName = "Bob",
                sharePaise = 16000L,
                settled = false
            )
        )

        // Verify pre-wipe state
        assertEquals(1, repository.observeAll().first().size)
        assertEquals(1, fakePayeeRuleDao.items.value.size)
        assertEquals(1, fakeMyUpiIdDao.items.value.size)
        assertEquals(1, fakeSplitShareDao.shares.value.size)

        var callbackInvoked = false
        viewModel.deleteAllData {
            callbackInvoked = true
        }

        testScheduler.advanceUntilIdle()

        // Verify callback and completion state
        assertTrue("Callback should be invoked upon wipe completion", callbackInvoked)
        assertTrue("wipeCompleted StateFlow should be true", viewModel.wipeCompleted.value)
        assertFalse("isWiping StateFlow should be false after completion", viewModel.isWiping.value)

        // Confirm all tables are completely empty
        assertEquals("Transactions table must be empty", 0, repository.observeAll().first().size)
        assertEquals("Payee rules table must be empty", 0, fakePayeeRuleDao.items.value.size)
        assertEquals("My UPI IDs table must be empty", 0, fakeMyUpiIdDao.items.value.size)
        assertEquals("Split shares table must be empty", 0, fakeSplitShareDao.shares.value.size)

        // Confirm Flow-based summaries go back to 0
        val monthTotal = repository.observeMonthTotal(0L, Long.MAX_VALUE).first()
        assertEquals("Month total Flow must return 0 after wipe", 0L, monthTotal)

        val categorySummary = repository.observeCategorySummary(0L, Long.MAX_VALUE).first()
        assertTrue("Category summary Flow must be empty map after wipe", categorySummary.isEmpty())
    }
}
