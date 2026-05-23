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

- `./gradlew detekt -q` still fails on pre-existing unrelated issues.
- Export-related detekt warnings found during this pass were fixed.
- Last detekt run in this session reported one new `ExportWorkRunner` catch warning plus the existing backlog; the catch warning was fixed afterward, and detekt was not rerun per session instruction.
- Remaining failures should be handled separately or baselined intentionally.
