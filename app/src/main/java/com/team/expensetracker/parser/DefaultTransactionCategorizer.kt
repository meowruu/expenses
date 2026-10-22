package com.team.expensetracker.parser

import com.team.expensetracker.data.model.PayeeRule
import com.team.expensetracker.data.model.TransactionType

/**
 * Production implementation of [TransactionCategorizer] using keyword heuristics and PayeeRule overrides.
 *
 * Evaluation Rules:
 * 1. User PayeeRule overrides are evaluated FIRST. A matching [PayeeRule.upiId] takes absolute precedence.
 * 2. If no rule matches, evaluates curated keyword dictionaries over merchant name and UPI VPA.
 *    Categories are evaluated in priority order:
 *    Food → Groceries → Transport → Fuel → Shopping → Bills → Entertainment → Health → Education → Rent → Transfers
 * 3. Falls back strictly to [Categories.UNCATEGORIZED] when no heuristics match.
 * 4. Ensures all returned category strings are valid per [Categories].
 */
class DefaultTransactionCategorizer : TransactionCategorizer {

    override fun categorize(
        transaction: ParsedTransaction,
        rules: List<PayeeRule>
    ): String {
        return categorize(
            merchant = transaction.merchant,
            upiId = transaction.upiId,
            type = transaction.type,
            rules = rules
        )
    }

    override fun categorize(
        merchant: String?,
        upiId: String?,
        type: TransactionType,
        rules: List<PayeeRule>
    ): String {
        // Step 1: User PayeeRule Overrides (Highest Priority)
        val ruleCategory = matchPayeeRule(upiId, rules)
        if (ruleCategory != null) {
            return ruleCategory
        }

        // If both merchant and UPI ID are missing, default immediately
        if (merchant.isNullOrBlank() && upiId.isNullOrBlank()) {
            return Categories.UNCATEGORIZED
        }

        val combinedTarget = buildTargetText(merchant, upiId)

        // Step 2: Keyword Dictionary Matching (evaluated in priority order)
        if (matchesFood(combinedTarget)) return Categories.FOOD
        if (matchesGroceries(combinedTarget)) return Categories.GROCERIES
        if (matchesTransport(combinedTarget)) return Categories.TRANSPORT
        if (matchesFuel(combinedTarget)) return Categories.FUEL
        if (matchesShopping(combinedTarget)) return Categories.SHOPPING
        if (matchesBills(combinedTarget)) return Categories.BILLS
        if (matchesEntertainment(combinedTarget)) return Categories.ENTERTAINMENT
        if (matchesHealth(combinedTarget)) return Categories.HEALTH
        if (matchesEducation(combinedTarget)) return Categories.EDUCATION
        if (matchesRent(combinedTarget)) return Categories.RENT
        if (matchesTransfers(combinedTarget)) return Categories.TRANSFERS

        // Step 3: Default Fallback
        return Categories.UNCATEGORIZED
    }

    /**
     * Matches the incoming transaction's UPI ID against user-configured [PayeeRule] records.
     */
    private fun matchPayeeRule(upiId: String?, rules: List<PayeeRule>): String? {
        if (upiId.isNullOrBlank() || rules.isEmpty()) {
            return null
        }
        val cleanUpi = upiId.trim()
        val matched = rules.firstOrNull { it.upiId.trim().equals(cleanUpi, ignoreCase = true) }
        return matched?.category?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun buildTargetText(merchant: String?, upiId: String?): String {
        val upiHandle = upiId?.substringBefore("@").orEmpty()
        return "${merchant.orEmpty()} ${upiId.orEmpty()} $upiHandle".lowercase()
    }

    private fun matchesFood(text: String): Boolean =
        FOOD_BRAND_KEYWORDS.any { text.contains(it) } ||
                FOOD_NOUN_PATTERNS.any { it.containsMatchIn(text) }

    private fun matchesGroceries(text: String): Boolean =
        GROCERIES_BRAND_KEYWORDS.any { text.contains(it) } ||
                GROCERIES_NOUN_PATTERNS.any { it.containsMatchIn(text) }

    private fun matchesTransport(text: String): Boolean =
        TRANSPORT_BRAND_KEYWORDS.any { text.contains(it) } ||
                TRANSPORT_NOUN_PATTERNS.any { it.containsMatchIn(text) }

    private fun matchesFuel(text: String): Boolean =
        FUEL_BRAND_KEYWORDS.any { text.contains(it) } ||
                FUEL_NOUN_PATTERNS.any { it.containsMatchIn(text) }

    private fun matchesShopping(text: String): Boolean =
        SHOPPING_BRAND_KEYWORDS.any { text.contains(it) } ||
                SHOPPING_NOUN_PATTERNS.any { it.containsMatchIn(text) }

    private fun matchesBills(text: String): Boolean =
        BILLS_BRAND_KEYWORDS.any { text.contains(it) } ||
                BILLS_NOUN_PATTERNS.any { it.containsMatchIn(text) }

    private fun matchesEntertainment(text: String): Boolean =
        ENTERTAINMENT_BRAND_KEYWORDS.any { text.contains(it) } ||
                ENTERTAINMENT_NOUN_PATTERNS.any { it.containsMatchIn(text) }

    private fun matchesHealth(text: String): Boolean =
        HEALTH_BRAND_KEYWORDS.any { text.contains(it) } ||
                HEALTH_NOUN_PATTERNS.any { it.containsMatchIn(text) }

    private fun matchesEducation(text: String): Boolean =
        EDUCATION_BRAND_KEYWORDS.any { text.contains(it) } ||
                EDUCATION_NOUN_PATTERNS.any { it.containsMatchIn(text) }

    private fun matchesRent(text: String): Boolean =
        RENT_NOUN_PATTERNS.any { it.containsMatchIn(text) }

    private fun matchesTransfers(text: String): Boolean =
        TRANSFERS_NOUN_PATTERNS.any { it.containsMatchIn(text) }

    companion object {

        // -------------------------------------------------------------------------
        // FOOD — delivery, restaurants, fast food, cafes, fine dining
        // -------------------------------------------------------------------------
        private val FOOD_BRAND_KEYWORDS = listOf(
            "swiggy", "zomato", "eatsure", "behrouz", "faasos", "ovenstory", "box8",
            "magicpin", "freshmenu",
            "mcdonald", "domino", "pizza hut", "pizzahut", "kfc", "burger king", "burgerking",
            "subway", "taco bell", "tacobell", "starbucks", "chaayos", "chai point", "chaipoint",
            "barista", "third wave", "blue tokai", "haldiram", "bikanervala", "barbeque nation",
            "bbq nation", "mainland china"
        )

        private val FOOD_NOUN_PATTERNS = listOf(
            Regex("""(?i)\b(?:restaurant|restaurants|dhaba|dining|bhojnalaya|kitchen|canteen|tiffin|mess|biryani|pizza|burger)\b"""),
            Regex("""(?i)\b(?:cafe|coffee|bakery|bakes|cakes|pastry|sweet|sweets|mithai|tea|food)\b""")
        )

        // -------------------------------------------------------------------------
        // GROCERIES — supermarkets, instant grocery, hypermarkets, dairy, vegetables
        // -------------------------------------------------------------------------
        private val GROCERIES_BRAND_KEYWORDS = listOf(
            "blinkit", "zepto", "instamart", "bigbasket", "bbdaily", "dmart",
            "spencers", "nature's basket", "natures basket", "more retail",
            "mother dairy", "country delight", "fresho"
        )

        private val GROCERIES_NOUN_PATTERNS = listOf(
            Regex("""(?i)\b(?:supermarket|hypermarket|kirana|provision|vegetable|vegetables|fruits|milk|dairy|grocer|grocery|groceries)\b""")
        )

        // -------------------------------------------------------------------------
        // TRANSPORT — cabs, metro, railways, buses, flights, tolls, parking
        // -------------------------------------------------------------------------
        private val TRANSPORT_BRAND_KEYWORDS = listOf(
            "uber", "ola", "rapido", "namma metro", "dmrc", "irctc",
            "makemytrip", "goibibo", "yatra", "redbus", "abhibus"
        )

        private val TRANSPORT_NOUN_PATTERNS = listOf(
            Regex("""(?i)\b(?:metro|railway|train|cab|cabs|taxi|auto|bus|flight|airline|airport|toll|fastag|parking)\b""")
        )

        // -------------------------------------------------------------------------
        // FUEL — petrol stations, CNG pumps, diesel
        // -------------------------------------------------------------------------
        private val FUEL_BRAND_KEYWORDS = listOf(
            "hpcl", "bpcl", "iocl", "indian oil", "indianoil", "shell"
        )

        private val FUEL_NOUN_PATTERNS = listOf(
            Regex("""(?i)\b(?:petrol|diesel|fuel|cng|pump|fuels?)\b""")
        )

        // -------------------------------------------------------------------------
        // SHOPPING — e-commerce, apparel, electronics, footwear, jewellery, retail
        // -------------------------------------------------------------------------
        private val SHOPPING_BRAND_KEYWORDS = listOf(
            "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "tata cliq", "tataneu",
            "snapdeal", "shopsy", "croma", "vijay sales", "reliance digital", "apple store",
            "samsung", "oneplus", "boat", "noise", "lenovo", "zara", "h&m", "uniqlo",
            "marks & spencer", "max fashion", "westside", "pantaloons", "shoppers stop", "shoppersstop",
            "lifestyle", "trends", "reliance trends", "fabindia", "decathlon", "puma", "nike",
            "adidas", "bata", "woodland", "lenskart", "titan", "fastrack", "tanishq", "kalyan jewellers",
            "malabar gold"
        )

        private val SHOPPING_NOUN_PATTERNS = listOf(
            Regex("""(?i)\b(?:retail|clothing|apparel|footwear|garments|boutique|jeweller|jewellers|jewellery|opticals|mall|shopping|electronics)\b"""),
            Regex("""(?i)\b(?:store|outfitters|fashions?)\b""")
        )

        // -------------------------------------------------------------------------
        // BILLS — utilities, electricity, water, gas, telecom, broadband, DTH
        // -------------------------------------------------------------------------
        private val BILLS_BRAND_KEYWORDS = listOf(
            "bescom", "tata power", "tatapower", "adani electricity", "mseb", "bses", "uppcl", "cesc",
            "indraprastha gas", "igl", "mahanagar gas", "mgl", "indane", "bharat gas", "bharatgas",
            "hp gas", "hpgas",
            "airtel", "jio", "vodafone", "idea", "bsnl", "act fibernet", "actcorp", "hathway",
            "tata play", "tataplay", "dish tv", "dishtv", "sun direct", "billdesk", "bbps"
        )

        private val BILLS_NOUN_PATTERNS = listOf(
            Regex("""(?i)\b(?:electricity|vidyut|power|water supply|jal board|gas bill|cylinder|lpg)\b"""),
            Regex("""(?i)\b(?:recharge|broadband|postpaid|landline|telecom|dth|utility|bills?|bill pay)\b""")
        )

        // -------------------------------------------------------------------------
        // ENTERTAINMENT — streaming, cinema, music, events, gaming
        // -------------------------------------------------------------------------
        private val ENTERTAINMENT_BRAND_KEYWORDS = listOf(
            "netflix", "spotify", "hotstar", "disney", "prime video", "primevideo",
            "pvr", "inox", "cinepolis", "bookmyshow", "bms",
            "gaana", "wynk", "youtube premium", "google play"
        )

        private val ENTERTAINMENT_NOUN_PATTERNS = listOf(
            Regex("""(?i)\b(?:subscription|cinema|movies?|theatre|streaming|concert|event|gaming)\b""")
        )

        // -------------------------------------------------------------------------
        // HEALTH — pharmacies, hospitals, clinics, diagnostics, doctors
        // -------------------------------------------------------------------------
        private val HEALTH_BRAND_KEYWORDS = listOf(
            "apollo pharmacy", "apollopharmacy", "netmeds", "pharmeasy", "1mg", "tata 1mg",
            "medplus", "wellness forever", "dr lal", "metropolis", "practo"
        )

        private val HEALTH_NOUN_PATTERNS = listOf(
            Regex("""(?i)\b(?:hospital|clinic|diagnostic|pathology|pharmacy|medical|chemist|healthcare|dental|doctor)\b""")
        )

        // -------------------------------------------------------------------------
        // EDUCATION — schools, colleges, tuition, coaching, online courses
        // -------------------------------------------------------------------------
        private val EDUCATION_BRAND_KEYWORDS = listOf(
            "allen", "aakash", "fiitjee", "byjus", "unacademy", "coursera", "udemy"
        )

        private val EDUCATION_NOUN_PATTERNS = listOf(
            Regex("""(?i)\b(?:school|college|university|vidyalaya|tuition|coaching|fees?)\b""")
        )

        // -------------------------------------------------------------------------
        // RENT — housing, maintenance charges, landlord payments
        // -------------------------------------------------------------------------
        private val RENT_NOUN_PATTERNS = listOf(
            Regex("""(?i)\b(?:rent|landlord|society maintenance|apartment maintenance|housing maintenance|maintenance)\b""")
        )

        // -------------------------------------------------------------------------
        // TRANSFERS — P2P bank transfers, NEFT/IMPS, self-transfers
        // -------------------------------------------------------------------------
        private val TRANSFERS_NOUN_PATTERNS = listOf(
            Regex("""(?i)\b(?:neft|imps|rtgs|transfer|self transfer|peer|p2p|send money)\b""")
        )
    }
}
