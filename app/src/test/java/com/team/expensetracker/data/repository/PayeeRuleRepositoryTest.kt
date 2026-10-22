package com.team.expensetracker.data.repository

import com.team.expensetracker.data.fake.FakePayeeRuleDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PayeeRuleRepositoryImpl].
 */
class PayeeRuleRepositoryTest {

    private lateinit var fakeDao: FakePayeeRuleDao
    private lateinit var repository: PayeeRuleRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakePayeeRuleDao()
        repository = PayeeRuleRepositoryImpl(fakeDao)
    }

    @Test
    fun testGetCategoryForNonExistentPayeeReturnsNull() = runTest {
        val category = repository.getCategoryFor("unknown@upi")
        assertNull(category)
    }

    @Test
    fun testSaveAndRetrievePayeeRule() = runTest {
        repository.saveRule("swiggy@icici", "Food & Dining")

        val category = repository.getCategoryFor("swiggy@icici")
        assertEquals("Food & Dining", category)
    }

    @Test
    fun testUpdateExistingPayeeRule() = runTest {
        repository.saveRule("merchant@okaxis", "Shopping")
        assertEquals("Shopping", repository.getCategoryFor("merchant@okaxis"))

        repository.saveRule("merchant@okaxis", "Groceries")
        assertEquals("Groceries", repository.getCategoryFor("merchant@okaxis"))
    }

    @Test
    fun testWhitespaceHandling() = runTest {
        repository.saveRule("  netflix@axis  ", "  Entertainment  ")
        assertEquals("Entertainment", repository.getCategoryFor("netflix@axis"))
        assertEquals("Entertainment", repository.getCategoryFor(" netflix@axis "))
    }

    @Test
    fun testObserveAllRules() = runTest {
        repository.saveRule("b_merchant@upi", "Bills")
        repository.saveRule("a_merchant@upi", "Auto")

        val all = repository.observeAll().first()
        assertEquals(2, all.size)
        assertEquals("a_merchant@upi", all[0].upiId)
        assertEquals("b_merchant@upi", all[1].upiId)
    }
}
