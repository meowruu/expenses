# TEAM ROLE CONTRACT: B3: SMS, PERMISSIONS AND INTEGRATION

| Attribute | Value | Attribute | Value |
| :--- | :--- | :--- | :--- |
| **Project** | TBD (Privacy-First Android UPI Expense Tracker) | **Git Branch** | `b3-sms` |
| **Role Assignment** | B3: SMS, Permissions and Integration | **Date / Sprint** | TBD (24-Hour Hackathon) |

## 1. Title and Metadata
- **Document Title:** Team Role Contract – B3: SMS, Permissions and Integration
- **Project Name:** TBD (Privacy-First Android Expense Tracker that auto-logs UPI bank SMS on-device)
- **Role:** B3: SMS, Permissions and Integration
- **Git Branch:** `b3-sms`
- **Date:** TBD (24-Hour Hackathon)

## 2. Purpose of the Role
The purpose of the B3 role is to establish the project foundation, build the real SMS ingestion pipeline, manage privacy permissions, and lead end-to-end integration. B3 configures the repository and Compose baseline, filters bank sender IDs, and creates the simulation workflow. B3 ensures seamless integration across all components and operates the golden device for demonstration.

## 3. Core Deliverables
| Task # | Task Description | Detailed Scope / Requirements | Done-When Criterion |
| :--- | :--- | :--- | :--- |
| **Project Setup** | GitHub Repo & Base Project Foundation | Create the GitHub repo and add everyone; set up the Android project (Kotlin + Jetpack Compose) with the blank app running on the emulator, and one branch per person. | GitHub repo active with all 5 members added, base project runs on emulator, and branches are created. |
| **Task 19** | Bank SMS BroadcastReceiver | SMS receiver that only accepts SMS from bank sender IDs (e.g. AXISBK). | Receiver filters incoming SMS, dropping non-bank messages and accepting bank sender IDs. |
| **Task 14** | "Simulate SMS" Pipeline Flow | "Simulate SMS" flow so the full pipeline works without a real SMS. | Simulate SMS flow feeds mock bank SMS into receiver and triggers full processing pipeline. |
| **Task 20** | Privacy-First Permission Flow | Permission flow: first explain what is read and why, then show the system popup, and only when the user turns "Auto-logging on". | Explanatory UI precedes system permission request; permission is triggered solely upon user toggle. |
| **Integration** | End-to-End System Test | Run the end-to-end test: Simulate SMS, entry created, category applied, summary updated. | Full integration test passes from simulated SMS to database entry and reactive summary update. |
| **Task 21** | Real Phone & Restricted Settings Testing | Test on a real phone, including "Allow restricted settings" on Android 13+. | App installs and operates on physical Android phone with Android 13+ restricted settings verified. |
| **Task 28** | Security Code Cross-Review | Review B4's security code (manifest, encryption, lock). Nobody reviews their own security code. | Formal security audit review signed off on B4's security deliverables. |
| **Demo Ops** | Golden Device Operation | Operate the "golden" phone or emulator for the demo (32) and the backup video (34). | Golden device prepped and operated smoothly during rehearsals, demo presentation, and video recording. |

## 4. Shared Obligations
- **Setup (Tasks 1-3): **Install Android Studio, emulator and Antigravity (1-3); review SPEC.md and RULES.md.
- **SMS Samples (Task 5): **Collect 4-5 real bank SMS samples with account digits and names masked, maintaining them in a samples/ folder.
- **Demo Flow (Task 32): **Rehearse the demo flow: open the app, unlock, SMS arrives, entry created and categorized, summary updates.
- **Backup Video (Task 34): **Operate the golden device and assist F1 with recording the backup video.

## 5. Dependencies
| Direction | What is Needed / Supplied | With Whom | When / Milestone | Purpose & Impact |
| :--- | :--- | :--- | :--- | :--- |
| **Inbound (Needs)** | SPEC.md & RULES.md (Task 4) | B4 | Kickoff (Hour 0-1) | Required to align receiver and permission flows with privacy and security constraints. |
| **Inbound (Needs)** | SMS Parser & OTP Filter (Tasks 10, 12, 13) | B1 | Integration Phase (Hour 8-12) | Required to parse intercepted SMS strings in the pipeline. |
| **Inbound (Needs)** | Repository save methods (Tasks 6, 15, 16) | B2 | Integration Phase (Hour 8-12) | Required to persist parsed transactions during end-to-end integration test. |
| **Inbound (Needs)** | Permission Explainer & Simulate UI | F1 | Integration Phase (Hour 6-10) | Required to trigger permission flow and simulation from the UI. |
| **Inbound (Needs)** | Review of receiver & permission code (Task 28) | B4 | Review Phase (Hour 14-18) | Required for independent security review of B3's code. |
| **Outbound (Supplies)** | GitHub Repo & Android Baseline | Entire Team | Kickoff (Hour 0-1) | Provides base workspace, emulator config, and personal git branches to all 5 members. |
| **Outbound (Supplies)** | Simulate SMS Mechanism (Task 14) | B1, B2, F1 | Core Build (Hour 4-8) | Enables team to test full pipeline without relying on cellular SMS. |
| **Outbound (Supplies)** | Security Code Review (Task 28) | B4 | Review Phase (Hour 14-18) | Performs independent review of B4's manifest, encryption, and lock code. |
| **Outbound (Supplies)** | Golden Device Operation (Tasks 32, 34) | Entire Team | Demo Phase (Hour 20-24) | Drives live presentation and backup video recording on prepped hardware/emulator. |

## 6. Cross-Review Duties
In strict adherence to RULES.md, nobody reviews their own security code. Task 28 explicitly mandates: B3 reviews B4's security code (manifest, encryption, lock). In return, B4 reviews B3's receiver and permission code. Both reviews must be completed and documented before final build integration.

## 7. Stretch Items
Task 22: Notification listener for GPay/PhonePe (Assigned to: B3) [Only if time is left; otherwise roadmap slide]. Note: Shared team stretch items include Task 30 (CSV export - F1) and Task 31 (More banks/merchants - B1).

## 8. Non-Negotiable Rules from RULES.md
- **No INTERNET permission: **App must function 100% on-device; zero network permissions in AndroidManifest.xml.
- **No raw SMS saved: **Store only necessary fields (amount, debit/credit, merchant, UPI ID, ref number, date, bank, category); never save raw SMS text.
- **OTP rejected first: **OTP messages must be identified and rejected first, before any other parsing occurs.
- **allowBackup="false": **android:allowBackup="false" must be strictly configured in manifest to prevent ADB extraction.

## 9. Working Agreement
- **Regular Short Syncs: **Participate in short, frequent standups across the 24-hour sprint to catch blockers early.
- **Small Frequent Commits: **Make small, frequent commits to git with clear messages to avoid merge conflicts.
- **Dedicated Personal Branch: **Work exclusively on git branch b3-sms and integrate via tested pull requests.
- **Escalate Blockers Early: **Escalate any technical hurdle or cross-role blocker immediately to the team and lead.

## 10. Acceptance Checklist and Signatures
### Acceptance Checklist
- [ ] GitHub repository created, all 5 members added, base Compose project running on emulator.
- [ ] Personal branches (b1-parser, b2-data, b3-sms, b4-security, f1-ui) established.
- [ ] Android Studio, emulator, and Antigravity installed; SPEC.md and RULES.md reviewed.
- [ ] 4-5 masked real bank SMS samples collected and placed in samples/ folder.
- [ ] SMS receiver (Task 19) accepts only bank sender IDs (e.g. AXISBK).
- [ ] Simulate SMS flow (Task 14) enables complete pipeline testing without real SMS.
- [ ] Permission flow (Task 20) explains read/why before popup, triggered on "Auto-logging on".
- [ ] End-to-end test executed: Simulate SMS -> entry created -> category applied -> summary updated.
- [ ] Tested on real phone (Task 21), including "Allow restricted settings" on Android 13+.
- [ ] B4's security code reviewed (Task 28: manifest, encryption, lock) and signed off.
- [ ] B3's receiver and permission code submitted to B4 for review and approved.
- [ ] Golden device prepped and operated for demo flow (Task 32) and backup video (Task 34).
- [ ] Compliance verified: no INTERNET, no raw SMS saved, OTP rejected first, allowBackup="false".

### Signature Block

| Team Member Sign-off (B3: SMS, Permissions and Integration) | Team Lead Sign-off (Project Lead / PM) |
| :--- | :--- |
| **Name:** TBD | **Name:** TBD |
| **Signature:** _________________________________________ | **Signature:** _________________________________________ |
| **Date:** TBD | **Date:** TBD |