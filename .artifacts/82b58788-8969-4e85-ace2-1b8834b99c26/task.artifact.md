# Task List - Fix MMRA Startup Flow, Account Navigation, and Avatar Initials

- `[x]` Task 1: Add initials helper and safe dev-only reset helper in `UserProfileManager.kt`.
- `[x]` Task 2: Wire up `onAccountClick` navigation to `MY_ACCOUNT` across `HomeScreen`, `MedicineScreen`, `ScheduleScreen`, `HistoryScreen`, and `MMRANavHost.kt`.
- `[x]` Task 3: Update headers (`HomeHeader`, `MedicineHeader`, `ScheduleHeader`) and `HistoryScreen` to use dynamic initials from `UserProfileManager`.
- `[x]` Task 4: Run `./gradlew :app:assembleDebug` and verify build and fixes.
