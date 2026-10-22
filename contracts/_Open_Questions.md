# OPEN QUESTIONS & TBD TRACKER

**Source Document:** Team Task List: Everyone, End to End (24 Hours)

**Status:** Documented for team lead alignment and hackathon sprint resolution. In strict compliance with technical writing guidelines, no information has been invented.

## Summary of TBD Items

| # | Open Item / Parameter | Context in Source Document | Missing Specification | Recommended Resolution / Hackathon Action |
| :--- | :--- | :--- | :--- | :--- |
| **1** | **Team Member Names** | Document lists roles as B1, B2, B3, B4, F1. | Specific legal/student names of members are omitted. | Assign actual team member names to role codes during initial kickoff standup. |
| **2** | **Team Lead Name** | Document assigns coordination tasks to lead/team. | Explicit designation of which person is Team Lead is missing. | Designate Team Lead (typically B3 or B4 given repo/spec responsibilities) at kickoff. |
| **3** | **Official Product Name** | Project is described as a privacy-first Android expense tracker. | Official consumer app brand name is omitted. | Agree on a concise hackathon product name (e.g., "SnoopFree Expense", "TrackUPI", "ZeroNet Expense"). |
| **4** | **Exact Calendar Date & Milestones** | Document states "24 Hours" duration. | Specific calendar start date, timezone, and hourly phase checkpoints (H+2, H+8, H+18) are omitted. | Establish hackathon kickoff timestamp and bind dependency handoffs to elapsed sprint hours. |
| **5** | **Code Review Partners for B1, B2, F1** | Task 28 explicitly specifies cross-review between B3 and B4. | Explicit peer review partners for B1 (parser), B2 (database), and F1 (Compose UI) are not assigned. | Assign B2 to review B1's parser repo integration, and B1 to review F1's UI data binding. |
| **6** | **Target Android SDK Version** | Task 21 cites "Android 13+ restricted settings". | Specific minSdk, compileSdk, and targetSdk are not enumerated. | Configure `compileSdk = 34`, `targetSdk = 34`, `minSdk = 26` (Android 8.0 Oreo) for broad UPI compatibility. |
| **7** | **Physical Test Phone Make & Model** | Task 21 requires testing on a "real phone". | Specific hardware model, OEM skin (MIUI, OneUI, Pixel), and Android build are unspecified. | Select one team member's phone running Android 13 or 14 to verify notification/SMS restricted settings. |
| **8** | **Golden Demo Device Specification** | B3 operates the golden phone or emulator for Task 32 and 34. | Decision whether golden presentation device is physical hardware or emulator is TBD. | B3 to set up a clean Android emulator image (API 34) as primary, with physical phone as secondary backup. |
| **9** | **Backup Video Storage & Format** | Task 34 states F1 records backup video. | File resolution, maximum duration, hosting drive, and submission format are omitted. | Standardize on 1080p MP4, under 3 minutes, saved in a shared repository `/demo/` directory and cloud backup. |
| **10** | **Payee Rules Database Schema** | Tasks 13 and 15 reference "B2's payee rules table". | Specific table columns (payee identifier pattern, default category, override timestamp) are omitted. | B2 to define concise Room entity: `PayeeRule(id, pattern, categoryId, userModifiedTimestamp)`. |