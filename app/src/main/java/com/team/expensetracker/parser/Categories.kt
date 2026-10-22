package com.team.expensetracker.parser

/**
 * Standard expense and transaction categories defined across project contracts and specifications.
 *
 * Finalized Shared Category Taxonomy:
 * - Food, Groceries, Transport, Fuel, Shopping, Bills, Entertainment, Health, Education, Rent, Transfers, Uncategorized
 */
object Categories {
    /** Food delivery, restaurants, cafes, fast food, and dining. */
    const val FOOD = "Food"

    /** Supermarkets, instant grocery delivery, hypermarkets, vegetable stores, and dairy. */
    const val GROCERIES = "Groceries"

    /** Cabs, auto-rickshaws, metro, railways, flights, bus tickets, tolls, and parking. */
    const val TRANSPORT = "Transport"

    /** Petrol, diesel, CNG, and fuel stations. */
    const val FUEL = "Fuel"

    /** E-commerce, apparel, electronics, footwear, jewelry, and retail merchandise. */
    const val SHOPPING = "Shopping"

    /** Electricity, water, cooking gas, mobile recharge, broadband, DTH, and utility bills. */
    const val BILLS = "Bills"

    /** Movies, cinema, streaming subscriptions, music, and events. */
    const val ENTERTAINMENT = "Entertainment"

    /** Pharmacies, hospitals, clinics, medical diagnostics, doctors, and chemist shops. */
    const val HEALTH = "Health"

    /** School, college, university, tuition fees, coaching institutes, and educational courses. */
    const val EDUCATION = "Education"

    /** House rent, landlord payments, society maintenance, and apartment fees. */
    const val RENT = "Rent"

    /** Person-to-person bank transfers, internal self-transfers, NEFT/IMPS, and peer transfers. */
    const val TRANSFERS = "Transfers"

    /** Default fallback category applied when no user rule or keyword matches. */
    const val UNCATEGORIZED = "Uncategorized"

    /**
     * Complete set of valid categories recognized by the application.
     */
    val ALL: Set<String> = setOf(
        FOOD,
        GROCERIES,
        TRANSPORT,
        FUEL,
        SHOPPING,
        BILLS,
        ENTERTAINMENT,
        HEALTH,
        EDUCATION,
        RENT,
        TRANSFERS,
        UNCATEGORIZED
    )

    /**
     * Verifies if a given category name is part of the standardized taxonomy.
     */
    fun isValid(category: String): Boolean = ALL.contains(category)
}
