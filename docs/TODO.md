# TODO

## App

- [ ] **Replace `webClientId` placeholder** — `PlatformModule.android.kt:46` has `"YOUR_WEB_CLIENT_ID"`. Create a Web OAuth client ID in Google Cloud Console (project `pleasest-e3424`) and replace it.
- [ ] **Handle `UNAUTHENTICATED` errors from cloud functions** — Catch `FirebaseFunctionsException` with code `UNAUTHENTICATED` in `ReceiptRepositoryImpl` and `ExportWorker`. Redirect to sign-in or show a message.
- [ ] **Apple Sign-In button** — `AuthScreen.kt:100` has a TODO for platform-specific `AppleButtonUiContainer`.
- [ ] **Send `exportId` in cloud function payload** ��� `ExportWorker` must include `exportId` in the request data so the cloud function can write it to Firestore.
- [ ] **Sync pending exports on app open** — On app launch, query Firestore `exports/{uid}/history` for completed exports and sync status + URL to local `ExportHistoryDao`. This recovers exports where the worker was killed before getting the response.
- [ ] **Firestore security rules for exports** — Allow authenticated users to read only their own exports: `match /exports/{uid}/history/{exportId} { allow read: if request.auth.uid == uid; }`. Deny client writes (server-only).

## Cloud Functions Deploy

- [ ] **Deploy updated TS functions** — `receipt.ts` now requires auth. `cd functions && npm run build && cd .. && firebase deploy --only functions:ts`
- [ ] **Deploy Python codebase** — `functions-py/` is a new codebase (exportToSheets). `firebase deploy --only functions:py`
- [ ] **Delete orphaned verify functions** — `verifyGoogleToken` and `verifyAppleToken` may still be deployed. Delete them: `firebase functions:delete verifyGoogleToken verifyAppleToken --region europe-west1`
- [ ] **Enable App Check** — `index.ts:42` and `index.ts:97` have `enforceAppCheck: false`. Enable when ready (requires client-side App Check SDK on both platforms).
- [ ] **Push notifications** — FCM is only used inline in `exportToSheets`. No standalone push function exists. Add one if needed for other features.

## Docs

- [ ] **Update export plan doc** — `docs/wip/wip_export-to-sheets-plan.md:1418` still references deleted `verifyGoogleToken` and `verifyAppleToken`.
