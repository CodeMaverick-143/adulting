# Contributing to Adulting

Thank you for your interest in contributing to **Adulting**! We are thrilled to welcome developers, designers, writers, and financial enthusiasts to help build the best local-first personal finance platform for Android.

Please take a moment to review this guide before submitting code or proposing changes.

---

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). Please report any violations or unacceptable behavior to **contact@ferrixlabs.in**.

---

## How Can You Contribute?

You can contribute to Adulting in many ways:

1. **Reporting Bugs**: Found a bug or unexpected calculation? Open an issue using our [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.yml).
2. **Suggesting Features**: Have an idea for a budget view, calendar integration, or widget? Submit a proposal via our [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.yml).
3. **Submitting Pull Requests**: Implement a bugfix, add a new feature, or improve performance.
4. **Improving Documentation**: Fix typos, add architectural diagrams, or write setup guides.
5. **Localization & Translations**: Help translate strings in `res/values/` to reach users worldwide.

---

## Development Setup

### 1. Prerequisites

- **Java Development Kit (JDK)**: JDK 17 (Eclipse Temurin, Azul Zulu, or Android Studio bundled OpenJDK).
- **IDE**: [Android Studio Meerkat](https://developer.android.com/studio) (or newer) / IntelliJ IDEA.
- **Android SDK**:
  - `compileSdk`: **36** (Android 16, minorApiLevel 1)
  - `targetSdk`: **36**
  - `minSdk`: **24** (Android 7.0 Nougat)
  - Android SDK Build-Tools and Platform-Tools installed.

### 2. Clone the Repository

```bash
git clone https://github.com/CodeMaverick-143/adulting.git
cd adulting
```

### 3. Configure Secrets (Optional)

Adulting uses the Secrets Gradle Plugin for optional AI or cloud capabilities:

```bash
cp .env.example .env
```

If you are using Gemini AI integrations, add your API key to `.env`:
```env
GEMINI_API_KEY=your_gemini_api_key_here
```
*(For standard offline budgeting and local-first use, this is entirely optional.)*

### 4. Open in Android Studio

1. Launch Android Studio.
2. Select **Open** and navigate to the project directory.
3. Allow Gradle to sync dependencies and build the project index.
4. Connect an Android device with Developer Mode & USB Debugging enabled, or start an Android Emulator (API 24 to 36).
5. Click **Run 'app'** (`Shift + F10`) to build and launch the application.

---

## Development Workflow & Git Standards

### 1. Branching Strategy

- `main`: Production-ready branch. Must remain stable and compilable at all times.
- Feature branches: Branch off `main` using descriptive prefixes:
  - `feature/add-budget-rollover`
  - `fix/leap-year-calculation`
  - `docs/update-architecture-guide`
  - `refactor/room-dao-queries`

### 2. Conventional Commits

We follow the [Conventional Commits](https://www.conventionalcommits.org/) standard. Commit messages must be structured as follows:

```
<type>(<optional scope>): <short summary>

[optional body explaining rationale]

[optional footer(s), e.g., Closes #123]
```

#### Allowed Types:
- `feat`: A new feature for users.
- `fix`: A bug fix.
- `docs`: Documentation-only changes.
- `style`: Formatting, missing semicolons, whitespace (no functional change).
- `refactor`: Code refactoring without adding features or fixing bugs.
- `perf`: Performance improvement.
- `test`: Adding or correcting tests.
- `build`: Changes affecting the build system or dependencies (Gradle, version catalog).
- `ci`: Changes to CI configuration or GitHub Actions.
- `chore`: Routine maintenance, updating gitignore, etc.

*Example:*
```bash
feat(bills): add support for bi-monthly recurrence frequency
fix(forecast): correct year rollover when projecting December to January
docs(readme): add architecture flowchart and setup instructions
```

---

## Coding Standards & Architectural Guidelines

### 1. Architectural Integrity
Adulting strictly adheres to **Clean Architecture** principles combined with **MVVM** and **Unidirectional Data Flow (UDF)**:
- **UI Layer (`com.example.ui`)**: Declarative Jetpack Compose and Material 3 components. Screens consume state via Kotlin `StateFlow` and emit events back to the ViewModel. Keep Composables stateless where feasible.
- **Domain Layer (`com.example.domain`)**: Pure Kotlin business rules and algorithms without Android framework dependencies. Date logic must use `AppDate` and `YearMonth` domain primitives.
- **Data Layer (`com.example.data`)**: Repositories abstracting the Room SQLite Database (`AppDatabase`), Data Access Objects (`Dao`), and local entities. Data is exposed to consumers as reactive Kotlin `Flow`s.

### 2. Date & Time Handling
- Always use `com.example.domain.AppDate` and `com.example.domain.YearMonth` for recurrence calculations and database persistence.
- Do **not** use heavy java.time or ThreeTenABP libraries that break API 24 compatibility or cause time-zone jitter on local-first budgets.
- All date calculations must be deterministic and testable across leap years, 30/31-day boundaries, and year transitions.

### 3. Room Database Changes
- Room entities must use clear column names and defaults.
- Any change to the database schema **requires** incrementing the database version in `AppDatabase.kt` and writing an explicit migration object (e.g. `MIGRATION_1_2`).
- Do not use destructive migration (`fallbackToDestructiveMigration(true)` is strictly prohibited in production builds).

### 4. Jetpack Compose Best Practices
- Use Material 3 design tokens (`MaterialTheme.colorScheme`, `MaterialTheme.typography`).
- Add `@Preview` annotations for reusable UI components.
- Assign `modifier = Modifier.testTag(...)` to interactive buttons, navigation items, and inputs to facilitate automated UI testing.

---

## Testing Your Changes

Before submitting your pull request, ensure all tests and lint checks pass cleanly.

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Run Android Lint
```bash
./gradlew lintDebug
```

### Build APKs
```bash
./gradlew assembleDebug
```

### Writing Tests
- All business algorithms in `com.example.domain` must include corresponding unit tests in `app/src/test/java/com/example/domain/`.
- Repository methods and database queries should be verified with Robolectric tests or in-memory Room instances.

---

## Submitting a Pull Request (PR)

1. **Push your branch** to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
2. **Open a Pull Request** against the `main` branch of `CodeMaverick-143/adulting`.
3. **Fill out the Pull Request Template**:
   - Provide a clear summary of what was changed and why.
   - Reference related issues (e.g., `Closes #42`).
   - Include before/after screenshots or GIFs for visual and UI modifications.
4. **Respond to Code Reviews**:
   - Maintainers will review your PR. Be open to feedback and suggestions.
   - Once approved and CI checks pass, your PR will be squash-merged into `main`.

---

## Need Help?

Have questions or need guidance on an implementation detail?
- Open a GitHub Discussion.
- Email the maintainers at **contact@ferrixlabs.in**.

Thank you for helping make Adulting better for everyone! 🚀
