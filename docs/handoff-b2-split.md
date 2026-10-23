# B2 Split Expense Feature Handoff

**Author:** Role B2 — Data Layer (Database, Repositories, ViewModels)  
**Target Branch:** `b2-split`  
**Package:** `com.team.expensetracker`  
**Database Version:** `2` (Upgraded from `1` with non-destructive `MIGRATION_1_2`)

---

## 1. Overview & Architectural Guarantees

The Split Expense feature is a **100% offline, local-only** component:
1. **Zero External APIs / Permissions:** No Splitwise API, no network calls, and zero `android.permission.INTERNET` usage.
2. **Integer Arithmetic Invariant (`sharePaise: Long`):** All shares and totals are strictly stored and computed in paise (`Long`). Float and Double are completely avoided.
3. **Remainder Distribution Invariant:** When an expense cannot be divided evenly among participants (e.g., splitting 100.00 INR / 10000 paise among 3 people):
   - Person 0 gets: 3,334 paise
   - Person 1 gets: 3,333 paise
   - Person 2 gets: 3,333 paise
   - **Sum of shares:** Exactly 10,000 paise (0 rounding error).
4. **Non-Destructive Room Migration (v1 -> v2):** `MIGRATION_1_2` safely runs `CREATE TABLE IF NOT EXISTS split_shares` and creates necessary indices without dropping or altering existing `transactions`, `payee_rules`, or `my_upi_ids`.
5. **Cascade & Wipe Integration:**
   - `split_shares.transactionId` has a foreign key referencing `transactions.id` with `onDelete = CASCADE`.
   - `SettingsViewModel.deleteAllData()` and `TransactionRepository.deleteAll()` clear `split_shares` alongside all other tables.

---

## 2. Database Schema (Room v2)

```sql
CREATE TABLE IF NOT EXISTS `split_shares` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `transactionId` INTEGER NOT NULL,
    `personName` TEXT NOT NULL,
    `sharePaise` INTEGER NOT NULL,
    `settled` INTEGER NOT NULL,
    FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS `index_split_shares_transactionId` ON `split_shares` (`transactionId`);
CREATE INDEX IF NOT EXISTS `index_split_shares_personName` ON `split_shares` (`personName`);
CREATE INDEX IF NOT EXISTS `index_split_shares_settled` ON `split_shares` (`settled`);
```

---

## 3. Data & Repository Signatures

### Domain Model: `SplitShare`
```kotlin
package com.team.expensetracker.data.model

data class SplitShare(
    val id: Long = 0L,
    val transactionId: Long,
    val personName: String,
    val sharePaise: Long,
    val settled: Boolean = false
)
```

### Interface: `SplitRepository`
```kotlin
package com.team.expensetracker.data.repository

interface SplitRepository {
    suspend fun splitEqually(transactionId: Long, totalPaise: Long, people: List<String>): List<SplitShare>
    fun observeSharesForTransaction(transactionId: Long): Flow<List<SplitShare>>
    fun observeOwedPerPerson(): Flow<Map<String, Long>>
    suspend fun markSettled(shareId: Long, settled: Boolean = true)
    suspend fun deleteSharesForTransaction(transactionId: Long)
    suspend fun deleteAll()
}
```

---

## 4. UI Integration Guide for F1 (Frontend)

### 4.1 How to Obtain `SplitViewModel`
In any Composable or Screen:
```kotlin
val context = LocalContext.current
val splitViewModel: SplitViewModel = viewModel(factory = ExpenseViewModelFactory.create(context))
```

### 4.2 Available StateFlows & Methods in `SplitViewModel`

- **`val owedPerPerson: StateFlow<Map<String, Long>>`**  
  Emits a map of person name to total unsettled paise owed by that individual (e.g. `{"Alice" to 3334L, "Bob" to 6500L}`). Automatically updates whenever a split is added or marked settled.

- **`val currentTransactionShares: StateFlow<List<SplitShare>>`**  
  Emits the list of shares for the currently inspected transaction.

- **`fun loadSharesForTransaction(transactionId: Long)`**  
  Call this when entering a Transaction Detail screen to observe its existing split shares.

- **`fun splitEqually(transactionId: Long, totalPaise: Long, people: List<String>, onComplete: (List<SplitShare>) -> Unit = {})`**  
  Divides `totalPaise` equally among the provided `people`, correctly distributes remainder paise, and saves the shares.

- **`fun markSettled(shareId: Long, settled: Boolean = true, onComplete: () -> Unit = {})`**  
  Marks a specific share as settled (or unsettled).

---

### 4.3 Copy-Paste Examples for F1

#### Example A: Splitting an Expense from Transaction Detail Screen
```kotlin
val peopleList = listOf("Alice", "Bob", "Charlie")

// Call on Split button click
splitViewModel.splitEqually(
    transactionId = transaction.id,
    totalPaise = transaction.amountPaise,
    people = peopleList
) { shares ->
    // Done! shares contains the generated SplitShare list
}
```

#### Example B: Marking a Share as Paid / Settled
```kotlin
// In the list of shares for a transaction:
IconButton(onClick = { splitViewModel.markSettled(share.id, settled = !share.settled) }) {
    Icon(
        imageVector = if (share.settled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
        contentDescription = if (share.settled) "Settled" else "Mark Settled"
    )
}
```

#### Example C: Showing "Who Owes Me" Dashboard
```kotlin
val owedMap by splitViewModel.owedPerPerson.collectAsState()

LazyColumn {
    items(owedMap.toList()) { (person, amountPaise) ->
        val formattedAmount = String.format("₹%.2f", amountPaise / 100.0)
        Text("$person owes: $formattedAmount")
    }
}
```
