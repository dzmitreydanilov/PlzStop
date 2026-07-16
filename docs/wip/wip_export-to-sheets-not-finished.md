# Export to Spreadsheet or CSV — Not Finished

These items remain after the current plan execution pass.

## iOS Export Runtime Verification

- iOS now has a real export scheduler path:
  - `IosExportWorkerScheduler` launches the shared `ExportWorkRunner`.
  - `ExportWorkRunner` builds the payload, calls `exportToSheets`, and updates `export_history`.
  - Swift registers `AppFcmTokenBridge` for optional FCM token delivery.
  - iOS app backgrounding no longer cancels app-scope child jobs, so a started export is not cancelled by `scenePhase == .background`.
  - Large payload compression is decided in shared code and uses gzip/base64 on both Android and iOS.
- Runtime-test a Google Sheets export on an iOS device or simulator.

## Deep Link and Spreadsheet Opening

- Runtime-test notification tap handling on a device or simulator:
  - Android receives `plzstop://open?url=...` through launch and warm intents.
  - iOS receives `plzstop://open?url=...` through SwiftUI `onOpenURL`.
  - Google Sheets URLs with query and fragment parts are preserved after encode/decode.

## Python Cloud Function Runtime Test

- Emulator-test or deploy-test `functions-py/exportToSheets`.
- Verify:
  - callable auth is required
  - raw and gzip/base64 payloads both parse
  - single-tab exports create one worksheet with category summary
  - separate-tabs exports use the full category union on every month tab
  - FCM notification failure does not fail the export
  - returned `spreadsheetUrl` is persisted by the worker

Unit coverage for the Python helper logic is in place and currently passes with:
`python3 -m pytest functions-py`

## Lint Baseline

- `./gradlew :composeApp:detekt --auto-correct -q` was rerun after the export work.
- Export/auth changes have no remaining detekt findings.
- Detekt still exits non-zero on the unrelated repository baseline: 50 weighted issues in existing analytics, date/time,
  navigation, theming, scanner, onboarding, and shared UI files.
- Remaining failures should be handled separately or baselined intentionally.

## iOS Test Linker Baseline

- Shared and iOS platform test sources compile with `:composeApp:compileTestKotlinIosSimulatorArm64`.
- The same common suite executes through Android KMP host tests: 60 tests pass.
- `:composeApp:iosSimulatorArm64Test` and the final Xcode app link are blocked by the local toolchain mismatch: the
  Compose framework references `UIViewLayoutRegion`, but Xcode 16.3 / iOS Simulator SDK 18.4 does not provide the
  expected symbol. Swift source compilation, including the auth bridges, completes before this unrelated link failure.
- Upgrade/alignment of Xcode, the simulator SDK, Compose, and Kotlin/Native is tracked separately from the export change.

## External Release Verification

- Verify and record that the Google OAuth consent screen is published/Production and approved for the `spreadsheets` and
  `drive.file` scopes. Repository code cannot prove Google Cloud Console state.
- Complete the Android and iOS interactive runtime matrices in OpenSpec tasks 5.3 and 5.4 on configured devices/accounts.
