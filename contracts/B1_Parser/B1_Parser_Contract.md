# TEAM ROLE CONTRACT: B1: PARSER AND BRAIN

| Attribute | Value | Attribute | Value |
| :--- | :--- | :--- | :--- |
| **Project** | TBD (Privacy-First Android UPI Expense Tracker) | **Git Branch** | `b1-parser` |
| **Role Assignment** | B1: Parser and Brain | **Date / Sprint** | TBD (24-Hour Hackathon) |

## 1. Title and Metadata
- **Document Title:** Team Role Contract – B1: Parser and Brain
- **Project Name:** TBD (Privacy-First Android Expense Tracker that auto-logs UPI bank SMS on-device)
- **Role:** B1: Parser and Brain
- **Git Branch:** `b1-parser`
- **Date:** TBD (24-Hour Hackathon)

## 2. Purpose of the Role
The purpose of the B1 role is to architect, build, and rigorously validate the on-device SMS parsing engine and intelligent categorizer. B1 ensures bank SMS messages are accurately dissected into core financial data fields while immediately identifying and rejecting OTPs before any parsing occurs. B1 guarantees that parsed data flows correctly to the local repository, resolves parsing defects found during end-to-end integration, and thoroughly tests all transactional edge cases.

## 3. Core Deliverables
| Task # | Task Description | Detailed Scope / Requirements | Done-When Criterion |
| :--- | :--- | :--- | :--- |
| **Task 5** | SMS Sample Collection & Maintenance | Keep the 4-5 masked real bank SMS samples in a samples/ folder covering valid debit/credit, failed transactions, and refunds. | 4-5 masked real bank SMS samples are stored and maintained in samples/ folder. |
| **Task 10** | SMS Parsing Engine | SMS parser that extracts amount, debit/credit, UPI ID and ref number. | Parser accurately extracts amount, debit/credit flag, UPI ID, and ref number from bank SMS. |
| **Task 12** | Pre-Parsing OTP Rejection Check | OTP reject check. OTP messages are rejected first, before any other parsing. | 100% of OTP messages are identified and rejected prior to transaction parsing. |
| **Task 11** | Parser Unit Test Suite | Unit tests for the parser: valid SMS, OTP, failed transaction, refund. | Automated unit tests pass across valid SMS, OTP, failed transaction, and refund test cases. |
| **Task 13** | Expense Categorizer Engine | Categorizer: keyword dictionary, guess merchant vs person from the UPI ID, and remember categories the user changed (uses B2's payee rules table). | Categorizer assigns categories, guesses merchant vs person from UPI ID, and recalls user overrides via B2's table. |
| **Integration** | Pipeline Integration & Edge Case Testing | Connect the parser to B3's receiver and Simulate SMS button, with output saved through B2's repository; fix any parsing bugs found in the end-to-end test; re-run the parser against every sample and edge case (failed transaction, refund, OTP). | Parser integrates with B3 receiver/simulation and B2 repository; all edge cases pass end-to-end test without bugs. |

## 4. Shared Obligations
- **Setup (Tasks 1-3): **Install Android Studio, emulator and Antigravity (1-3); review SPEC.md and RULES.md.
- **SMS Samples (Task 5): **Collect 4-5 real bank SMS samples with account digits and names masked, maintaining them in a samples/ folder.
- **Demo Flow (Task 32): **Rehearse the demo flow: open the app, unlock, SMS arrives, entry created and categorized, summary updates.
- **Backup Video (Task 34): **Rehearse demo flow and assist F1 and B3 with recording the backup video.

## 5. Dependencies
| Direction | What is Needed / Supplied | With Whom | When / Milestone | Purpose & Impact |
| :--- | :--- | :--- | :--- | :--- |
| **Inbound (Needs)** | SPEC.md & RULES.md (Task 4) | B4 | Kickoff (Hour 0-1) | Required to align parsing rules with privacy constraints and OTP rejection mandate. |
| **Inbound (Needs)** | Transaction class & Repo interfaces (Task 6) | B2 | First Priority (Hour 1-2) | Required to map parsed SMS outputs to structured entity data fields. |
| **Inbound (Needs)** | Payee rules Room table (Task 15) | B2 | Integration Phase (Hour 8-12) | Required for categorizer to store and retrieve user-modified category rules. |
| **Inbound (Needs)** | SMS Receiver (Task 19) & Simulate SMS (Task 14) | B3 | Integration Phase (Hour 10-14) | Required to connect parsing engine to incoming and simulated SMS streams. |
| **Outbound (Supplies)** | SMS Parser (Task 10) & OTP Filter (Task 12) | B3, B2 | Core Build (Hour 6-10) | Powers B3's incoming pipeline and feeds structured data to B2's repository. |
| **Outbound (Supplies)** | Categorizer & Payee Rules Integration (Task 13) | B2, F1 | Integration Phase (Hour 10-14) | Enables automatic expense categorization and reactive dashboard updates in UI. |

## 6. Cross-Review Duties
In strict adherence to RULES.md, nobody reviews their own security code. B1's cross-review duties include verifying integration endpoints with B2 (payee rules table and repository save) and B3 (SMS receiver and Simulate SMS pipeline). Designated peer reviewer for B1's parser unit tests and regex algorithms is TBD (assigned to team peer review).

## 7. Stretch Items
Task 31: More banks, more SMS formats, bigger merchant dictionary (Assigned to: B1) [Only if time is left; otherwise roadmap slide]. Note: Shared team stretch items include Task 22 (Notification listener for GPay/PhonePe - B3) and Task 30 (CSV export - F1).

## 8. Non-Negotiable Rules from RULES.md
- **No INTERNET permission: **App must function 100% on-device; zero network permissions in AndroidManifest.xml.
- **No raw SMS saved: **Store only necessary fields (amount, debit/credit, merchant, UPI ID, ref number, date, bank, category); never save raw SMS text.
- **OTP rejected first: **OTP messages must be identified and rejected first, before any other parsing occurs.
- **allowBackup="false": **android:allowBackup="false" must be strictly configured in manifest to prevent ADB extraction.

## 9. Working Agreement
- **Regular Short Syncs: **Participate in short, frequent standups across the 24-hour sprint to catch blockers early.
- **Small Frequent Commits: **Make small, frequent commits to git with clear messages to avoid merge conflicts.
- **Dedicated Personal Branch: **Work exclusively on git branch b1-parser and integrate via tested pull requests.
- **Escalate Blockers Early: **Escalate any technical hurdle or cross-role blocker immediately to the team and lead.

## 10. Acceptance Checklist and Signatures
### Acceptance Checklist
- [ ] Android Studio, emulator, and Antigravity installed; SPEC.md and RULES.md reviewed.
- [ ] Git branch b1-parser established with frequent, documented commits.
- [ ] 4-5 masked real bank SMS samples collected and placed in samples/ folder.
- [ ] SMS parser (Task 10) extracts amount, debit/credit, UPI ID, and ref number.
- [ ] OTP rejection check (Task 12) tested and confirmed to reject OTPs first.
- [ ] Unit tests (Task 11) pass for valid SMS, OTP, failed transactions, and refunds.
- [ ] Categorizer (Task 13) implemented with keyword dictionary, UPI heuristics, and payee rules.
- [ ] Parser integrated with B3 receiver, Simulate SMS button, and B2 repository.
- [ ] All edge cases re-tested and zero parsing bugs remaining in end-to-end flow.
- [ ] Demo flow (Task 32) rehearsed and backup video (Task 34) supported.
- [ ] Compliance verified: no INTERNET, no raw SMS saved, OTP rejected first, allowBackup="false".

### Signature Block

| Team Member Sign-off (B1: Parser and Brain) | Team Lead Sign-off (Project Lead / PM) |
| :--- | :--- |
| **Name:** TBD | **Name:** TBD |
| **Signature:** _________________________________________ | **Signature:** _________________________________________ |
| **Date:** TBD | **Date:** TBD |