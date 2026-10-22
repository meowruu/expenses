package com.team.expensetracker.parser

import com.team.expensetracker.data.model.PayeeRule
import com.team.expensetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for [TransactionCategorizer] and [DefaultTransactionCategorizer].
 *
 * Adheres strictly to privacy and testing rules:
 * - Uses only fake/masked test data and fake merchant names.
 * - Asserts category validity against contract-defined categories ([Categories.ALL]).
 */
class TransactionCategorizerTest {

    private val categorizer = DefaultTransactionCategorizer()

    // =========================================================================
    // 1. Food Category Detection (Delivery, Restaurants, Cafes)
    // =========================================================================

    @Test
    fun categorize_foodMerchants_categorizesAsFood() {
        val foodSamples = listOf(
            Pair("FAKE SWIGGY", "fakeswiggy@fakebank"),
            Pair("FAKE ZOMATO DELIVERY", "zomato@fakebank"),
            Pair("FAKE CORNER CAFE & BAKERY", "cornercafe@fakebank"),
            Pair("MCDONALD FAST FOOD", "mcdonald@fakebank"),
            Pair("CHAAYOS TEA LOUNGE", "chaayos@fakebank"),
            Pair("LOCAL SWEETS & MITHAI", "sweets@fakebank")
        )

        for ((merchant, upiId) in foodSamples) {
            val result = categorizer.categorize(merchant = merchant, upiId = upiId)
            assertEquals("Expected Food for ($merchant, $upiId)", Categories.FOOD, result)
        }
    }

    // =========================================================================
    // 1b. Groceries Category Detection (Supermarkets, Instant Delivery, Dairy)
    // =========================================================================

    @Test
    fun categorize_groceryMerchants_categorizesAsGroceries() {
        val grocerySamples = listOf(
            Pair("FAKE BLINKIT COMMERCE", "blinkit@fakebank"),
            Pair("ZEPTO GROCERY DELIVERY", "zepto@fakebank"),
            Pair("BIGBASKET FRESH", "bbdaily@fakebank"),
            Pair("DMART SUPERMARKET", "dmart@fakebank"),
            Pair("AMUL DAIRY PARLOUR", "amuldairy@fakebank"),
            Pair("LOCAL KIRANA STORE", "kirana@fakebank")
        )

        for ((merchant, upiId) in grocerySamples) {
            val result = categorizer.categorize(merchant = merchant, upiId = upiId)
            assertEquals("Expected Groceries for ($merchant, $upiId)", Categories.GROCERIES, result)
        }
    }

    // =========================================================================
    // 2. Shopping Category Detection (E-Commerce, Fashion, Electronics)
    // =========================================================================

    @Test
    fun categorize_shoppingMerchants_categorizesAsShopping() {
        val shoppingSamples = listOf(
            Pair("AMAZON INDIA SELLER", "amazonpay@fakebank"),
            Pair("FLIPKART INTERNET", "flipkart@fakebank"),
            Pair("MYNTRA FASHION", "myntra@fakebank"),
            Pair("AJIO TRENDS STORE", "ajio@fakebank"),
            Pair("CROMA RETAIL ELECTRONICS", "croma@fakebank"),
            Pair("DECATHLON SPORTS STORE", "decathlon@fakebank"),
            Pair("ZARA APPARELS", "zara@fakebank"),
            Pair("H&M CLOTHING STORE", "hnm@fakebank"),
            Pair("LENSKART OPTICALS", "lenskart@fakebank"),
            Pair("CITY MALL FOOTWEAR", "footwear@fakebank")
        )

        for ((merchant, upiId) in shoppingSamples) {
            val result = categorizer.categorize(merchant = merchant, upiId = upiId)
            assertEquals("Expected Shopping for ($merchant, $upiId)", Categories.SHOPPING, result)
        }
    }

    // =========================================================================
    // 3. Bills Category Detection (Utilities & Telecom only)
    // =========================================================================

    @Test
    fun categorize_billsAndUtilities_categorizesAsBills() {
        val billsSamples = listOf(
            Pair("BESCOM ELECTRICITY", "bescom@fakebank"),
            Pair("AIRTEL PREPAID RECHARGE", "airtel@fakebank"),
            Pair("JIO FIBER BROADBAND", "jiofiber@fakebank")
        )

        for ((merchant, upiId) in billsSamples) {
            val result = categorizer.categorize(merchant = merchant, upiId = upiId)
            assertEquals("Expected Bills for ($merchant, $upiId)", Categories.BILLS, result)
        }
    }

    // =========================================================================
    // 3b. Transport Category Detection (Cabs, Metro, Railways, Buses, Flights)
    // =========================================================================

    @Test
    fun categorize_transportMerchants_categorizesAsTransport() {
        val transportSamples = listOf(
            Pair("UBER RIDES INDIA", "uber@fakebank"),
            Pair("OLA CABS TRAVEL", "olacabs@fakebank"),
            Pair("IRCTC TRAIN TICKET", "irctc@fakebank"),
            Pair("RAPIDO BIKE TAXI", "rapido@fakebank")
        )

        for ((merchant, upiId) in transportSamples) {
            val result = categorizer.categorize(merchant = merchant, upiId = upiId)
            assertEquals("Expected Transport for ($merchant, $upiId)", Categories.TRANSPORT, result)
        }
    }

    // =========================================================================
    // 3c. Fuel Category Detection (Petrol Stations, CNG, Diesel)
    // =========================================================================

    @Test
    fun categorize_fuelMerchants_categorizesAsFuel() {
        val fuelSamples = listOf(
            Pair("HPCL PETROL PUMP", "hpclfuel@fakebank"),
            Pair("BPCL FUEL STATION", "bpcl@fakebank"),
            Pair("INDIAN OIL PUMP", "indianoil@fakebank")
        )

        for ((merchant, upiId) in fuelSamples) {
            val result = categorizer.categorize(merchant = merchant, upiId = upiId)
            assertEquals("Expected Fuel for ($merchant, $upiId)", Categories.FUEL, result)
        }
    }

    // =========================================================================
    // 3d. Entertainment Category Detection (Streaming, Cinema)
    // =========================================================================

    @Test
    fun categorize_entertainmentMerchants_categorizesAsEntertainment() {
        val entertainmentSamples = listOf(
            Pair("NETFLIX ENTERTAINMENT", "netflix@fakebank"),
            Pair("SPOTIFY MUSIC SUBSCRIPTION", "spotify@fakebank"),
            Pair("PVR CINEMAS BOOKING", "pvr@fakebank")
        )

        for ((merchant, upiId) in entertainmentSamples) {
            val result = categorizer.categorize(merchant = merchant, upiId = upiId)
            assertEquals("Expected Entertainment for ($merchant, $upiId)", Categories.ENTERTAINMENT, result)
        }
    }

    // =========================================================================
    // 3e. Health Category Detection (Pharmacies, Hospitals, Clinics)
    // =========================================================================

    @Test
    fun categorize_healthMerchants_categorizesAsHealth() {
        val healthSamples = listOf(
            Pair("APOLLO PHARMACY MEDICAL", "apollopharmacy@fakebank"),
            Pair("NETMEDS ONLINE PHARMACY", "netmeds@fakebank"),
            Pair("CITY HOSPITAL CLINIC", "cityhospital@fakebank")
        )

        for ((merchant, upiId) in healthSamples) {
            val result = categorizer.categorize(merchant = merchant, upiId = upiId)
            assertEquals("Expected Health for ($merchant, $upiId)", Categories.HEALTH, result)
        }
    }

    // =========================================================================
    // 3f. Education Category Detection (Schools, Coaching, Courses)
    // =========================================================================

    @Test
    fun categorize_educationMerchants_categorizesAsEducation() {
        val educationSamples = listOf(
            Pair("CITY PUBLIC SCHOOL FEES", "schoolfees@fakebank"),
            Pair("BYJUS LEARNING APP", "byjus@fakebank"),
            Pair("LOCAL COACHING CENTRE", "coaching@fakebank")
        )

        for ((merchant, upiId) in educationSamples) {
            val result = categorizer.categorize(merchant = merchant, upiId = upiId)
            assertEquals("Expected Education for ($merchant, $upiId)", Categories.EDUCATION, result)
        }
    }

    // =========================================================================
    // 3g. Rent Category Detection (Housing, Maintenance)
    // =========================================================================

    @Test
    fun categorize_rentPayments_categorizesAsRent() {
        val rentSamples = listOf(
            Pair("SOCIETY APARTMENT RENT & MAINTENANCE", "maintenance@fakebank"),
            Pair("LANDLORD MONTHLY RENT", "landlord@fakebank")
        )

        for ((merchant, upiId) in rentSamples) {
            val result = categorizer.categorize(merchant = merchant, upiId = upiId)
            assertEquals("Expected Rent for ($merchant, $upiId)", Categories.RENT, result)
        }
    }

    // =========================================================================
    // 4. Case-Insensitivity
    // =========================================================================

    @Test
    fun categorize_caseInsensitivity_matchesRegardlessOfCasing() {
        assertEquals(Categories.FOOD, categorizer.categorize(merchant = "swiggy", upiId = "swiggy@fakebank"))
        assertEquals(Categories.FOOD, categorizer.categorize(merchant = "SWIGGY", upiId = "SWIGGY@FAKEBANK"))
        assertEquals(Categories.FOOD, categorizer.categorize(merchant = "SwIgGy", upiId = "SwIgGy@fakebank"))

        assertEquals(Categories.SHOPPING, categorizer.categorize(merchant = "amazon", upiId = "amazon@fakebank"))
        assertEquals(Categories.SHOPPING, categorizer.categorize(merchant = "AMAZON", upiId = "AMAZON@FAKEBANK"))

        assertEquals(Categories.BILLS, categorizer.categorize(merchant = "bescom", upiId = "bescom@fakebank"))
        assertEquals(Categories.BILLS, categorizer.categorize(merchant = "BESCOM", upiId = "BESCOM@FAKEBANK"))
    }

    // =========================================================================
    // 5. PayeeRule User Override (Beats Keyword Dictionary)
    // =========================================================================

    @Test
    fun categorize_payeeRuleOverride_beatsKeywordDictionary() {
        // Swiggy normally defaults to "Food"
        val merchant = "FAKE SWIGGY"
        val upiId = "fakeswiggy@fakebank"

        // User configured a rule overriding this specific payee to "Shopping"
        val rules = listOf(
            PayeeRule(upiId = "fakeswiggy@fakebank", category = Categories.SHOPPING)
        )

        val result = categorizer.categorize(merchant = merchant, upiId = upiId, rules = rules)

        // The user rule must take absolute precedence!
        assertEquals(Categories.SHOPPING, result)
    }

    @Test
    fun categorize_payeeRuleMatching_isCaseInsensitiveAndTrimsWhitespace() {
        val rules = listOf(
            PayeeRule(upiId = "  myprovider@fakebank  ", category = Categories.BILLS)
        )

        val result = categorizer.categorize(
            merchant = "Custom Provider",
            upiId = "MYPROVIDER@fakebank",
            rules = rules
        )

        assertEquals(Categories.BILLS, result)
    }

    // =========================================================================
    // 6. No Match / Default Fallback
    // =========================================================================

    @Test
    fun categorize_unknownMerchantAndUpi_returnsDefaultUncategorized() {
        val result = categorizer.categorize(
            merchant = "RANDOM PERSON 123",
            upiId = "randomuser99@fakebank"
        )

        assertEquals(Categories.UNCATEGORIZED, result)
    }

    @Test
    fun categorize_nullMerchantAndNullVpa_returnsDefaultUncategorized() {
        val resultNulls = categorizer.categorize(merchant = null, upiId = null)
        assertEquals(Categories.UNCATEGORIZED, resultNulls)

        val resultBlank = categorizer.categorize(merchant = "   ", upiId = "   ")
        assertEquals(Categories.UNCATEGORIZED, resultBlank)
    }

    // =========================================================================
    // 7. Credits & Refunds Behavior
    // =========================================================================

    @Test
    fun categorize_creditsAndRefunds_handlesMerchantOrFallbackCorrectly() {
        // Credit refund from a known food merchant
        val refundResult = categorizer.categorize(
            merchant = "ZOMATO REFUND",
            upiId = "zomato@fakebank",
            type = TransactionType.CREDIT
        )
        assertEquals(Categories.FOOD, refundResult)

        // Credit refund from a known grocery merchant
        val groceryRefund = categorizer.categorize(
            merchant = "BLINKIT REFUND",
            upiId = "blinkit@fakebank",
            type = TransactionType.CREDIT
        )
        assertEquals(Categories.GROCERIES, groceryRefund)

        // Credit transfer from an unknown individual
        val creditTransfer = categorizer.categorize(
            merchant = "FRIEND SENDER",
            upiId = "friend@fakebank",
            type = TransactionType.CREDIT
        )
        assertEquals(Categories.UNCATEGORIZED, creditTransfer)

        // Credit with user override
        val creditWithRule = categorizer.categorize(
            merchant = "EMPLOYER PAYROLL",
            upiId = "payroll@fakebank",
            type = TransactionType.CREDIT,
            rules = listOf(PayeeRule(upiId = "payroll@fakebank", category = Categories.BILLS))
        )
        assertEquals(Categories.BILLS, creditWithRule)
    }

    // =========================================================================
    // 8. Strict Category Name Validity Check
    // =========================================================================

    @Test
    fun categorize_allEmittedCategories_areStrictlyValidPerContracts() {
        val allTestInputs = listOf(
            Pair("SWIGGY", "swiggy@fakebank"),
            Pair("AMAZON", "amazon@fakebank"),
            Pair("BESCOM", "bescom@fakebank"),
            Pair("UNKNOWN", "unknown@fakebank"),
            Pair(null, null),
            Pair("NETFLIX", null),
            Pair(null, "groceries@fakebank")
        )

        for ((merchant, upiId) in allTestInputs) {
            val category = categorizer.categorize(merchant = merchant, upiId = upiId)
            assertTrue(
                "Category '$category' must be in contract categories ${Categories.ALL}",
                Categories.isValid(category)
            )
        }
    }

    // =========================================================================
    // 9. Pipeline Convenience Helpers
    // =========================================================================

    @Test
    fun toTransaction_withCategorizer_integratesSeamlessly() {
        val parsed = ParsedTransaction(
            amountPaise = 45000L,
            type = TransactionType.DEBIT,
            merchant = "FAKE SWIGGY",
            upiId = "swiggy@fakebank",
            refNumber = "REF12345",
            date = 1774200000000L,
            bank = "HDFC"
        )

        val rules = listOf(
            PayeeRule(upiId = "swiggy@fakebank", category = Categories.FOOD)
        )

        // Syntax A: parsed.toTransaction(category = categorizer.categorize(parsed, rules))
        val txDirect = parsed.toTransaction(category = categorizer.categorize(parsed, rules))
        assertEquals(Categories.FOOD, txDirect.category)
        assertEquals(45000L, txDirect.amountPaise)
        assertEquals("HDFC", txDirect.bank)

        // Syntax B: parsed.toCategorizedTransaction(categorizer, rules)
        val txHelper = parsed.toCategorizedTransaction(categorizer, rules)
        assertEquals(Categories.FOOD, txHelper.category)
    }
}
