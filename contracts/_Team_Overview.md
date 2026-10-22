# TEAM OVERVIEW: PRIVACY-FIRST ANDROID UPI EXPENSE TRACKER

**Sprint Duration:** 24-Hour Hackathon | **Architecture:** Kotlin + Jetpack Compose, On-Device Processing, Zero Internet Permission

## Executive Team Matrix
| Role | Git Branch | Focus Area | Core Tasks | Key Deliverables | Critical Dependencies | Cross-Review Partner |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **B1** | `b1-parser` | SMS Parser & Brain | 5, 10, 11, 12, 13 | Bank SMS parser, OTP reject filter, regex unit test suite, intelligent categorizer, samples/ bank SMS folder. | Needs B4 specs, B2 data class (Task 6), B2 payee rules (15), B3 receiver (19). Supplies parser & categorizer to B3 & B2. | Integration reviews with B2 and B3; parser peer review TBD. |
| **B2** | `b2-data` | Data Model & Database | 6, 15, 16, 17, 18 | Transaction data class (first priority), Room DB, ViewModels, dedupe logic, "My UPI IDs" self-transfer logic, data-wipe function. | Needs B4 rules, SQLCipher setup (25), B1 entity format. Supplies Task 6 first to F1/B1, real ViewModels to F1, wipe to B4. | Collaborates with B4 on SQLCipher and zero raw SMS audit; repo peer review TBD. |
| **B3** | `b3-sms` | SMS Pipeline & Integration | 1-3, 14, 19, 20, 21, 28 | GitHub repo setup, Android Compose project, bank SMS receiver (AXISBK), Simulate SMS flow, privacy permission flow, golden device. | Needs B4 rules, B1 parser/OTP (10, 12), B2 repo (6, 15), F1 UI. Supplies project baseline to team, Simulate SMS to team. | Reviews B4's security code (Task 28); submits receiver & permissions to B4. |
| **B4** | `b4-security` | Security, Rules & Pitch | 4, 23-28, 33 | SPEC.md & RULES.md, hardened manifest, SQLCipher encryption, zero raw SMS audit, biometric/PIN lock, FLAG_SECURE, pitch deck. | Needs B2 wipe function (27), B2 Room DB (25), B3 receiver code (28), F1 lock/settings UI. Supplies SPEC/RULES to team, pitch deck. | Submits security code to B3 (Task 28); reviews B3's receiver & permissions. |
| **F1** | `f1-ui` | Frontend (Compose) | 7, 8, 9, 29, 34 + all UI | Manual entry screen, Home dashboard, Transaction detail, Simulate UI, Permission UI, Settings & Lock UI, Category chart, backup video. | Needs B3 Compose baseline, B2 data class (Task 6 first), B2 real ViewModels (16), B4 lock/wipe logic. Supplies UI to team, backup video. | Reviews ViewModel state flow with B2 and permission/simulate UX with B3; UI peer review TBD. |

## Shared Team Commitments
- **Setup (Tasks 1-3):** Install Android Studio, emulator, and Antigravity; review SPEC.md and RULES.md.
- **SMS Samples (Task 5):** Collect and maintain 4-5 masked real bank SMS samples in `samples/`.
- **Demo Flow (Task 32):** Open app -> Biometric/PIN unlock -> Bank SMS arrives -> Auto-logged and categorized -> Monthly summary updates.
- **Backup Video (Task 34):** Recorded by F1 using golden device operated by B3 with full team support.
- **Stretch Roadmap Items (Cut if time runs short):** Task 22 (Notification listener - B3), Task 30 (CSV export - F1), Task 31 (More banks/merchants - B1). Single chart retained for Task 29.