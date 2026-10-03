# Changelog

All notable changes to the **Adulting** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Home screen app widgets for quick balance overview and upcoming bills.
- Encrypted local backup and restore to user-selected device storage.
- Multi-currency support and locale-aware number formatting.
- Biometric app lock (Fingerprint / Face Unlock).
- Custom category creation and custom tagging for transactions.

---

## [2.0.0] - 2026-09-29

### Added
- **Budget Caps & Threshold Alerts**:
  - Added `BudgetCapEntity` and `BudgetCapDao` allowing users to configure monthly spending limits per category.
  - Added configurable alert thresholds (e.g. notify when spending exceeds 80% of budget).
  - Implemented Room database migration `MIGRATION_1_2` with automatic table creation and category index.
- **Predictive Month-by-Month Forecasting**:
  - Added `MonthByMonthForecastScreen` projecting cashflow and balances across upcoming months.
  - Added multi-month timeline calculations in `FinancialPlannerViewModel`.
- **System Reminders & Exact Alarms**:
  - Implemented `ReminderScheduler` leveraging Android `AlarmManager` (`SCHEDULE_EXACT_ALARM`).
  - Added `ReminderBroadcastReceiver` with interactive notification actions: **Mark as Paid** and **Snooze**.
  - Added `BootReceiver` to re-register alarms upon device reboot (`BOOT_COMPLETED`), timezone shift, or app update.
- **Enhanced UI & Navigation**:
  - Redesigned navigation bar with Material 3 icons and clean route switching.
  - Added branded top app bar with custom logo and contextual subtitles.
  - Added transaction history ledger screen (`TransactionsScreen`).
- **Target SDK Upgrade**:
  - Upgraded compile and target SDK to Android 16 (API 36, minorApiLevel 1).
  - Added runtime permission handling for `POST_NOTIFICATIONS` on Android 13+.

### Changed
- Refactored `RecurrenceCalculator` to support leap years, boundary clamping, and historical amount preservation.
- Migrated codebase to Kotlin 2.2 and Compose Compiler plugin.
- Standardized secrets handling via Gradle Secrets Plugin and `.env.example`.

### Fixed
- Fixed date overflow when projecting occurrences from 31-day months into shorter months (e.g. February).
- Fixed notification cancellation when recurring payments are deleted.

---

## [1.0.0] - 2026-08-15

### Added
- Initial public release of Adulting.
- Core monthly financial planning overview (Expected vs Received, Planned vs Paid).
- Income stream tracking with customizable frequencies (Weekly, Biweekly, Monthly, etc.).
- Recurring bills tracking with category classifications.
- Offline Room SQLite database architecture.
- Jetpack Compose and Material 3 design system.
