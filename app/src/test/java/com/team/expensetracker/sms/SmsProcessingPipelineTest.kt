package com.team.expensetracker.sms

import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType
import com.team.expensetracker.data.repository.PayeeRuleRepository
import com.team.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite for [SmsProcessingPipeline].
 *
 * Verifies end-to-end processing guarantees using pure JVM Kotlin:
 *  - Debit SMS parsing, categorization, and Long paise storage
 *  - Credit SMS parsing and persistence
 *  - Pre-parsing OTP rejection (must NEVER be stored)
 *  - Non-bank sender filtering (must be dropped immediately)
 *  - Bank reference number deduplication
 *  - User PayeeRule categorization precedence
 */
class SmsProcessingPipelineTest {

    private class FakeTransactionRepository : TransactionRepository {
        val inserted = mutableListOf<Transaction>()

        override suspend fun insert(tx: Transaction): Boolean {
            if (!tx.refNumber.isNullOrBlank() && inserted.any { it.refNumber == tx.refNumber }) {
                return false
            }
            inserted.add(tx)
            return true
        }

        override fun observeAll(): Flow<List<Transaction>> = flowOf(inserted)
        override fun observeMonthTotal(monthStart: Long, monthEnd: Long): Flow<Long> = flowOf(0L)
        override fun observeCategorySummary(monthStart: Long, monthEnd: Long): Flow<Map<String, Long>> = flowOf(emptyMap())
        override suspend fun updateCategory(id: Long, category: String) {}
        override suspend fun deleteAll() { inserted.clear() }
    }

    private class FakePayeeRuleRepository : PayeeRuleRepository {
        val rules = mutableMapOf<String, String>()

        override suspend fun getCategoryFor(upiId: String): String? = rules[upiId.trim().lowercase()]
        override suspend fun saveRule(upiId: String, category: String) {
            rules[upiId.trim().lowercase()] = category
        }
    }

    private lateinit var transactionRepo: FakeTransactionRepository
    private lateinit var payeeRuleRepo: FakePayeeRuleRepository
    private lateinit var pipeline: SmsProcessingPipeline

    @Before
    fun setUp() {
        transactionRepo = FakeTransactionRepository()
        payeeRuleRepo = FakePayeeRuleRepository()
        pipeline = SmsProcessingPipeline(
            transactionRepository = transactionRepo,
            payeeRuleRepository = payeeRuleRepo
        )
    }

    @Test
    fun `debit SMS from bank sender is parsed categorized and saved in Long paise`() = runBlocking {
        val sms = "Dear Customer, Rs.450.00 has been debited from account XX1234 being a UPI transaction to swiggy@icici (UPI Ref no 123456789012)."
        val sender = "AX-AXISBK"

        val success = pipeline.processIncomingSms(body = sms, sender = sender)

        assertTrue("Expected processIncomingSms to return true", success)
        assertEquals(1, transactionRepo.inserted.size)

        val tx = transactionRepo.inserted.first()
        assertEquals(45000L, tx.amountPaise) // 450 INR = 45,000 paise
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("swiggy@icici", tx.upiId)
        assertEquals("123456789012", tx.refNumber)
        assertEquals("Food", tx.category) // Categorized by Swiggy
        assertTrue("Bank name must be non-empty", tx.bank.isNotBlank())
    }

    @Test
    fun `credit SMS from bank sender is parsed and saved`() = runBlocking {
        val sms = "INR 2,500.00 credited to A/c XX5678 on 22-Oct-26 by transfer from friend@fakebank. UPI Ref: 334455667788. Avail Bal: INR 15,000.00"
        val sender = "HDFCBK"

        val success = pipeline.processIncomingSms(body = sms, sender = sender)

        assertTrue("Expected processIncomingSms to return true", success)
        assertEquals(1, transactionRepo.inserted.size)

        val tx = transactionRepo.inserted.first()
        assertEquals(250000L, tx.amountPaise) // 2500 INR = 250,000 paise
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("334455667788", tx.refNumber)
        assertEquals("HDFC", tx.bank)
    }

    @Test
    fun `OTP SMS is rejected and NEVER saved`() = runBlocking {
        val otpSms = "Your OTP for SBI transaction is 482910. Valid for 5 minutes. Do not share OTP with anyone."
        val sender = "SBIINB"

        val result = pipeline.processIncomingSms(body = otpSms, sender = sender)

        assertFalse("Expected OTP message to be rejected", result)
        assertTrue("Zero transactions must be saved for OTP messages", transactionRepo.inserted.isEmpty())
    }

    @Test
    fun `non-bank sender is ignored and NEVER saved`() = runBlocking {
        val sms = "Dear Customer, Rs.500.00 has been debited from account XX1234 being a UPI transaction to swiggy@icici (UPI Ref no 123456789012)."
        val personalSender = "+919876543210"

        val result = pipeline.processIncomingSms(body = sms, sender = personalSender)

        assertFalse("Expected non-bank sender to be rejected", result)
        assertTrue("Zero transactions must be saved for non-bank senders", transactionRepo.inserted.isEmpty())
    }

    @Test
    fun `duplicate bank reference number is rejected by repository`() = runBlocking {
        val sms = "Dear Customer, Rs.300.00 debited from A/c XX1234 via UPI Ref no 998877665544."
        val sender = "AXISBK"

        val firstInsert = pipeline.processIncomingSms(body = sms, sender = sender)
        assertTrue("First insert must succeed", firstInsert)
        assertEquals(1, transactionRepo.inserted.size)

        val secondInsert = pipeline.processIncomingSms(body = sms, sender = sender)
        assertFalse("Duplicate refNumber must be rejected", secondInsert)
        assertEquals("List size must remain 1 after duplicate rejection", 1, transactionRepo.inserted.size)
    }

    @Test
    fun `user payee rule override takes precedence over default categorizer heuristics`() = runBlocking {
        // Register custom rule: swiggy@icici -> "Bills" instead of default "Food"
        payeeRuleRepo.saveRule("swiggy@icici", "Bills")

        val sms = "Dear Customer, Rs.450.00 debited from account XX1234 to swiggy@icici. Ref: 112233445566."
        val sender = "VK-HDFCBK"

        val success = pipeline.processIncomingSms(body = sms, sender = sender)
        assertTrue(success)

        val tx = transactionRepo.inserted.first()
        assertEquals("Bills", tx.category) // User rule override respected!
    }
}
