## 1. Backend OAuth Contract

- [x] 1.1 Add shared backend constants for the required `spreadsheets` and `drive.file` scopes and reject a link exchange
  that does not return both scopes.
- [x] 1.2 Return stable callable `details.reason` values for unauthenticated, missing-code, missing-refresh-token,
  missing-scope, reconnect-required, and token-endpoint-unavailable failures without logging provider bodies.
- [x] 1.3 Write and read Google grants only as `encryptedRefreshToken` with scope and timestamp metadata, and leave an
  existing valid grant unchanged on failed relink.
- [x] 1.4 Update export-time credential refresh to delete permanently invalid grants and return
  `GOOGLE_RECONNECT_REQUIRED`, while keeping transient endpoint failures retryable.
- [x] 1.5 Make unlink delete stored ciphertext even when Google reports an already-invalid token, and add account-deletion
  cleanup that removes any remaining `googleOAuthAccounts/{uid}` record.
- [x] 1.6 Deny all client access to `googleOAuthAccounts` in Firestore rules and add emulator coverage proving cross-UID
  and direct-client reads/writes fail.

## 2. Platform Sign-In and Authorization Separation

- [x] 2.1 Replace the shared multi-token `GoogleUser` contract with distinct identity-sign-in and Sheets-authorization
  results so no shared UI/domain model exposes a Google access token.
- [x] 2.2 Keep Android Google sign-in on Credential Manager with identity scopes only, and expose a separate foreground
  AuthorizationClient operation that requests the two export scopes plus offline access and returns only a server code.
- [x] 2.3 Add an Android reconnect variant that uses the provider-supported explicit-consent prompt when a new refresh
  token is required, and verify cancellation clears the pending authorization result.
- [x] 2.4 Split the iOS Google bridge into basic Firebase sign-in and Sheets offline authorization paths; discard basic-flow
  access tokens/server codes and return only the server code from the export connection path.
- [x] 2.5 Verify the iOS Apple bridge clears the identity token callback and raw nonce on success, cancellation, malformed
  response, and Firebase rejection, with a new nonce for every reauthentication.
- [x] 2.6 Add platform tests proving basic Google/Apple sign-in establishes the same Firebase session behavior without
  requesting or persisting Sheets credentials.

## 3. Shared Auth and Export Flow

- [x] 3.1 Update `ConnectGoogleAccountUseCase` and the export UI to consume only a one-time server authorization code,
  preserve the current Firebase UID, and continue export after a successful link.
- [x] 3.2 Remove `ConfirmExport(accessToken)` and all client access-token branches from export events, state handling,
  repository contracts, and composables.
- [x] 3.3 Change Google-link checking to distinguish linked, unlinked, and lookup-failed states instead of mapping server
  or network failure to unlinked.
- [x] 3.4 Map structured callable reasons to foreground sign-in/reconnect UX and terminal background results without
  parsing human-readable provider messages.
- [x] 3.5 Add assertions around Android WorkManager input, the iOS scheduler, and `ExportWorkRunner` payload construction
  proving that no token, authorization code, or nonce field is persisted or sent to `exportToSheets`.
- [x] 3.6 Remove FCM registration-token retrieval and spreadsheet-URL notification delivery from export so the callable
  result and idempotency record are the only remote result channels.
- [x] 3.7 Reorder sign-out and account-deletion cleanup so remote unlink runs while Firebase auth is available, local
  identity sessions always clear on unlink failure, and recent-login reauthentication occurs before destructive cleanup.

## 4. Storage and Operations

- [x] 4.1 Define a clean rollout with no legacy-field fallback or migration; delete development-only grant records and
  reconnect test accounts before onboarding real users.
- [x] 4.2 Document encryption-key rotation and key-loss recovery, including retaining the old key during re-encryption
  and deleting records to force reconnect if ciphertext cannot be recovered.
- [ ] 4.3 Verify the Google OAuth consent screen is production-ready for the two export scopes so production refresh tokens
  are not subject to the seven-day Testing-mode lifetime.
- [x] 4.4 Update `docs/cloudfunc/export-to-sheets-api.md` to remove `googleAccessToken`, document link/status/unlink callables,
  and publish the structured reconnect/error contract.
- [x] 4.5 Mark the contradictory token decisions in `docs/wip/wip_export-to-sheets-plan.md` as superseded by this OpenSpec
  change, while preserving the unrelated CSV and workbook design notes.
- [x] 4.6 Create `docs/features/export.md` as the end-to-end technical specification covering Google and Apple sign-in,
  Firebase sessions, Google Sheets linking and token exchange, background export, CSV export, reconnect, unlink, and error
  paths; include Mermaid architecture, sequence, and state diagrams, and verify every diagram renders successfully.

## 5. Verification

- [x] 5.1 Extend Python unit tests for unauthenticated link/export, encryption at rest, required scopes, missing refresh
  tokens, non-destructive relink failure, invalid-grant deletion, revocation cleanup, and token redaction.
- [x] 5.2 Add shared/platform tests for Google identity sign-in, Apple nonce handling, Firebase-session restoration,
  link-state lookup errors, token-free export payloads, reconnect mapping, and sign-out cleanup failure.
- [ ] 5.3 Runtime-test Android Google sign-in, first Sheets consent, repeat export without consent, forced reconnect,
  process-death worker recovery, and sign-out while offline.
- [ ] 5.4 Runtime-test iOS Google sign-in, Apple sign-in, Apple-user Google connection, repeat export, forced reconnect,
  background scheduling, and nonce cleanup on cancellation.
- [x] 5.5 Run the Python function tests, common/platform Kotlin tests, Android and iOS compilation checks, detekt, and
  OpenSpec validation; record any unrelated baseline failures separately.
- [x] 5.6 Add App Check initialization for Android/iOS, enforce it on all callables, add per-UID quotas and export
  idempotency/input bounds, harden Sheets formulas/OAuth revocation, and document the external production setup checklist.
