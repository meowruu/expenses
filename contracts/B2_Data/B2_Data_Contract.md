# TEAM ROLE CONTRACT: B2: DATA LAYER

| Attribute | Value | Attribute | Value |
| :--- | :--- | :--- | :--- |
| **Project** | TBD (Privacy-First Android UPI Expense Tracker) | **Git Branch** | `b2-data` |
| **Role Assignment** | B2: Data Layer | **Date / Sprint** | TBD (24-Hour Hackathon) |

## 1. Title and Metadata
- **Document Title:** Team Role Contract – B2: Data Layer
- **Project Name:** TBD (Privacy-First Android Expense Tracker that auto-logs UPI bank SMS on-device)
- **Role:** B2: Data Layer
- **Git Branch:** `b2-data`
- **Date:** TBD (24-Hour Hackathon)

## 2. Purpose of the Role
The purpose of the B2 role is to architect and implement the local data layer, persistent storage, and reactive state management for the application. B2 is responsible for establishing the core Transaction data model, Room database tables, deduplication logic, and ViewModels. B2 delivers clean, encrypted local data storage and seamless data observation for the UI layer.

## 3. Core Deliverables
| Task # | Task Description | Detailed Scope / Requirements | Done-When Criterion |
| :--- | :--- | :--- | :--- |
| **Task 6** | Transaction Model & Repo Interfaces (First Priority) | First priority: push the Transaction data class and the Repository interfaces so everyone else can start building (6). Transaction data class: amount, debit/credit, merchant, UPI ID, ref number, date, bank, category. | Transaction data class and Repository interfaces are pushed to git as the first team dependency. |
| **Task 15** | Room Local Database | Room database with tables for transactions and payee rules. | Room database compiles, runs locally, and handles persistence for transactions and payee rules. |
| **Task 16** | Repository & ViewModels | Repository and ViewModels exposing reactive state (monthly total, category summary, recent list). | ViewModels expose observable state flows that accurately reflect database contents and updates. |
| **Task 17** | Reference Number Deduplication | Dedupe logic so the same ref number is never saved twice. | Inserting duplicate ref numbers is blocked and ignored; no duplicate transactions exist in database. |
| **Task 18** | "My UPI IDs" Self-Transfer Exclusion | "My UPI IDs" logic so transfers between the user's own accounts are not counted as spending. | Transactions between user-owned UPI IDs are recognized and excluded from spending totals. |
| **Integration** | Data Layer Integration & Privacy Support | Hand F1 the real ViewModels to replace fake data; make sure parser output saves and the monthly total and category summary update; provide the data-wipe function for B4's "delete all data" button, and work with B4 so SQLCipher encryption wraps the database cleanly. | F1 binds to real ViewModels; parser output saves to DB and updates summaries; data-wipe clears DB; SQLCipher encrypts DB. |

## 4. Shared Obligations
- **Setup (Tasks 1-3): **Install Android Studio, emulator and Antigravity (1-3); review SPEC.md and RULES.md.
- **SMS Samples (Task 5): **Collect 4-5 real bank SMS samples with account digits and names masked, maintaining them in a samples/ folder.
- **Demo Flow (Task 32): **Rehearse the demo flow: open the app, unlock, SMS arrives, entry created and categorized, summary updates.
- **Backup Video (Task 34): **Rehearse demo flow and assist F1 and B3 with recording the backup video.

## 5. Dependencies
| Direction | What is Needed / Supplied | With Whom | When / Milestone | Purpose & Impact |
| :--- | :--- | :--- | :--- | :--- |
| **Inbound (Needs)** | SPEC.md & RULES.md (Task 4) | B4 | Kickoff (Hour 0-1) | Required to implement data constraints: no raw SMS saved and encrypted storage. |
| **Inbound (Needs)** | SQLCipher encryption setup (Task 25) | B4 | Database Build (Hour 4-8) | Required to wrap Room database in SQLCipher encryption cleanly. |
| **Inbound (Needs)** | Parser output entity format (Task 10) | B1 | Core Build (Hour 6-8) | Required to ensure parser entities map cleanly into Room DAOs and repository. |
| **Inbound (Needs)** | UI state observation needs | F1 | Core Build (Hour 4-6) | Required to shape ViewModel exposed StateFlows to match screen dashboard layouts. |
| **Outbound (Supplies)** | Transaction class & Repo interfaces (Task 6) | F1, B1 | First Priority (Hour 1-2) | First priority deliverable: unblocks F1 (UI mockups) and B1 (parser mapping). |
| **Outbound (Supplies)** | Real ViewModels (Task 16) | F1 | Integration Phase (Hour 6-10) | Replaces mock UI data with real reactive database feeds. |
| **Outbound (Supplies)** | Payee rules Room table (Task 15) | B1 | Core Build (Hour 6-8) | Enables B1 categorizer to store and retrieve user-modified category preferences. |
| **Outbound (Supplies)** | Data-wipe function | B4 | Security Integration (Hour 8-12) | Supplies clean database purge routine for B4's "delete all data" button (Task 27). |

## 6. Cross-Review Duties
In strict adherence to RULES.md, nobody reviews their own security code. B2 collaborates directly with B4 to review database storage and schema to ensure zero raw SMS is persisted (Task 26) and SQLCipher encryption wraps database cleanly (Task 25). Designated peer reviewer for B2's repository and ViewModels is TBD (assigned to team peer review).

## 7. Stretch Items
No stretch tasks are directly assigned to B2. Shared team stretch items include Task 22 (Notification listener for GPay/PhonePe - B3), Task 30 (CSV export - F1), and Task 31 (More banks/merchants - B1) [All stretch items: only if time is left; otherwise roadmap slide].

## 8. Non-Negotiable Rules from RULES.md
- **No INTERNET permission: **App must function 100% on-device; zero network permissions in AndroidManifest.xml.
- **No raw SMS saved: **Store only necessary fields (amount, debit/credit, merchant, UPI ID, ref number, date, bank, category); never save raw SMS text.
- **OTP rejected first: **OTP messages must be identified and rejected first, before any other parsing occurs.
- **allowBackup="false": **android:allowBackup="false" must be strictly configured in manifest to prevent ADB extraction.

## 9. Working Agreement
- **Regular Short Syncs: **Participate in short, frequent standups across the 24-hour sprint to catch blockers early.
- **Small Frequent Commits: **Make small, frequent commits to git with clear messages to avoid merge conflicts.
- **Dedicated Personal Branch: **Work exclusively on git branch b2-data and integrate via tested pull requests.
- **Escalate Blockers Early: **Escalate any technical hurdle or cross-role blocker immediately to the team and lead.

## 10. Acceptance Checklist and Signatures
### Acceptance Checklist
- [ ] Android Studio, emulator, and Antigravity installed; SPEC.md and RULES.md reviewed.
- [ ] Git branch b2-data established with frequent, documented commits.
- [ ] FIRST PRIORITY: Transaction data class and Repository interfaces pushed to git.
- [ ] Room database (Task 15) created with tables for transactions and payee rules.
- [ ] Repository and ViewModels (Task 16) expose reactive monthly total, category summary, and recent list.
- [ ] Deduplication logic (Task 17) prevents saving the same ref number twice.
- [ ] "My UPI IDs" logic (Task 18) excludes self-transfers from spending totals.
- [ ] Real ViewModels handed to F1, replacing fake data cleanly.
- [ ] Parser output saves verified and reactive summary updates confirmed.
- [ ] Data-wipe function provided for B4's "delete all data" button.
- [ ] Database wrapped cleanly with SQLCipher encryption in collaboration with B4.
- [ ] Demo flow (Task 32) rehearsed and backup video (Task 34) supported.
- [ ] Compliance verified: no INTERNET, no raw SMS saved, OTP rejected first, allowBackup="false".

### Signature Block

| Team Member Sign-off (B2: Data Layer) | Team Lead Sign-off (Project Lead / PM) |
| :--- | :--- |
| **Name:** TBD | **Name:** TBD |
| **Signature:** _________________________________________ | **Signature:** _________________________________________ |
| **Date:** TBD | **Date:** TBD |