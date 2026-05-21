# PlzStop - Expense Tracker

A personal expense tracking app built with Kotlin Multiplatform and Compose Multiplatform. Track expenses manually or by scanning receipts with AI. See spending trends at a glance.

## Platforms

- Android
- iOS

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Compose Multiplatform, Material 3 |
| Architecture | MVI (StateHolder + sealed State/Event) |
| Networking | Ktor |
| Database | Room + SQLCipher |
| DI | Koin |
| Navigation | Navigation 3 |
| Image loading | Coil |
| Receipt AI | Firebase Cloud Functions + Gemini 2.5 Flash |
| Analytics | Amplitude |
| Async | Kotlin Coroutines + Flow |
| Build | Gradle (version catalog), Detekt |

## Features

| Feature | Description |
|---------|-------------|
| [Onboarding](docs/features/onboarding.md) | Setup wizard: currency, name, budget |
| [Home](docs/features/home.md) | Dashboard with categories and spending overview |
| [Add/Edit Expense](docs/features/add-expense.md) | Calculator keyboard, receipt scanning, currency conversion |
| [Receipt Items](docs/features/receipt-items.md) | Batch receipt line item editor |
| [Analytics Overview](docs/features/analytics-overview.md) | Pie chart and category spending breakdown |
| [Monthly Expenses](docs/features/monthly-expenses.md) | Month-by-month expense list with day grouping |

## Project Structure

```
PlzStop/
  composeApp/
    src/
      commonMain/          Shared code (all features, core, navigation, DI)
      androidMain/         Android platform code
      iosMain/             iOS platform code
      commonTest/          Shared tests
  iosApp/                  iOS app entry point (SwiftUI bridge)
  functions/               Firebase Cloud Functions (receipt analysis)
  build-logic/             Gradle build configuration
  config/                  Detekt and other config
  docs/
    features/              Functional specifications
    cloudfunc/             Cloud function deployment guide
```

### App Architecture

```
composeApp/src/commonMain/kotlin/com/please/stop/app/
  core/
    db/                    Room database, entities, migrations
    models/                Shared domain and UI models
    stateholder/           MVI base classes, StateSaver
  navigation/              Navigation 3 routes, router, bottom tabs
  network/                 HTTP client, flowFromSuspend, mapToResult
  di/                      Koin root modules
  features/
    home/                  Home dashboard
      data/                Repository implementations
      domain/              Use cases, repository interfaces, models
      presentation/        StateHolder, State, Event, UI
    expenses/              Add/edit expense + receipt items
      data/
      domain/
      presentation/
      create/              Create expense flow
      edit/                Edit expense flow
      receiptitems/        Receipt items editor
    analytics/             Overview + monthly breakdown
      data/
      domain/
      presentation/
      monthly/             Monthly expenses sub-feature
    onboarding/            First-launch setup
      data/
      domain/
      presentation/
```

## Build & Run

```bash
# Android
./gradlew :composeApp:assembleDebug

# iOS
# Open iosApp/ in Xcode and run

# Lint
./gradlew detekt
```

## Developer Tooling

- [Android CLI with Codex and Claude Code](docs/dev-tools/android-cli.md)
- [Codex Status Line](docs/dev-tools/codex-status-line.md)

## Cloud Functions

See [docs/cloudfunc/deploy-cloud-functions.md](docs/cloudfunc/deploy-cloud-functions.md) for deployment instructions.

```bash
cd functions && npm install && npm run build && cd .. && firebase deploy --only functions
```
