<p align="center">
  <img src="app/src/main/res/drawable/app_logo.png" alt="Adulting Logo" width="120" height="120" style="border-radius: 24px;" />
</p>

<h1 align="center">Adulting</h1>

<p align="center">
  <strong>The Local-First, Privacy-Preserving Personal Finance & Monthly Cashflow Planner for Android.</strong>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=flat-square" alt="License: Apache 2.0" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin 2.2" /></a>
  <a href="https://developer.android.com/about/versions/16"><img src="https://img.shields.io/badge/Android%20SDK-Target%2036%20%7C%20Min%2024-3DDC84.svg?style=flat-square&logo=android&logoColor=white" alt="Android SDK" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20Material%203-4285F4.svg?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose & Material 3" /></a>
  <a href="https://developer.android.com/training/data-storage/room"><img src="https://img.shields.io/badge/Storage-Room%20SQLite%20(Local--First)-F4511E.svg?style=flat-square&logo=sqlite&logoColor=white" alt="Room SQLite" /></a>
  <a href="https://github.com/CodeMaverick-143/adulting/actions"><img src="https://img.shields.io/badge/CI-GitHub%20Actions-2088FF.svg?style=flat-square&logo=githubactions&logoColor=white" alt="CI Status" /></a>
  <a href="CONTRIBUTING.md"><img src="https://img.shields.io/badge/PRs-Welcome-brightgreen.svg?style=flat-square" alt="PRs Welcome" /></a>
</p>

---

## 💡 About Adulting

Managing finances shouldn't require surrendering your personal privacy to third-party cloud servers or paying recurring subscription fees just to track where your money goes.

**Adulting** is a modern, privacy-first Android application designed to give you total mastery over your monthly cashflow. Built using **Jetpack Compose**, **Material 3**, and **Android Room**, Adulting stores 100% of your financial information locally on your device. No ads, no third-party tracking, no account requirements, and zero remote data harvesting.

Whether you're balancing multiple income streams, juggling recurring utility bills, or forecasting your runway months into the future, Adulting provides a calm, reliable, and deterministic workspace for your money.

---

## ✨ Key Features

### 🗓️ Monthly Financial Planner & Cockpit
- Real-time aggregation of **Expected vs. Received Income** and **Planned vs. Paid Bills**.
- Dynamic remaining cashflow balance calculations that adapt as payments are logged.
- Prominent overdue alerts highlighting bills requiring immediate attention.
- Fast month-to-month switcher to review past financial history or plan future budgets.

### 💼 Multi-Stream Income Management
- Track salary, freelancing, investments, side hustles, and allowances in one unified view.
- Flexible payment schedules: **Weekly**, **Biweekly**, **Monthly**, **Quarterly**, **Half-Yearly**, **Yearly**, or **One-Time**.
- Log received payments directly with actual amounts and receipt dates.

### 💳 Recurring Payments, Bills & Subscriptions
- Organize housing, utilities, subscriptions (Netflix, Spotify), EMIs, and insurance policies.
- Smart recurrence engine that handles leap years, variable month lengths (28, 29, 30, 31 days), and date clamping.
- Quick **Mark as Paid** action toggles with full historical amount preservation.

### 📈 Month-by-Month Predictive Forecasting
- Interactive multi-month cashflow runway projections.
- Forecast future balances by expanding active recurrence schedules into upcoming months.
- One-tap navigation from any forecasted month directly into that month's planner.

### 🎯 Category-Level Budget Caps & Alert Thresholds
- Set monthly spending ceilings for specific expense categories (e.g. Dining, Shopping, Entertainment).
- Configurable warning thresholds (e.g. alert when spending crosses 80% of budget cap).
- Real-time progress bars and visual indicators showing category health.

### ⏰ Exact Alarms & System Notifications
- High-priority local bill reminders powered by Android `AlarmManager` (`SCHEDULE_EXACT_ALARM`).
- Interactive notification action buttons: **Mark as Paid** or **Snooze** directly from the status bar.
- Resilient `BootReceiver` that automatically reschedules all active alarms when the device reboots or timezones change.

### 🔒 100% Local-First & Private by Design
- All records are saved in a private SQLite database inside the app's sandboxed storage.
- Never sends your financial data over the network.
- Completely functional offline without an internet connection.

---

## 🏛️ System Architecture

Adulting is engineered according to **Clean Architecture** and **Unidirectional Data Flow (UDF)**:

```
┌─────────────────────────────────────────────────────────────┐
│                       Jetpack Compose UI                    │
│   (PlannerScreen, IncomeScreen, BillsScreen, ForecastScreen)│
└──────────────────────────────▲──────────────────────────────┘
                               │ StateFlow (UI State)
                               │ User Events
┌──────────────────────────────▼──────────────────────────────┐
│                  FinancialPlannerViewModel                  │
│       (State coordination, coroutines, screen workflows)    │
└──────────────────────────────▲──────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
┌───────────────────────────────┐   ┌─────────────────────────┐
│         Domain Layer          │   │       Data Layer        │
│   (RecurrenceCalculator,      │   │  (Repository, Room DB,  │
│    AppDate, YearMonth)        │   │   DAOs, Migrations)     │
└───────────────────────────────┘   └─────────────┬───────────┘
                                                  ▼
                                    ┌─────────────────────────┐
                                    │    SQLite Database      │
                                    │ (adulting_database.db)  │
                                    └─────────────────────────┘
```

For an in-depth breakdown of components, data models, and database migrations, see our [ARCHITECTURE.md](ARCHITECTURE.md).

---

## 🛠️ Tech Stack & Libraries

| Category | Technology | Description |
| :--- | :--- | :--- |
| **Language** | [Kotlin 2.2](https://kotlinlang.org/) | Modern, safe JVM language with Coroutines & Flow |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) | Declarative UI toolkit with Material 3 components |
| **Design System** | [Material 3](https://m3.material.io/) | Dynamic theming, custom color palettes, elevated surfaces |
| **Architecture** | Clean Architecture / MVVM | Separation of concerns with unidirectional data flow |
| **Local Database** | [Android Room 2.7.0](https://developer.android.com/training/data-storage/room) | SQLite abstraction with KSP code generation |
| **Navigation** | [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) | Single-activity Compose-native navigation graph |
| **Background Alarms** | Android `AlarmManager` | Exact alarms for persistent, battery-friendly bill notifications |
| **Secrets Management** | [Secrets Gradle Plugin](https://github.com/google/secrets-gradle-plugin) | Secure local property injection via `.env` |
| **Target Platform** | Android 16 (API 36) | Min SDK: Android 7.0 (API 24) |
| **Testing** | JUnit 4, Robolectric, Coroutines Test | Fast JVM unit testing and simulated Android runtime tests |

---

## 📂 Repository Structure

```
adulting/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml          # Permissions, receivers & activity declaration
│   │   │   ├── java/com/example/
│   │   │   │   ├── AdultingApp.kt           # Application class & reminder initialization
│   │   │   │   ├── MainActivity.kt          # Single-activity Compose entry point
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/               # Room DB, Entities, DAOs & Migrations
│   │   │   │   │   ├── model/               # Planner models, summaries & enums
│   │   │   │   │   └── repository/          # FinancialPlannerRepository
│   │   │   │   ├── domain/                  # RecurrenceCalculator, AppDate, YearMonth
│   │   │   │   ├── reminder/                # Alarm scheduler & Broadcast receivers
│   │   │   │   └── ui/
│   │   │   │       ├── bills/               # Recurring bills management screen
│   │   │   │       ├── components/          # Metric cards, chips & dialogs
│   │   │   │       ├── forecast/            # Month-by-month predictive forecast
│   │   │   │       ├── income/              # Multi-stream income screen
│   │   │   │       ├── navigation/          # Navigation destinations & bottom bar
│   │   │   │       ├── planner/             # Monthly cashflow summary screen
│   │   │   │       ├── theme/               # Material 3 typography & color tokens
│   │   │   │       ├── transactions/        # Historical ledger screen
│   │   │   │       └── viewmodel/           # FinancialPlannerViewModel
│   │   │   └── res/                         # Vector drawables, launcher icons & strings
│   │   └── test/                            # Unit tests for domain algorithms & repository
│   └── build.gradle.kts                     # App module configuration & dependencies
├── gradle/
│   └── libs.versions.toml                   # Centralized Gradle version catalog
├── website/                                 # Landing page & privacy policy
├── .github/
│   ├── workflows/ci.yml                     # Automated GitHub Actions build & test CI
│   ├── ISSUE_TEMPLATE/                      # Bug report & feature request templates
│   └── pull_request_template.md             # Standardized pull request template
├── ARCHITECTURE.md                          # In-depth architectural documentation
├── CHANGELOG.md                             # Version history and release notes
├── CODE_OF_CONDUCT.md                       # Contributor Covenant Code of Conduct
├── CONTRIBUTING.md                          # Contribution guidelines & dev setup
├── LICENSE                                  # Apache License 2.0
├── README.md                                # Project master documentation
└── SECURITY.md                              # Vulnerability reporting & security policy
```

---

## 🚀 Getting Started

### Prerequisites

Before building Adulting, ensure you have:
- **JDK 17**: Eclipse Temurin, Azul Zulu, or OpenJDK.
- **Android Studio**: Meerkat (2024.3+) or newer.
- **Android SDK**:
  - `compileSdk`: **36** (Android 16)
  - `targetSdk`: **36**
  - `minSdk`: **24** (Android 7.0)

### 1. Clone the Repository

```bash
git clone https://github.com/CodeMaverick-143/adulting.git
cd adulting
```

### 2. Configure Environment

Copy the example environment configuration:

```bash
cp .env.example .env
```

*(The `.env` file allows optional Gemini AI keys if desired, but is not needed for core local-first budgeting).*

### 3. Open in Android Studio

1. Open Android Studio and select **Open Project**.
2. Choose the `adulting` directory.
3. Wait for Gradle to download dependencies and sync.
4. Select a virtual or physical device running Android 7.0+ (API 24 to 36).
5. Press **Run** (`Shift + F10`) to build and install the debug application.

---

## 🔨 Build Commands

All standard Gradle commands can be run from the command line:

```bash
# Build the debug APK
./gradlew assembleDebug

# Build the release APK (signed with release.keystore)
./gradlew assembleRelease

# Build the Android App Bundle (AAB for Google Play)
./gradlew bundleRelease

# Clean previous build artifacts
./gradlew clean
```

The compiled artifacts will be located at:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release Bundle (AAB): `app/build/outputs/bundle/release/app-release.aab`

### 🚀 Automated GitHub Releases (AAB Distribution)

Whenever a significant change is made, you can create a release in two ways:

1. **Push a Version Tag**:
   ```bash
   git tag v2.0.0
   git push origin v2.0.0
   ```
   GitHub Actions automatically builds the `.aab` bundle and `.apk`, generates release notes from your commits, and attaches them to a new [GitHub Release](https://github.com/CodeMaverick-143/adulting/releases).

2. **Manual Dispatch from GitHub Actions**:
   Go to **Actions** → **Build & Publish Release** → click **Run workflow**, specify the version (e.g. `v2.0.0`), and trigger the release build.

---

## 🧪 Testing & Code Quality

Adulting maintains high test standards for financial calculation reliability:

```bash
# Run all JVM unit and Robolectric tests
./gradlew testDebugUnitTest

# Run Android Lint static code analysis
./gradlew lintDebug
```

Unit test reports are generated under:
`app/build/reports/tests/testDebugUnitTest/index.html`

---

## 📦 Database Migrations

Adulting uses Room's schema migration system to preserve user data across app updates:

- **Version 1**: Initial schema with incomes, recurring payments, payment occurrences, transactions, and reminders.
- **Version 2 (`MIGRATION_1_2`)**: Added `budget_caps` table with unique indexing to support monthly spending ceilings and proactive warning alerts.

Destructive migrations are strictly disabled in production builds to prevent accidental data loss.

---

## 🤝 Contributing

We welcome contributions from everyone! Whether it's adding support for custom recurrence patterns, improving Material 3 animations, or writing tests:

1. Read our [Code of Conduct](CODE_OF_CONDUCT.md).
2. Review our [Contributing Guidelines](CONTRIBUTING.md) for branch naming and commit conventions.
3. Check out the [Architecture Guide](ARCHITECTURE.md) to understand data flow before making changes.
4. Open a Pull Request following our [PR Template](.github/pull_request_template.md).

---

## 🛡️ Security & Privacy

If you discover a security vulnerability, please do not file a public issue. Follow our [Security Policy](SECURITY.md) and report it to **security@ferrixlabs.in**.

For information on how user data is protected, read our [Privacy Policy](https://adulting.ferrixlabs.in/privacy-policy) or view [website/privacy-policy.html](website/privacy-policy.html).

---

## 📜 License

Adulting is open-source software licensed under the **[Apache License 2.0](LICENSE)**.

```
Copyright 2026 Adulting Project Authors & Ferrix Labs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

---

<p align="center">
  Crafted with care by <strong><a href="https://adulting.ferrixlabs.in">Ferrix Labs</a></strong> and open-source contributors.
</p>
