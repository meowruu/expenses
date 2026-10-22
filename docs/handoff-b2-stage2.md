# B2 Stage 2 Handoff & Technical Specification

**Author:** Role B2 — Database, DAOs, Repositories, ViewModels & Data Layer  
**Target Git Branch:** `b2-data`  
**Base Package:** `com.team.expensetracker`  
**Dependencies:** Room 2.7.2, KSP 2.2.10-2.0.2, Coroutines 1.8.0, Lifecycle ViewModel 2.6.1

---

## 1. Overview & Architectural Guarantees

B2 Stage 2 delivers the complete persistent storage layer, reactive repository implementations, and Jetpack Compose ViewModels for our offline, privacy-first Android UPI expense tracker.

### Strict Privacy & Storage Invariants
1. **Zero Raw SMS Storage (Privacy Rule):** Under no circumstance is raw SMS body text or unparsed message content stored, logged, or included in any entity. Only structured financial metadata is retained.
2. **Integer Arithmetic for Currency (`amountPaise: Long`):** Currency is represented strictly in paise (`1 INR = 100 paise`) using `Long`. `Float` and `Double` are strictly prohibited to prevent financial rounding errors.
3. **Database-Enforced Deduplication & Known Limitation:** `TransactionEntity` enforces a unique SQLite index on `refNumber`. Inserting duplicate bank reference / UTR numbers returns `false` and does not insert duplicate rows. Missing/null reference numbers are supported and do not collide.
   - **Known Limitation:** Transactions with a null `refNumber` (such as manual cash entries or SMS notifications without a reference number) cannot be deduplicated; each manual insertion creates a distinct transaction record.
4. **Self-Transfer Exclusion & Case/Whitespace Normalization:** User-owned UPI IDs tracked in `my_upi_ids` are automatically excluded from monthly expenditure and category summary calculations. All UPI IDs are normalized (`trim().lowercase()`) across all repository operations and Room queries (`LOWER(TRIM(upiId))`), ensuring self-transfers are accurately identified regardless of casing or surrounding whitespace.
5. **Credit Exclusion:** Transactions with `type = TransactionType.CREDIT` (refunds, income) are automatically excluded from expense totals and spending summaries.
6. **Null UPI ID Spending Inclusion:** Transactions where `upiId IS NULL` (for instance, manual cash entries) are guaranteed to count as spending. The SQL query explicitly handles null UPI rows: `(upiId IS NULL OR TRIM(upiId) = '' OR LOWER(TRIM(upiId)) NOT IN (SELECT LOWER(TRIM(upiId)) FROM my_upi_ids WHERE upiId IS NOT NULL))`.
7. **Complete Three-Table Wipe Path:** Both `TransactionRepository.deleteAll()` and `SettingsViewModel.deleteAllData()` wipe all three database tables (`transactions`, `payee_rules`, and `my_upi_ids`) in one coordinated operation. Following a wipe, reactive Flow-based summaries immediately reset to `0L` and empty maps.
8. **Cross-Table Flow Reactivity:** Room queries for monthly totals and category summaries join/subquery `my_upi_ids`. Invalidation tracking guarantees that when either transactions or own-UPI IDs change, dashboard streams re-emit updated aggregations automatically.

---

## 2. File & Package Inventory

```
app/src/main/java/com/team/expensetracker/
├── data/
│   ├── model/                                  # Stage 1 Shared Models
│   │   ├── Transaction.kt
│   │   ├── TransactionType.kt
│   │   ├── PayeeRule.kt
│   │   └── MyUpiId.kt
│   ├── local/
│   │   ├── AppDatabase.kt                      # Room Database (v1)
│   │   ├── converter/
│   │   │   └── Converters.kt                   # TypeConverters for TransactionType
│   │   ├── entity/                             # Room Entities
│   │   │   ├── TransactionEntity.kt
│   │   │   ├── PayeeRuleEntity.kt
│   │   │   └── MyUpiIdEntity.kt
│   │   ├── dao/                                # Room DAOs
│   │   │   ├── TransactionDao.kt
│   │   │   ├── PayeeRuleDao.kt
│   │   │   └── MyUpiIdDao.kt
│   │   └── model/
│   │       └── CategorySpending.kt             # Query Projection POJO
│   ├── repository/                             # Repository Interfaces & Impls
│   │   ├── TransactionRepository.kt            # Shared Interface
│   │   ├── PayeeRuleRepository.kt              # Shared Interface
│   │   ├── MyUpiIdRepository.kt                # Shared Interface
│   │   ├── TransactionRepositoryImpl.kt        # Implementation
│   │   ├── PayeeRuleRepositoryImpl.kt          # Implementation
│   │   └── MyUpiIdRepositoryImpl.kt            # Implementation
│   └── di/
│       └── DataLayerContainer.kt               # Dependency Injection Container
└── ui/
    └── viewmodel/                              # ViewModels for F1
        ├── DateUtils.kt                        # Date range epoch helper
        ├── HomeViewModel.kt                    # Dashboard & Summary StateFlows
        ├── TransactionDetailViewModel.kt       # Category editing & Rule learning
        ├── ManualEntryViewModel.kt             # Manual expense submission
        ├── MyUpiIdsViewModel.kt                # Own-UPI ID management
        ├── SettingsViewModel.kt                # Privacy & Data-wipe routine
        ├── ExpenseViewModel.kt                 # Unified All-In-One ViewModel
        └── ExpenseViewModelFactory.kt          # ViewModelProvider.Factory
```

---

## 3. Database & DAO Signatures

### 3.1 Database: `AppDatabase`
- **Class:** `com.team.expensetracker.data.local.AppDatabase`
- **Database File:** `"expense_tracker.db"`
- **Version:** `1` (`exportSchema = false`, destructive migration disabled)
- **Singleton Access:**
  ```kotlin
  AppDatabase.getInstance(context: Context, openHelperFactory: SupportSQLiteOpenHelper.Factory? = null): AppDatabase
  ```

### 3.2 DAOs
- **`TransactionDao`** (`com.team.expensetracker.data.local.dao.TransactionDao`):
  - `suspend fun insert(transaction: TransactionEntity): Long` (Returns rowId or `-1L` on duplicate conflict)
  - `suspend fun existsByRefNumber(refNumber: String): Boolean`
  - `fun observeAll(): Flow<List<TransactionEntity>>`
  - `suspend fun getById(id: Long): TransactionEntity?`
  - `fun observeMonthTotal(monthStart: Long, monthEnd: Long): Flow<Long>`
  - `fun observeCategorySummary(monthStart: Long, monthEnd: Long): Flow<List<CategorySpending>>`
  - `suspend fun updateCategory(id: Long, category: String): Int`
  - `suspend fun deleteAll(): Int`

- **`PayeeRuleDao`** (`com.team.expensetracker.data.local.dao.PayeeRuleDao`):
  - `suspend fun getCategoryFor(upiId: String): String?`
  - `suspend fun saveRule(rule: PayeeRuleEntity): Long`
  - `fun observeAll(): Flow<List<PayeeRuleEntity>>`
  - `suspend fun delete(upiId: String): Int`
  - `suspend fun deleteAll(): Int`

- **`MyUpiIdDao`** (`com.team.expensetracker.data.local.dao.MyUpiIdDao`):
  - `fun observeAll(): Flow<List<MyUpiIdEntity>>`
  - `suspend fun add(myUpiId: MyUpiIdEntity): Long`
  - `suspend fun remove(id: String): Int`
  - `suspend fun isMine(id: String): Boolean`
  - `suspend fun deleteAll(): Int`

---

## 4. Repository Implementation & Initialization

All implementations satisfy the shared Stage 1 interfaces with zero signature modifications:
- `TransactionRepositoryImpl(private val dao: TransactionDao, private val payeeRuleDao: PayeeRuleDao? = null, private val myUpiIdDao: MyUpiIdDao? = null) : TransactionRepository`
- `PayeeRuleRepositoryImpl(private val dao: PayeeRuleDao) : PayeeRuleRepository`
- `MyUpiIdRepositoryImpl(private val dao: MyUpiIdDao) : MyUpiIdRepository`

### Initializing Repositories via Service Locator:
```kotlin
val container = DataLayerContainer.getInstance(context)
val transactionRepo = container.transactionRepository
val payeeRuleRepo = container.payeeRuleRepository
val myUpiIdRepo = container.myUpiIdRepository
```

---

## 5. ViewModels for F1 (Frontend) Integration

F1 can instantiate any ViewModel using the built-in `ExpenseViewModelFactory`:

```kotlin
// In any Composable screen:
val context = LocalContext.current
val homeViewModel: HomeViewModel = viewModel(factory = ExpenseViewModelFactory.create(context))
```

### 5.1 `HomeViewModel` (Dashboard Screen — Task 8)
- `val transactions: StateFlow<List<Transaction>>`: Chronological recent transactions list.
- `val monthTotalPaise: StateFlow<Long>`: Current month's total debit expenditure in paise.
- `val categorySummary: StateFlow<Map<String, Long>>`: Category breakdown (`"Food" to 150000L`).
- `fun setDateRange(monthStart: Long, monthEnd: Long)`: Changes date window.
- `fun resetToCurrentMonth()`: Restores current calendar month window.

### 5.2 `TransactionDetailViewModel` (Detail & Edit Screen — Task 9)
- `val isSaving: StateFlow<Boolean>`
- `val message: StateFlow<String?>`
- `fun updateCategory(id: Long, newCategory: String, upiId: String? = null, saveAsRule: Boolean = false)`: Updates the transaction's category. If `saveAsRule = true`, automatically registers the rule in `PayeeRuleRepository` so future transactions from this payee inherit the category automatically.

### 5.3 `ManualEntryViewModel` (Manual Logging Screen — Task 7)
- `val isSubmitting: StateFlow<Boolean>`
- `val submissionSuccess: StateFlow<Boolean>`
- `val errorMessage: StateFlow<String?>`
- `fun addExpense(amountPaise: Long, note: String, category: String, date: Long = System.currentTimeMillis(), upiId: String? = null, onResult: (Boolean) -> Unit)`

### 5.4 `MyUpiIdsViewModel` (Own-UPI IDs Screen — Task 18)
- `val upiIds: StateFlow<List<MyUpiId>>`: Observable list of user-owned UPI IDs.
- `fun addUpiId(upiId: String)`
- `fun removeUpiId(upiId: String)`

### 5.5 `SettingsViewModel` (Settings & Data-Wipe Screen — Task 27)
- `val isWiping: StateFlow<Boolean>`
- `val wipeCompleted: StateFlow<Boolean>`
- `val errorMessage: StateFlow<String?>`
- `fun deleteAllData(onComplete: () -> Unit)`: Executes complete database wipe.

### 5.6 `ExpenseViewModel` (Unified All-In-One Option)
If F1 prefers a single shared ViewModel across tabs:
```kotlin
val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory.create(context))
```

---

## 6. Integration Guide for Teammates

### 6.1 For B3 (SMS Pipeline & Simulation)
When an SMS is received and parsed by B1 into fields, persist the transaction via `TransactionRepository`:
```kotlin
val container = DataLayerContainer.getInstance(context)
val transactionRepo = container.transactionRepository

val tx = Transaction(
    id = 0L,
    amountPaise = 25000L, // 250 INR in paise (never Float/Double)
    type = TransactionType.DEBIT,
    merchant = parsedMerchant,
    upiId = parsedUpiId,
    refNumber = parsedRefNumber,
    date = timestamp,
    bank = "AXISBK",
    category = categorizedName
)

val isSaved = transactionRepo.insert(tx)
if (!isSaved) {
    // Duplicate refNumber detected; safely drop message
}
```

### 6.2 For B1 (Parser & Categorizer)
Query user-defined rules before applying heuristic dictionary fallbacks:
```kotlin
val container = DataLayerContainer.getInstance(context)
val payeeRuleRepo = container.payeeRuleRepository

val existingCategory = payeeRuleRepo.getCategoryFor(upiId)
val finalCategory = existingCategory ?: heuristicCategorizer.categorize(merchant, upiId)
```

### 6.3 For B4 (Security & Encryption)
B4 configures SQLCipher database encryption via `SupportSQLiteOpenHelper.Factory`:
```kotlin
// In Application or security initialization class:
val passphrase = SecurityKeyManager.getDatabasePassphrase(context)
val openHelperFactory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(passphrase)

// Initialize data layer with encryption factory:
val container = DataLayerContainer.getInstance(
    context = context,
    openHelperFactory = openHelperFactory
)
```
- **Zero Raw SMS Audit:** Audit confirmed. No raw SMS field exists in Room entity schemas or domain models.

---

## 7. Verification & Test Execution Results

All tests were executed against the codebase using the Android Gradle toolchain:

| Command | Status | Result |
| :--- | :--- | :--- |
| `.\gradlew.bat assembleDebug` | **PASS** | `BUILD SUCCESSFUL` |
| `.\gradlew.bat testDebugUnitTest` | **PASS** | `BUILD SUCCESSFUL` (31 tests executed, 31 passed) |

### Test Cases Covered:
1. `testTransactionInsertionAndRetrieval` — Inserts entity, verifies field mapping and persistence.
2. `testDuplicateReferenceNumberRejection` — Verifies `insert` returns `false` on duplicate `refNumber`.
3. `testMissingOrNullReferenceNumbersCanBeInsertedMultipleTimes` — Multiple null/blank reference numbers do not collide.
4. `testTransactionCategoryUpdate` — Modifies category and verifies persisted state.
5. `testDeleteAllWipesData` — Clears transactions completely for data wipe.
6. `testMonthlyTotalsAndDateWindow` — Verifies time range filtering (inclusive boundaries).
7. `testExcludingCreditTransactionsFromSpending` — Guarantees credits/refunds are excluded from spending totals and category summaries.
8. `testExcludingSelfTransfers` — Confirms transactions to own-UPI IDs are excluded from spending totals.
9. `testReactiveUpdatesWhenOwnUpiIdIsAddedLater` — Adding a user-owned UPI ID reactively updates and reduces active dashboard totals.
10. `testEmptyDatabaseBehavior` — Verifies 0L totals, empty map, and empty list on fresh database.
11. `testCorrectLongPaiseIntegerArithmetic` — Verifies multi-billion paise arithmetic with zero rounding error.
12. `testManualEntryWithNullUpiIdCountsInMonthTotalAndCategorySummary` — Verifies manual entries with `upiId = null` count in month totals and category summaries.
13. `testSelfTransferIsExcluded` — Confirms self-transfer debit transactions are excluded from totals and category summaries.
14. `testUpiIdCaseAndWhitespaceDifferencesTreatedAsSame` — Verifies case-insensitive and trimmed UPI matching for own IDs and transfers.
15. `testDeleteAllDataEmptiesAllThreeTablesAndSummariesResetToZero` — Verifies repository `deleteAll()` empties all 3 tables (`transactions`, `payee_rules`, `my_upi_ids`) and resets Flow summaries to 0.
16. `SettingsViewModelTest` — Verifies `SettingsViewModel.deleteAllData()` clears all 3 tables and triggers callback.
17. `PayeeRuleRepositoryTest` — Verifies lookup, saving, updating, and whitespace normalization.
18. `MyUpiIdRepositoryTest` — Verifies adding, idempotency, checking `isMine`, and removal.
19. `HomeViewModelTest` — Verifies initial dashboard state and reactive `StateFlow` updates upon data insertion.
20. `DateUtilsTest` — Verifies start of month (00:00:00.000) and end of month (23:59:59.999) calendar computations.
