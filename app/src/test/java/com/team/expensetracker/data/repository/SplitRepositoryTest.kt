package com.team.expensetracker.data.repository

import com.team.expensetracker.data.fake.FakeSplitShareDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [SplitRepositoryImpl] behavior and integer paise arithmetic.
 */
class SplitRepositoryTest {

    private lateinit var fakeDao: FakeSplitShareDao
    private lateinit var repository: SplitRepository

    @Before
    fun setUp() {
        fakeDao = FakeSplitShareDao()
        repository = SplitRepositoryImpl(fakeDao)
    }

    @Test
    fun testEqualSplitOf10000PaiseAmong3PeopleSumsTo10000() = runTest {
        val totalPaise = 10000L // Rs. 100.00
        val people = listOf("Alice", "Bob", "Charlie")

        val shares = repository.splitEqually(
            transactionId = 1L,
            totalPaise = totalPaise,
            people = people
        )

        assertEquals("Should create exactly 3 shares", 3, shares.size)

        // 10000 / 3 = 3333 with remainder 1.
        // Remainder 1 is distributed to the first person.
        assertEquals("Alice", shares[0].personName)
        assertEquals(3334L, shares[0].sharePaise)

        assertEquals("Bob", shares[1].personName)
        assertEquals(3333L, shares[1].sharePaise)

        assertEquals("Charlie", shares[2].personName)
        assertEquals(3333L, shares[2].sharePaise)

        // Exact sum guarantee
        val sumOfShares = shares.sumOf { it.sharePaise }
        assertEquals("Sum of shares must equal totalPaise with zero rounding loss", totalPaise, sumOfShares)

        // All new shares should initially be unsettled
        assertTrue("All shares must be unsettled initially", shares.all { !it.settled })
    }

    @Test
    fun testOwedTotalsPerPerson() = runTest {
        // Transaction 1: Alice gets 2500, Bob gets 2500
        repository.splitEqually(1L, 5000L, listOf("Alice", "Bob"))

        // Transaction 2: Bob gets 4000, Charlie gets 4000
        repository.splitEqually(2L, 8000L, listOf("Bob", "Charlie"))

        val owedMap = repository.observeOwedPerPerson().first()

        assertEquals(2500L, owedMap["Alice"])
        assertEquals(6500L, owedMap["Bob"]) // 2500 + 4000
        assertEquals(4000L, owedMap["Charlie"])
    }

    @Test
    fun testSettledSharesExcludedFromOwedTotals() = runTest {
        val shares = repository.splitEqually(1L, 10000L, listOf("Alice", "Bob"))
        val aliceShare = shares.first { it.personName == "Alice" }
        val bobShare = shares.first { it.personName == "Bob" }

        // Both are owed initially
        val initialOwed = repository.observeOwedPerPerson().first()
        assertEquals(5000L, initialOwed["Alice"])
        assertEquals(5000L, initialOwed["Bob"])

        // Mark Alice's share settled
        repository.markSettled(aliceShare.id, settled = true)

        val updatedOwed = repository.observeOwedPerPerson().first()
        assertFalse("Settled debtor must not be in owed map", updatedOwed.containsKey("Alice"))
        assertEquals("Bob's unsettled share remains owed", 5000L, updatedOwed["Bob"])

        // Transaction shares reflection
        val txShares = repository.observeSharesForTransaction(1L).first()
        assertTrue("Alice's share must be marked settled", txShares.first { it.id == aliceShare.id }.settled)
        assertFalse("Bob's share must still be unsettled", txShares.first { it.id == bobShare.id }.settled)
    }

    @Test
    fun testDeleteAllClearsSplits() = runTest {
        repository.splitEqually(1L, 6000L, listOf("Alice", "Bob", "Charlie"))
        assertEquals(3, fakeDao.shares.value.size)

        repository.deleteAll()

        assertEquals("Fake DAO shares must be empty after deleteAll", 0, fakeDao.shares.value.size)
        val owedMap = repository.observeOwedPerPerson().first()
        assertTrue("Owed per person map must be empty after deleteAll", owedMap.isEmpty())
    }

    @Test
    fun testReSplitReplacesPreviousSharesForSameTransaction() = runTest {
        // Initial split between 2 people
        repository.splitEqually(1L, 4000L, listOf("Alice", "Bob"))
        assertEquals(2, repository.observeSharesForTransaction(1L).first().size)

        // Re-split same transaction among 3 people
        repository.splitEqually(1L, 6000L, listOf("Alice", "Charlie", "David"))
        val updatedShares = repository.observeSharesForTransaction(1L).first()

        assertEquals(3, updatedShares.size)
        assertEquals(listOf("Alice", "Charlie", "David"), updatedShares.map { it.personName })
    }
}
