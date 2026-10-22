package com.team.expensetracker.data

import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType
import com.team.expensetracker.ui.util.formatRupees

/**
 * Category summary item representing aggregate spending per category.
 */
data class CategorySummary(
    val category: String,
    val totalPaise: Long,
    val transactionCount: Int
) {
    fun formattedTotal(): String = formatRupees(totalPaise)
}

/**
 * Hardcoded dataset for prototyping and screen development.
 * Mirrors B2's Transaction data class.
 */
object FakeData {

    val CATEGORIES = listOf(
        "Food & Dining",
        "Groceries",
        "Shopping",
        "Bills & Utilities",
        "Transport",
        "Entertainment",
        "Health",
        "Investment",
        "Personal",
        "Other"
    )

    private val now = System.currentTimeMillis()
    private const val ONE_HOUR = 3600_000L
    private const val ONE_DAY = 86400_000L

    val initialTransactions: List<Transaction> = listOf(
        Transaction(
            id = 1L,
            amountPaise = 34900L, // ₹349.00
            type = TransactionType.DEBIT,
            merchant = "Swiggy",
            upiId = "swiggy@icici",
            refNumber = "UPI/429482910394",
            date = now - (2 * ONE_HOUR),
            bank = "AXIS",
            category = "Food & Dining"
        ),
        Transaction(
            id = 2L,
            amountPaise = 28550L, // ₹285.50
            type = TransactionType.DEBIT,
            merchant = "Zepto",
            upiId = "zepto@axisbank",
            refNumber = "UPI/429482719283",
            date = now - (5 * ONE_HOUR),
            bank = "AXIS",
            category = "Groceries"
        ),
        Transaction(
            id = 3L,
            amountPaise = 42000L, // ₹420.00
            type = TransactionType.DEBIT,
            merchant = "Uber India",
            upiId = "uber@hdfcbank",
            refNumber = "UPI/429381729182",
            date = now - ONE_DAY,
            bank = "HDFC",
            category = "Transport"
        ),
        Transaction(
            id = 4L,
            amountPaise = 142000L, // ₹1,420.00
            type = TransactionType.DEBIT,
            merchant = "Electricity Bill",
            upiId = "tneb@sbi",
            refNumber = "UPI/429281928374",
            date = now - (2 * ONE_DAY),
            bank = "SBI",
            category = "Bills & Utilities"
        ),
        Transaction(
            id = 5L,
            amountPaise = 219900L, // ₹2,199.00
            type = TransactionType.DEBIT,
            merchant = "Amazon India",
            upiId = "amazon@apl",
            refNumber = "UPI/429182736452",
            date = now - (3 * ONE_DAY),
            bank = "ICICI",
            category = "Shopping"
        ),
        Transaction(
            id = 6L,
            amountPaise = 64900L, // ₹649.00
            type = TransactionType.DEBIT,
            merchant = "Netflix India",
            upiId = "netflix@kotak",
            refNumber = "UPI/429081726354",
            date = now - (4 * ONE_DAY),
            bank = "HDFC",
            category = "Entertainment"
        ),
        Transaction(
            id = 7L,
            amountPaise = 58000L, // ₹580.00
            type = TransactionType.DEBIT,
            merchant = "Apollo Pharmacy",
            upiId = "apollopharmacy@icici",
            refNumber = "UPI/428981625344",
            date = now - (5 * ONE_DAY),
            bank = "SBI",
            category = "Health"
        ),
        Transaction(
            id = 8L,
            amountPaise = 15000L, // ₹150.00 (Refund)
            type = TransactionType.CREDIT,
            merchant = "Swiggy Refund",
            upiId = "swiggy@icici",
            refNumber = "UPI/428871625341",
            date = now - (6 * ONE_DAY),
            bank = "AXIS",
            category = "Food & Dining"
        )
    )

    /**
     * Calculates total spent in paise for DEBIT transactions in the current calendar month.
     * Uses java.util.Calendar to compare year + month against the transaction's epoch-millis date.
     */
    fun calculateTotalSpent(transactions: List<Transaction>): Long {
        val now = java.util.Calendar.getInstance()
        val currentYear = now.get(java.util.Calendar.YEAR)
        val currentMonth = now.get(java.util.Calendar.MONTH)
        return transactions
            .filter { it.type == TransactionType.DEBIT }
            .filter {
                val txCal = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
                txCal.get(java.util.Calendar.YEAR) == currentYear &&
                    txCal.get(java.util.Calendar.MONTH) == currentMonth
            }
            .sumOf { it.amountPaise }
    }

    /**
     * Calculates category-wise summary map for spending (category -> totalPaise).
     */
    fun calculateCategoryTotals(transactions: List<Transaction>): Map<String, Long> {
        val debits = transactions.filter { it.type == TransactionType.DEBIT }
        return debits
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amountPaise } }
    }

    /**
     * Calculates category-wise summary list for spending.
     */
    fun calculateCategorySummaries(transactions: List<Transaction>): List<CategorySummary> {
        val debits = transactions.filter { it.type == TransactionType.DEBIT }
        return debits
            .groupBy { it.category }
            .map { (cat, list) ->
                CategorySummary(
                    category = cat,
                    totalPaise = list.sumOf { it.amountPaise },
                    transactionCount = list.size
                )
            }
            .sortedByDescending { it.totalPaise }
    }
}
