package com.team.expensetracker.data.repository

import com.team.expensetracker.data.fake.FakeMyUpiIdDao
import com.team.expensetracker.data.fake.FakePayeeRuleDao
import com.team.expensetracker.data.fake.FakeTransactionDao
import com.team.expensetracker.data.local.entity.MyUpiIdEntity
import com.team.expensetracker.data.local.entity.PayeeRuleEntity
import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [TransactionRepositoryImpl] and core business logic contracts.
 */
class TransactionRepositoryTest {

    private lateinit var fakeMyUpiIdDao: FakeMyUpiIdDao
    private lateinit var fakePayeeRuleDao: FakePayeeRuleDao
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var repository: TransactionRepository

    @Before
    fun setUp() {
        fakeMyUpiIdDao = FakeMyUpiIdDao()
        fakePayeeRuleDao = FakePayeeRuleDao()
        fakeTransactionDao = FakeTransactionDao(fakeMyUpiIdDao)
        repository = TransactionRepositoryImpl(
            dao = fakeTransactionDao,
            payeeRuleDao = fakePayeeRuleDao,
            myUpiIdDao = fakeMyUpiIdDao
        )
    }

    @Test
    fun testTransactionInsertionAndRetrieval() = runTest {
        val tx = Transaction(
            id = 0L,
            amountPaise = 25000L, // Rs. 250.00
            type = TransactionType.DEBIT,
            merchant = "Swiggy",
            upiId = "swiggy@icici",
            refNumber = "REF123456",
            date = 10_000L,
            bank = "HDFC",
            category = "Food"
        )

        val inserted = repository.insert(tx)
        assertTrue("Insert should return true for first insertion", inserted)

        val all = repository.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(25000L, all[0].amountPaise)
        assertEquals("Swiggy", all[0].merchant)
        assertEquals("REF123456", all[0].refNumber)
    }

    @Test
    fun testDuplicateReferenceNumberRejection() = runTest {
        val tx1 = Transaction(
            id = 0L,
            amountPaise = 10000L,
            type = TransactionType.DEBIT,
            merchant = "Uber",
            upiId = "uber@axis",
            refNumber = "DUPLICATE_REF_001",
            date = 10_000L,
            bank = "AXIS",
            category = "Travel"
        )
        val tx2 = tx1.copy(amountPaise = 50000L, merchant = "Different Merchant")

        val firstInsert = repository.insert(tx1)
        assertTrue("First insert must succeed", firstInsert)

        val duplicateInsert = repository.insert(tx2)
        assertFalse("Duplicate ref number must be rejected and return false", duplicateInsert)

        val all = repository.observeAll().first()
        assertEquals("Only one transaction should be persisted", 1, all.size)
        assertEquals(10000L, all[0].amountPaise)
    }

    @Test
    fun testMissingOrNullReferenceNumbersCanBeInsertedMultipleTimes() = runTest {
        val txWithoutRef1 = Transaction(
            id = 0L,
            amountPaise = 1500L,
            type = TransactionType.DEBIT,
            merchant = "Local Tea Stall",
            upiId = null,
            refNumber = null,
            date = 10_000L,
            bank = "Cash",
            category = "Food"
        )
        val txWithoutRef2 = Transaction(
            id = 0L,
            amountPaise = 2000L,
            type = TransactionType.DEBIT,
            merchant = "Local Bakery",
            upiId = null,
            refNumber = "   ", // Blank string should also normalize to null
            date = 11_000L,
            bank = "Cash",
            category = "Food"
        )

        assertTrue(repository.insert(txWithoutRef1))
        assertTrue("Transactions without reference numbers should not collide", repository.insert(txWithoutRef2))

        val all = repository.observeAll().first()
        assertEquals(2, all.size)
    }

    @Test
    fun testTransactionCategoryUpdate() = runTest {
        val tx = Transaction(
            id = 0L,
            amountPaise = 30000L,
            type = TransactionType.DEBIT,
            merchant = "Amazon",
            upiId = "amazon@apl",
            refNumber = "REF999",
            date = 10_000L,
            bank = "SBI",
            category = "General"
        )
        repository.insert(tx)
        val saved = repository.observeAll().first()[0]

        repository.updateCategory(saved.id, "Shopping")

        val updated = repository.observeAll().first()[0]
        assertEquals("Shopping", updated.category)
    }

    @Test
    fun testDeleteAllWipesData() = runTest {
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = 5000L,
                type = TransactionType.DEBIT,
                merchant = "Grocery",
                upiId = null,
                refNumber = "REF1",
                date = 10_000L,
                bank = "HDFC",
                category = "Groceries"
            )
        )
        assertEquals(1, repository.observeAll().first().size)

        repository.deleteAll()
        assertEquals(0, repository.observeAll().first().size)
    }

    @Test
    fun testMonthlyTotalsAndDateWindow() = runTest {
        val windowStart = 10_000L
        val windowEnd = 20_000L

        // Inside window: 5000 paise
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = 5000L,
                type = TransactionType.DEBIT,
                merchant = "M1",
                upiId = null,
                refNumber = "R1",
                date = 15_000L,
                bank = "HDFC",
                category = "Food"
            )
        )

        // Inside window: 7500 paise
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = 7500L,
                type = TransactionType.DEBIT,
                merchant = "M2",
                upiId = null,
                refNumber = "R2",
                date = 20_000L,
                bank = "HDFC",
                category = "Shopping"
            )
        )

        // Before window: 10000 paise (should be ignored)
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = 10000L,
                type = TransactionType.DEBIT,
                merchant = "M3",
                upiId = null,
                refNumber = "R3",
                date = 5_000L,
                bank = "HDFC",
                category = "Bills"
            )
        )

        val total = repository.observeMonthTotal(windowStart, windowEnd).first()
        assertEquals(12500L, total)
    }

    @Test
    fun testExcludingCreditTransactionsFromSpending() = runTest {
        val windowStart = 10_000L
        val windowEnd = 20_000L

        // Debit: 4000 paise
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = 4000L,
                type = TransactionType.DEBIT,
                merchant = "Coffee",
                upiId = null,
                refNumber = "D1",
                date = 12_000L,
                bank = "HDFC",
                category = "Food"
            )
        )

        // Credit (Refund or Salary): 50000 paise
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = 50000L,
                type = TransactionType.CREDIT,
                merchant = "Salary/Refund",
                upiId = null,
                refNumber = "C1",
                date = 14_000L,
                bank = "HDFC",
                category = "Income"
            )
        )

        val total = repository.observeMonthTotal(windowStart, windowEnd).first()
        assertEquals("Credit transactions must be excluded from spending totals", 4000L, total)

        val summary = repository.observeCategorySummary(windowStart, windowEnd).first()
        assertEquals(1, summary.size)
        assertEquals(4000L, summary["Food"])
        assertFalse("Income category from Credit must not appear in spending summary", summary.containsKey("Income"))
    }

    @Test
    fun testExcludingSelfTransfers() = runTest {
        val windowStart = 10_000L
        val windowEnd = 20_000L

        // User registers own UPI ID
        fakeMyUpiIdDao.add(MyUpiIdEntity("user@okhdfcbank"))

        // Self-transfer to user's own account: 15000 paise
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = 15000L,
                type = TransactionType.DEBIT,
                merchant = "Self Transfer",
                upiId = "user@okhdfcbank",
                refNumber = "SELF_001",
                date = 15_000L,
                bank = "HDFC",
                category = "Transfer"
            )
        )

        // Normal expenditure: 3000 paise
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = 3000L,
                type = TransactionType.DEBIT,
                merchant = "Book Store",
                upiId = "books@icici",
                refNumber = "BOOK_001",
                date = 16_000L,
                bank = "HDFC",
                category = "Education"
            )
        )

        val total = repository.observeMonthTotal(windowStart, windowEnd).first()
        assertEquals("Self-transfers to user's own UPI IDs must be excluded", 3000L, total)

        val summary = repository.observeCategorySummary(windowStart, windowEnd).first()
        assertEquals(1, summary.size)
        assertEquals(3000L, summary["Education"])
    }

    @Test
    fun testReactiveUpdatesWhenOwnUpiIdIsAddedLater() = runTest {
        val windowStart = 10_000L
        val windowEnd = 20_000L

        // Transaction logged to accountBeforeRegistration
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = 8000L,
                type = TransactionType.DEBIT,
                merchant = "Savings Wallet",
                upiId = "wallet@paytm",
                refNumber = "TX_WALLET",
                date = 15_000L,
                bank = "Paytm",
                category = "Transfer"
            )
        )

        // Total initially counts it
        val initialTotal = repository.observeMonthTotal(windowStart, windowEnd).first()
        assertEquals(8000L, initialTotal)

        // Now user marks wallet@paytm as their own UPI ID
        fakeMyUpiIdDao.add(MyUpiIdEntity("wallet@paytm"))

        // Observe reactive update: total drops to 0
        val updatedTotal = repository.observeMonthTotal(windowStart, windowEnd).first()
        assertEquals("Total must reactively exclude newly registered own-UPI ID", 0L, updatedTotal)
    }

    @Test
    fun testEmptyDatabaseBehavior() = runTest {
        val total = repository.observeMonthTotal(0L, Long.MAX_VALUE).first()
        assertEquals(0L, total)

        val summary = repository.observeCategorySummary(0L, Long.MAX_VALUE).first()
        assertTrue(summary.isEmpty())

        val list = repository.observeAll().first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun testCorrectLongPaiseIntegerArithmetic() = runTest {
        // Test massive paise amounts without overflow or precision error
        // 100,000,000 INR = 10,000,000,000 paise (10 Billion paise)
        val largeAmount1 = 10_000_000_000L
        val largeAmount2 = 5_000_000_000L

        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = largeAmount1,
                type = TransactionType.DEBIT,
                merchant = "Real Estate",
                upiId = null,
                refNumber = "BIG1",
                date = 5_000L,
                bank = "HDFC",
                category = "Property"
            )
        )
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = largeAmount2,
                type = TransactionType.DEBIT,
                merchant = "Vehicle",
                upiId = null,
                refNumber = "BIG2",
                date = 6_000L,
                bank = "HDFC",
                category = "Automobile"
            )
        )

        val total = repository.observeMonthTotal(0L, 10_000L).first()
        assertEquals(15_000_000_000L, total)
    }

    @Test
    fun testManualEntryWithNullUpiIdCountsInMonthTotalAndCategorySummary() = runTest {
        val windowStart = 1_000L
        val windowEnd = 20_000L

        // Manual cash entry has upiId = null
        val manualCashTx = Transaction(
            id = 0L,
            amountPaise = 45000L, // Rs. 450
            type = TransactionType.DEBIT,
            merchant = "Local Vendor",
            upiId = null,
            refNumber = null,
            date = 10_000L,
            bank = "Cash",
            category = "Groceries"
        )
        assertTrue(repository.insert(manualCashTx))

        // Month total must count rows where upiId IS NULL
        val total = repository.observeMonthTotal(windowStart, windowEnd).first()
        assertEquals(45000L, total)

        // Category summary must also aggregate rows where upiId IS NULL
        val summary = repository.observeCategorySummary(windowStart, windowEnd).first()
        assertEquals(1, summary.size)
        assertEquals(45000L, summary["Groceries"])
    }

    @Test
    fun testSelfTransferIsExcluded() = runTest {
        val windowStart = 1_000L
        val windowEnd = 20_000L

        // Register user's own UPI ID
        fakeMyUpiIdDao.add(MyUpiIdEntity(upiId = "myaccount@okhdfcbank"))

        // Self-transfer transaction
        val selfTransfer = Transaction(
            id = 0L,
            amountPaise = 500000L, // Rs. 5000
            type = TransactionType.DEBIT,
            merchant = "Self",
            upiId = "myaccount@okhdfcbank",
            refNumber = "SELF_TRF_1",
            date = 5_000L,
            bank = "HDFC",
            category = "Transfer"
        )

        // Legitimate expense
        val normalExpense = Transaction(
            id = 0L,
            amountPaise = 15000L, // Rs. 150
            type = TransactionType.DEBIT,
            merchant = "Coffee Shop",
            upiId = "coffeeshop@icici",
            refNumber = "EXP_001",
            date = 6_000L,
            bank = "HDFC",
            category = "Food"
        )

        assertTrue(repository.insert(selfTransfer))
        assertTrue(repository.insert(normalExpense))

        // Self-transfer must be excluded; total must be only 15000L
        val total = repository.observeMonthTotal(windowStart, windowEnd).first()
        assertEquals(15000L, total)

        val summary = repository.observeCategorySummary(windowStart, windowEnd).first()
        assertEquals(1, summary.size)
        assertEquals(15000L, summary["Food"])
        assertFalse(summary.containsKey("Transfer"))
    }

    @Test
    fun testUpiIdCaseAndWhitespaceDifferencesTreatedAsSame() = runTest {
        val windowStart = 1_000L
        val windowEnd = 20_000L

        // User registers own UPI ID with surrounding whitespace and mixed case
        fakeMyUpiIdDao.add(MyUpiIdEntity(upiId = "  MyName@OkHdfcBank  "))

        // Self-transfer recorded in lowercase/trimmed format
        val selfTransfer1 = Transaction(
            id = 0L,
            amountPaise = 20000L,
            type = TransactionType.DEBIT,
            merchant = "Self",
            upiId = "myname@okhdfcbank",
            refNumber = "CASE_1",
            date = 5_000L,
            bank = "HDFC",
            category = "Transfer"
        )

        // Self-transfer recorded in uppercase format
        val selfTransfer2 = Transaction(
            id = 0L,
            amountPaise = 30000L,
            type = TransactionType.DEBIT,
            merchant = "Self",
            upiId = "  MYNAME@OKHDFCBANK  ",
            refNumber = "CASE_2",
            date = 6_000L,
            bank = "HDFC",
            category = "Transfer"
        )

        // Legitimate merchant expense
        val merchantTx = Transaction(
            id = 0L,
            amountPaise = 12000L,
            type = TransactionType.DEBIT,
            merchant = "Store",
            upiId = "store@upi",
            refNumber = "MERCH_1",
            date = 7_000L,
            bank = "HDFC",
            category = "Shopping"
        )

        assertTrue(repository.insert(selfTransfer1))
        assertTrue(repository.insert(selfTransfer2))
        assertTrue(repository.insert(merchantTx))

        // Both self-transfers must be excluded regardless of case or whitespace differences
        val total = repository.observeMonthTotal(windowStart, windowEnd).first()
        assertEquals(12000L, total)

        val summary = repository.observeCategorySummary(windowStart, windowEnd).first()
        assertEquals(1, summary.size)
        assertEquals(12000L, summary["Shopping"])
    }

    @Test
    fun testDeleteAllDataEmptiesAllThreeTablesAndSummariesResetToZero() = runTest {
        val windowStart = 1_000L
        val windowEnd = 20_000L

        // Populate table 1: transactions
        repository.insert(
            Transaction(
                id = 0L,
                amountPaise = 25000L,
                type = TransactionType.DEBIT,
                merchant = "Food Delivery",
                upiId = "delivery@icici",
                refNumber = "WIPE_001",
                date = 5_000L,
                bank = "HDFC",
                category = "Food"
            )
        )

        // Populate table 2: payee_rules
        fakePayeeRuleDao.saveRule(PayeeRuleEntity(upiId = "delivery@icici", category = "Food"))

        // Populate table 3: my_upi_ids
        fakeMyUpiIdDao.add(MyUpiIdEntity(upiId = "user@okhdfcbank"))

        // Verify data exists
        assertEquals(1, repository.observeAll().first().size)
        assertEquals(1, fakePayeeRuleDao.items.value.size)
        assertEquals(1, fakeMyUpiIdDao.items.value.size)
        assertEquals(25000L, repository.observeMonthTotal(windowStart, windowEnd).first())
        assertEquals(1, repository.observeCategorySummary(windowStart, windowEnd).first().size)

        // Execute wipe
        repository.deleteAll()

        // Confirm all 3 tables are empty
        assertEquals(0, repository.observeAll().first().size)
        assertEquals(0, fakePayeeRuleDao.items.value.size)
        assertEquals(0, fakeMyUpiIdDao.items.value.size)

        // Confirm Flow-based summaries go back to 0 and empty
        val totalAfterWipe = repository.observeMonthTotal(windowStart, windowEnd).first()
        assertEquals("Month total Flow must return 0 after wipe", 0L, totalAfterWipe)

        val summaryAfterWipe = repository.observeCategorySummary(windowStart, windowEnd).first()
        assertTrue("Category summary Flow must be empty after wipe", summaryAfterWipe.isEmpty())
    }
}
