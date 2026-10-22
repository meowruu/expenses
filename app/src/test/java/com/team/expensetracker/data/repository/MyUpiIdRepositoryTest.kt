package com.team.expensetracker.data.repository

import com.team.expensetracker.data.fake.FakeMyUpiIdDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MyUpiIdRepositoryImpl].
 */
class MyUpiIdRepositoryTest {

    private lateinit var fakeDao: FakeMyUpiIdDao
    private lateinit var repository: MyUpiIdRepository

    @Before
    fun setUp() {
        fakeDao = FakeMyUpiIdDao()
        repository = MyUpiIdRepositoryImpl(fakeDao)
    }

    @Test
    fun testAddAndObserveOwnUpiIds() = runTest {
        repository.add("primary@okhdfcbank")
        repository.add("secondary@icici")

        val list = repository.observeAll().first()
        assertEquals(2, list.size)
        assertTrue(list.any { it.upiId == "primary@okhdfcbank" })
        assertTrue(list.any { it.upiId == "secondary@icici" })
    }

    @Test
    fun testDuplicateAdditionIsIdempotent() = runTest {
        repository.add("user@upi")
        repository.add("user@upi")

        val list = repository.observeAll().first()
        assertEquals(1, list.size)
    }

    @Test
    fun testIsMineReturnsCorrectly() = runTest {
        repository.add("mywallet@axis")

        assertTrue(repository.isMine("mywallet@axis"))
        assertFalse(repository.isMine("stranger@axis"))
        assertFalse(repository.isMine(""))
    }

    @Test
    fun testRemoveUpiId() = runTest {
        repository.add("temp@upi")
        assertTrue(repository.isMine("temp@upi"))

        repository.remove("temp@upi")
        assertFalse(repository.isMine("temp@upi"))
        assertTrue(repository.observeAll().first().isEmpty())
    }
}
