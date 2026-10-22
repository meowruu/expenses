# TEAM ROLE CONTRACT: B4: SECURITY, RULES AND PITCH

| Attribute | Value | Attribute | Value |
| :--- | :--- | :--- | :--- |
| **Project** | TBD (Privacy-First Android UPI Expense Tracker) | **Git Branch** | `b4-security` |
| **Role Assignment** | B4: Security, Rules and Pitch | **Date / Sprint** | TBD (24-Hour Hackathon) |

## 1. Title and Metadata
- **Document Title:** Team Role Contract – B4: Security, Rules and Pitch
- **Project Name:** TBD (Privacy-First Android Expense Tracker that auto-logs UPI bank SMS on-device)
- **Role:** B4: Security, Rules and Pitch
- **Git Branch:** `b4-security`
- **Date:** TBD (24-Hour Hackathon)

## 2. Purpose of the Role
The purpose of the B4 role is to establish and enforce the app's security architecture, privacy constraints, and hackathon presentation. B4 authors the core specifications, configures database encryption and access locks, and audits compliance against all non-negotiable rules. B4 also articulates the product vision, privacy advantages, and future roadmap to hackathon judges.

## 3. Core Deliverables
| Task # | Task Description | Detailed Scope / Requirements | Done-When Criterion |
| :--- | :--- | :--- | :--- |
| **Task 4** | Architectural Specs & Rules (Kickoff) | Write SPEC.md (what the app does) and RULES.md (no INTERNET permission, no raw SMS saved, OTP rejected first). Everyone reviews it. | SPEC.md and RULES.md are documented, committed to repository, and reviewed by all team members. |
| **Task 23** | Manifest Security Hardening | Manifest with allowBackup="false" and no INTERNET permission. | Manifest explicitly sets allowBackup="false" and contains zero INTERNET permissions. |
| **Task 25** | Database Encryption with SQLCipher | Database encryption with SQLCipher. | Room database is encrypted on disk with SQLCipher and unreadable in plaintext. |
| **Task 26** | Data Storage Privacy Enforcement | Store only the necessary fields, never the raw SMS. | Audit confirms zero raw SMS strings are stored anywhere in database, logs, or preferences. |
| **Task 24** | Biometric/PIN Lock & FLAG_SECURE | Biometric/PIN lock and FLAG_SECURE. | App prompts for biometric/PIN upon launch/resume and blocks screenshots/recording via FLAG_SECURE. |
| **Task 27** | Privacy Backend & Data-Wipe Routine | Backend for the "what we read and why" screen and the "delete all data" button. | Backend provides explanatory text and invokes B2's data-wipe routine to clear all data. |
| **Task 28** | Security Code Cross-Review | Get B3 to review the security code, and review B3's receiver and permission code in return. | Cross-review completed: B3 signs off on security code, and B4 signs off on receiver/permission code. |
| **Compliance** | Integrated Build Compliance Audit | Confirm the integrated build still follows every rule in RULES.md. | Integrated APK passes compliance audit against all 4 non-negotiable RULES.md constraints. |
| **Task 33** | Hackathon Pitch Slide Deck | Slides covering the problem, the on-device privacy design, the security layers built, and the roadmap (Account Aggregator, iOS, encrypted sync). | Pitch deck completed, formatted, and aligned with live product demo flow. |

## 4. Shared Obligations
- **Setup (Tasks 1-3): **Install Android Studio, emulator and Antigravity (1-3); author and distribute SPEC.md and RULES.md.
- **SMS Samples (Task 5): **Collect 4-5 real bank SMS samples with account digits and names masked, maintaining them in a samples/ folder.
- **Demo Flow (Task 32): **Rehearse the demo flow: open the app, unlock, SMS arrives, entry created and categorized, summary updates.
- **Backup Video (Task 34): **Rehearse demo flow and assist with backup video coordination.

## 5. Dependencies
| Direction | What is Needed / Supplied | With Whom | When / Milestone | Purpose & Impact |
| :--- | :--- | :--- | :--- | :--- |
| **Inbound (Needs)** | Data-wipe function | B2 | Integration Phase (Hour 8-12) | Required to wire up backend for "delete all data" button (Task 27). |
| **Inbound (Needs)** | Room database instance | B2 | Database Build (Hour 4-8) | Required to configure SQLCipher database encryption wrapper (Task 25). |
| **Inbound (Needs)** | Receiver and permission code (Task 28) | B3 | Review Phase (Hour 14-18) | Required for security cross-review of B3's implementation. |
| **Inbound (Needs)** | Settings & Lock UI (Task 27 UI, Lock UI) | F1 | Integration Phase (Hour 8-12) | Required to wire up authentication and privacy settings screens. |
| **Inbound (Needs)** | Team review of SPEC.md & RULES.md | Entire Team | Kickoff (Hour 0-1) | All 5 members must review and sign off on foundational rules. |
| **Outbound (Supplies)** | SPEC.md & RULES.md (Task 4) | Entire Team | Kickoff (Hour 0-1) | Establishes non-negotiable architectural constraints and feature specifications. |
| **Outbound (Supplies)** | Hardened Manifest (Task 23) | Project Baseline | Setup Phase (Hour 1-2) | Hardens AndroidManifest.xml with allowBackup="false" and zero INTERNET permission. |
| **Outbound (Supplies)** | SQLCipher Encryption Setup (Task 25) | B2 | Database Build (Hour 4-8) | Delivers encryption wrapper for Room database. |
| **Outbound (Supplies)** | Biometric/PIN Lock & FLAG_SECURE (Task 24) | F1 | Core Build (Hour 8-12) | Delivers security lock logic and window protection. |
| **Outbound (Supplies)** | Privacy Backend & Data-Wipe Logic (Task 27) | F1 | Core Build (Hour 8-12) | Delivers backend for settings and wipe button. |
| **Outbound (Supplies)** | Security Code Review (Task 28) | B3 | Review Phase (Hour 14-18) | Reviews B3's receiver and permission code. |
| **Outbound (Supplies)** | Hackathon Pitch Slides (Task 33) | Entire Team | Presentation Phase (Hour 18-22) | Delivers presentation slide deck. |

## 6. Cross-Review Duties
In strict adherence to RULES.md, nobody reviews their own security code. Task 28 explicitly mandates: B4 submits security code (manifest, encryption, lock) to B3 for independent review. In return, B4 reviews B3's receiver and permission code. Both reviews must be documented and signed off prior to final integration.

## 7. Stretch Items
No stretch tasks are directly assigned to B4. Shared team stretch items include Task 22 (Notification listener for GPay/PhonePe - B3), Task 30 (CSV export - F1), and Task 31 (More banks/merchants - B1) [All stretch items: only if time is left; otherwise roadmap slide]. Note: B4 covers these features as roadmap items on slide deck (Task 33).

## 8. Non-Negotiable Rules from RULES.md
- **No INTERNET permission: **App must function 100% on-device; zero network permissions in AndroidManifest.xml.
- **No raw SMS saved: **Store only necessary fields (amount, debit/credit, merchant, UPI ID, ref number, date, bank, category); never save raw SMS text.
- **OTP rejected first: **OTP messages must be identified and rejected first, before any other parsing occurs.
- **allowBackup="false": **android:allowBackup="false" must be strictly configured in manifest to prevent ADB extraction.

## 9. Working Agreement
- **Regular Short Syncs: **Participate in short, frequent standups across the 24-hour sprint to catch blockers early.
- **Small Frequent Commits: **Make small, frequent commits to git with clear messages to avoid merge conflicts.
- **Dedicated Personal Branch: **Work exclusively on git branch b4-security and integrate via tested pull requests.
- **Escalate Blockers Early: **Escalate any technical hurdle or cross-role blocker immediately to the team and lead.

## 10. Acceptance Checklist and Signatures
### Acceptance Checklist
- [ ] Android Studio, emulator, and Antigravity installed.
- [ ] SPEC.md and RULES.md written (Task 4) and reviewed by all team members.
- [ ] Git branch b4-security established with frequent, documented commits.
- [ ] Manifest configured (Task 23) with allowBackup="false" and zero INTERNET permission.
- [ ] Database encryption with SQLCipher (Task 25) integrated with B2's Room database.
- [ ] Storage privacy enforced (Task 26): audit confirms no raw SMS saved.
- [ ] Biometric/PIN lock and FLAG_SECURE (Task 24) implemented and operational.
- [ ] Backend for "what we read and why" and "delete all data" (Task 27) implemented.
- [ ] Security code reviewed by B3 (Task 28) and signed off.
- [ ] B3's receiver and permission code reviewed by B4 (Task 28) and approved.
- [ ] Integrated build confirmed to follow every rule in RULES.md.
- [ ] Pitch slides (Task 33) created covering problem, privacy, security, and roadmap.
- [ ] Demo flow (Task 32) rehearsed and backup video (Task 34) supported.

### Signature Block

| Team Member Sign-off (B4: Security, Rules and Pitch) | Team Lead Sign-off (Project Lead / PM) |
| :--- | :--- |
| **Name:** TBD | **Name:** TBD |
| **Signature:** _________________________________________ | **Signature:** _________________________________________ |
| **Date:** TBD | **Date:** TBD |