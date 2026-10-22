# TEAM ROLE CONTRACT: F1: FRONTEND (JETPACK COMPOSE)

| Attribute | Value | Attribute | Value |
| :--- | :--- | :--- | :--- |
| **Project** | TBD (Privacy-First Android UPI Expense Tracker) | **Git Branch** | `f1-ui` |
| **Role Assignment** | F1: Frontend (Jetpack Compose) | **Date / Sprint** | TBD (24-Hour Hackathon) |

## 1. Title and Metadata
- **Document Title:** Team Role Contract – F1: Frontend (Jetpack Compose)
- **Project Name:** TBD (Privacy-First Android Expense Tracker that auto-logs UPI bank SMS on-device)
- **Role:** F1: Frontend (Jetpack Compose)
- **Git Branch:** `f1-ui`
- **Date:** TBD (24-Hour Hackathon)

## 2. Purpose of the Role
The purpose of the F1 role is to design and develop all user-facing interfaces using Jetpack Compose. F1 builds reactive screens for manual expense logging, monthly tracking, transaction management, and privacy settings. F1 integrates real ViewModels into the UI to deliver an intuitive experience and records the backup demonstration video.

## 3. Core Deliverables
| Task # | Task Description | Detailed Scope / Requirements | Done-When Criterion |
| :--- | :--- | :--- | :--- |
| **UI Baseline** | Fake Data Screen Prototyping | Start screens with fake data as soon as B2 pushes the data class and interfaces. | UI screens render preview mockups in Compose without waiting for real database implementation. |
| **Task 7** | Manual Expense Entry Screen | Manual expense entry screen: amount, note, category, date, optional UPI ID. | Screen allows user to input all transaction fields and submit to ViewModel. |
| **Task 8** | Home Dashboard Screen | Home screen: this month's total, category-wise summary, recent list. | Home screen displays reactive monthly total, categorized breakdown cards, and recent items list. |
| **Task 9** | Transaction Detail Screen | Transaction detail screen where the category can be changed. | Detail screen displays transaction attributes and allows user to update category, persisting the change. |
| **UI Components** | Supporting Screens & Components | Simulate SMS button UI, permission explainer UI, and the "My UPI IDs" screen; Lock screen UI, and Settings screen with "what we read and why" and the "delete all data" button (27, UI part). | All modal dialogs, explainer layouts, lock UI, and settings screens are fully built and styled in Compose. |
| **Integration** | ViewModel Integration | Replace fake data with B2's real ViewModels. | All UI components observe real LiveData/StateFlow from B2's ViewModels and update reactively. |
| **Task 29** | Category Chart & UI Polish | One simple category chart, plus basic polish. | A clean category distribution chart is rendered and general UI styling/spacing is polished. |
| **Task 34** | Demo Video Recording | Record the backup video (34). | Backup video recording captured, verified clear, and stored safely for hackathon submission. |

## 4. Shared Obligations
- **Setup (Tasks 1-3): **Install Android Studio, emulator and Antigravity (1-3); review SPEC.md and RULES.md.
- **SMS Samples (Task 5): **Collect 4-5 real bank SMS samples with account digits and names masked, maintaining them in a samples/ folder.
- **Demo Flow (Task 32): **Rehearse the demo flow: open the app, unlock, SMS arrives, entry created and categorized, summary updates.
- **Backup Video (Task 34): **Rehearse demo flow and record the official backup video using the golden device.

## 5. Dependencies
| Direction | What is Needed / Supplied | With Whom | When / Milestone | Purpose & Impact |
| :--- | :--- | :--- | :--- | :--- |
| **Inbound (Needs)** | Base Android Compose project | B3 | Setup Phase (Hour 0-1) | Required base repository and blank app setup on emulator. |
| **Inbound (Needs)** | Transaction class & Repo interfaces (Task 6) | B2 | First Priority (Hour 1-2) | First priority dependency required to start building UI screens with fake data. |
| **Inbound (Needs)** | Real ViewModels (Task 16) | B2 | Integration Phase (Hour 6-10) | Required to replace fake data with live database feeds. |
| **Inbound (Needs)** | Simulate SMS trigger & Permission callback | B3 | Integration Phase (Hour 8-12) | Required to wire up simulation button and permission explainer. |
| **Inbound (Needs)** | Biometric/PIN lock logic & Privacy backend (24, 27) | B4 | Integration Phase (Hour 8-12) | Required to bind lock screen and settings/wipe buttons. |
| **Inbound (Needs)** | Golden device operation for recording (Tasks 32, 34) | B3 | Recording Phase (Hour 20-22) | B3 operates device while F1 records backup video. |
| **Outbound (Supplies)** | Permission Explainer & Simulate SMS UI | B3 | Integration Phase (Hour 6-10) | Enables B3 to test permission and simulation flows. |
| **Outbound (Supplies)** | Settings Screen & Lock Screen UI | B4 | Integration Phase (Hour 8-12) | Enables B4 to test security mechanisms and data wipe. |
| **Outbound (Supplies)** | Complete User Interface | Entire Team | Demo Phase (Hour 20-24) | Provides complete visual app for demo flow (Task 32). |
| **Outbound (Supplies)** | Official Backup Demo Video (Task 34) | Entire Team | Final Submission (Hour 22-24) | Delivers recorded backup video file. |

## 6. Cross-Review Duties
In strict adherence to RULES.md, nobody reviews their own security code. F1 reviews ViewModel state observation and UI data binding with B2, and reviews permission explainer and Simulate SMS trigger UX with B3. Designated peer reviewer for F1's Jetpack Compose UI code is TBD (assigned to team peer review).

## 7. Stretch Items
Task 30: CSV export (UPI ID excluded by default) (Assigned to: F1) [Only if time is left; otherwise roadmap slide]. Note on Task 29: If time runs short, keep a single category chart and defer advanced charts to roadmap slide. Shared stretch items include Task 22 (Notification listener - B3) and Task 31 (More banks/merchants - B1).

## 8. Non-Negotiable Rules from RULES.md
- **No INTERNET permission: **App must function 100% on-device; zero network permissions in AndroidManifest.xml.
- **No raw SMS saved: **Store only necessary fields (amount, debit/credit, merchant, UPI ID, ref number, date, bank, category); never save raw SMS text.
- **OTP rejected first: **OTP messages must be identified and rejected first, before any other parsing occurs.
- **allowBackup="false": **android:allowBackup="false" must be strictly configured in manifest to prevent ADB extraction.

## 9. Working Agreement
- **Regular Short Syncs: **Participate in short, frequent standups across the 24-hour sprint to catch blockers early.
- **Small Frequent Commits: **Make small, frequent commits to git with clear messages to avoid merge conflicts.
- **Dedicated Personal Branch: **Work exclusively on git branch f1-ui and integrate via tested pull requests.
- **Escalate Blockers Early: **Escalate any technical hurdle or cross-role blocker immediately to the team and lead.

## 10. Acceptance Checklist and Signatures
### Acceptance Checklist
- [ ] Android Studio, emulator, and Antigravity installed; SPEC.md and RULES.md reviewed.
- [ ] Git branch f1-ui established with frequent, documented commits.
- [ ] 4-5 masked real bank SMS samples collected and placed in samples/ folder.
- [ ] Screens started with fake data immediately after B2 pushed data class and interfaces.
- [ ] Manual expense entry screen (Task 7) built (amount, note, category, date, optional UPI ID).
- [ ] Home screen (Task 8) built (this month's total, category summary, recent list).
- [ ] Transaction detail screen (Task 9) built allowing category modifications.
- [ ] Simulate SMS UI, Permission explainer UI, and "My UPI IDs" screen implemented.
- [ ] Lock screen UI and Settings screen with "what we read and why" and "delete all data" button built.
- [ ] Fake data replaced with B2's real ViewModels across all screens.
- [ ] One simple category chart and basic UI polish (Task 29) completed.
- [ ] Demo flow (Task 32) rehearsed and backup video (Task 34) recorded.
- [ ] Compliance verified: no INTERNET, no raw SMS saved, OTP rejected first, allowBackup="false".

### Signature Block

| Team Member Sign-off (F1: Frontend (Jetpack Compose)) | Team Lead Sign-off (Project Lead / PM) |
| :--- | :--- |
| **Name:** TBD | **Name:** TBD |
| **Signature:** _________________________________________ | **Signature:** _________________________________________ |
| **Date:** TBD | **Date:** TBD |