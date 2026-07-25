# Production setup TODO

Repository implementation is complete for the security and efficiency hardening below. There are no production users,
so no data migration or compatibility rollout is required. Do not deploy App Check enforcement until the Android and
Apple app registrations and development debug tokens are configured.

## Completed in the repository

- [x] Initialize Android App Check before other Firebase use: Debug provider for debuggable builds and Play Integrity for
  release builds.
- [x] Initialize Apple App Check before FirebaseApp.configure(): Debug provider for Debug builds and App Attest with a
  DeviceCheck fallback for Release builds.
- [x] Add the production App Attest entitlement to iosApp.entitlements.
- [x] Enforce App Check in every Node and Python callable.
- [x] Authenticate and validate receipt requests before consuming per-UID or global quota.
- [x] Use the paid Vertex analyzeReceipt path in the app; retain the capped Developer API function only as an alternate.
- [x] Replace IP-only receipt quotas with Firebase-UID quotas.
- [x] Add Python per-UID daily quotas and explicit instance/concurrency ceilings.
- [x] Require exportId, claim it before Sheets creation, return completed results on retries, and recover stale leases.
- [x] Limit export input to 5,000 rows and 2 MiB decompressed JSON; validate dates, finite numeric amounts, layout,
  decimal precision, IDs, titles, labels, and cell lengths.
- [x] Separate trusted server formulas from user cell values and correctly escape formula string literals.
- [x] Send OAuth revocation tokens in the POST body and attempt revocation from the auth-deletion cleanup trigger.
- [x] Remove the client-supplied FCM token and spreadsheet URL notification path from export.
- [x] Retry transient Sheets errors with bounded backoff and make transient/idempotency failures retryable on clients.
- [x] Log receipt duration/token counts and export duration/row count without logging credentials or user data.
- [x] Remove critical/high Node production dependency advisories and update vulnerable Python dependencies.

## Firebase App Check setup

### Android / Play Integrity

- [ ] In Google Play Console, open **Release > App integrity > Play Integrity API** and link Firebase project
  pleasest-e3424.
- [ ] Add the debug and release SHA-256 signing-certificate fingerprints to the Firebase Android app.
- [ ] In Firebase Console **App Check > Apps**, register the Android app with Play Integrity.
- [ ] Select the correct Play Integrity policy for the distribution channel. For a Play-only release, require
  PLAY_RECOGNIZED and LICENSED; do not enable Strong integrity unless device-support implications are accepted.
- [ ] Launch a debug build, copy the App Check debug token from Logcat, and register it under the Android app's
  **Manage debug tokens** screen. Never commit this token.
- [ ] Install a signed release build from the intended distribution channel and verify callable requests appear as
  verified in App Check metrics.

### Apple / App Attest

- [ ] In Apple Developer **Certificates, Identifiers & Profiles**, enable App Attest for the iOS App ID and regenerate
  the development/distribution provisioning profiles.
- [ ] Confirm the signed Release app contains
  com.apple.developer.devicecheck.appattest-environment = production.
- [ ] In Firebase Console **App Check > Apps**, register the Apple app with the App Attest provider.
- [ ] If DeviceCheck fallback must support an older deployment target, create a DeviceCheck private key in Apple
  Developer and upload its Key ID, Team ID, and private key to the Apple App Check registration. Keep the key outside the
  repository.
- [ ] Launch the Debug build or simulator, copy the Firebase App Check debug token from Xcode logs, and register it under
  the Apple app's **Manage debug tokens** screen. Never commit this token.
- [ ] Test a signed Release build on a physical device and verify callable requests appear as verified in App Check
  metrics. App Attest does not work as a production attestation on the simulator.

## Firebase Authentication and Google OAuth setup

- [ ] Enable Google and Apple in Firebase Authentication **Sign-in method**.
- [ ] Replace the Android YOUR_WEB_CLIENT_ID placeholder with the Web/server OAuth client ID.
- [ ] Register Android debug/release SHA-1 and SHA-256 certificates and download the updated google-services.json.
- [ ] Download the updated GoogleService-Info.plist; configure GIDServerClientID and the reversed-client-ID URL
  scheme in the iOS target.
- [ ] Enable Sign in with Apple for the App ID and configure Firebase with the Apple Team ID, Key ID, and private key.
- [ ] Publish the Google OAuth consent screen in Production with the spreadsheets and drive.file scopes. Complete any
  required sensitive-scope verification.
- [ ] Verify Google identity sign-in requests identity scopes only and Google Sheets consent is requested only when the
  user connects export.

## Secrets and deployment

- [ ] Set GEMINI_API_KEY only if the capped analyzeReceiptGemini alternate will remain deployed.
- [ ] Set Google export OAuth secrets:
  ```bash
  firebase functions:secrets:set GOOGLE_OAUTH_CLIENT_ID
  firebase functions:secrets:set GOOGLE_OAUTH_CLIENT_SECRET
  firebase functions:secrets:set GOOGLE_TOKEN_ENCRYPTION_KEY
  ```
  `GOOGLE_TOKEN_ENCRYPTION_KEY` must be Fernet-compatible.
- [ ] Register both App Check apps and their development debug tokens before deploying callable enforcement.
- [ ] Run npm --prefix functions run test:unit, Python tests, Firestore emulator tests, Kotlin tests, Android/iOS
  compilation, detekt, and dependency audits.
- [ ] Deploy the complete TypeScript codebase:
  npx -y firebase-tools@latest deploy --only functions:ts --project pleasest-e3424.
- [ ] Deploy the complete Python codebase:
  npx -y firebase-tools@latest deploy --only functions:py --project pleasest-e3424.
- [ ] Verify the live inventory contains analyzeReceipt, analyzeReceiptGemini, rateLimitCleanup,
  cleanupGoogleOAuthOnUserDelete, linkGoogleAccount, hasGoogleAccountLink, unlinkGoogleAccount, and exportToSheets.
- [ ] Delete obsolete verifyGoogleToken and verifyAppleToken functions if they still exist.
- [ ] Do not create a migration. If development-only OAuth documents exist, delete those test records and reconnect the
  test accounts before release.

## Build and verification status (2026-07-12)

- [x] Node receipt/security tests: 3 passed; TypeScript compilation passed.
- [x] Python OAuth/export tests: 66 passed.
- [x] Firestore security-rules emulator tests: 4 passed.
- [x] OpenSpec strict validation passed for define-export-token-lifecycle.
- [x] Android host tests and Android debug Kotlin compilation passed.
- [x] Kotlin iOS simulator target compilation passed, including the export retry changes.
- [ ] Align the Compose iOS framework and local Xcode simulator SDK, then rerun the native iOS build. The current link
  uses the iOS 18.4 simulator SDK while a bundled Compose object targets 18.5, and fails on UIViewLayoutRegion before
  producing the app. Upgrade/select the compatible Xcode version or update the Compose toolchain, clean derived data,
  rebuild the Compose framework, and rerun xcodebuild.
- [ ] Run the final signed Android and iOS builds after the Firebase/Apple/Google console setup above is complete.
- [ ] Re-run the production dependency audits from a network-enabled shell. The last completed audit found no Python
  vulnerabilities and no critical/high Node production advisories; nine transitive moderate Node advisories remain
  documented and accepted pending a mutually compatible Firebase Admin/Functions release.

## Monitoring and cost controls

- [ ] Create Google Cloud budget alerts at 50%, 80%, and 100% of the agreed monthly budget.
- [ ] Add dashboards/alerts for callable error rate, p95 duration, App Check rejection rate, instance count, Firestore
  operation rate, Sheets/OAuth 429 and 5xx, receipt token counts, and Vertex AI spend.
- [ ] Confirm rateLimitCleanup removes expired rateLimits and callableRateLimits documents.
- [ ] Load-test receipt and export peaks below the configured Vertex AI and Sheets quotas. Google Sheets currently
  permits 300 write requests per minute per project.
- [ ] Re-run npm audit --omit=dev and pip-audit -r functions-py/requirements.txt before every production deployment;
  document any accepted moderate findings with reachability and owner.

## Runtime release checks

- [ ] Android: debug App Check, release Play Integrity, Google sign-in, first Sheets consent, repeat export, lost-response
  idempotent retry, forced reconnect, process-death WorkManager recovery, unlink, and account deletion.
- [ ] iOS: debug App Check, physical-device App Attest, Google sign-in, Apple sign-in, Apple-user Google connection,
  repeat export, bounded retry, forced reconnect, unlink, account deletion, and nonce cleanup on cancellation.
- [ ] Verify unauthenticated and unattested callable requests are rejected without consuming shared Gemini capacity.
- [ ] Verify malformed/oversized exports are rejected before OAuth and Sheets calls.
- [ ] Verify the same exportId never creates more than one spreadsheet and a lost successful response returns the
  existing spreadsheet URL on retry.
- [ ] Verify sign-out/account deletion attempts Google revocation while Firebase authentication is still available and
  always clears local identity state.
