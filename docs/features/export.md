# Export Feature Technical Specification

## Purpose and scope

The export feature sends a selected expense date range either to a user-owned Google Spreadsheet or to a local CSV share
sheet. Google/Apple sign-in authenticates the user to PlzStop through Firebase. Google Sheets authorization is a separate,
incremental permission flow. CSV export is local and has no authentication, OAuth, callable, or Firestore dependency.

This document is the implementation contract for Android, iOS, shared Kotlin, Firebase Auth, callable functions,
Firestore, Google OAuth, and Google Sheets. It supersedes the credential decisions in
`docs/wip/wip_export-to-sheets-plan.md`.

## Security invariants

1. A Google or Apple provider credential is acquired only after an explicit foreground sign-in or reauthentication action.
2. Basic sign-in never requests Google Sheets or Drive scopes.
3. A Sheets connection returns only a one-time server authorization code to shared code.
4. No Google access token, refresh token, Firebase token, authorization code, or nonce is persisted in UI state, local
   storage, WorkManager input, the iOS scheduler, or `exportToSheets` request data.
5. Firebase callable authentication is the only client identity used by the backend; App Check independently verifies
   that requests originate from an attested PlzStop app.
6. The Google Sheets refresh token is durable only as backend-encrypted ciphertext keyed by the verified Firebase UID.
7. A Google Sheets access token exists only in function memory for one export invocation.
8. Export requests use a durable local export ID as a server idempotency key.
9. Link lookup failure is an error/unknown state, not an unlinked state.
10. CSV export remains available without Firebase or Google.

## Architecture

```mermaid
flowchart LR
    subgraph Device[Android or iOS]
        IdentityUI[Google or Apple identity UI]
        SheetsUI[Google Sheets consent UI]
        Attestation[Play Integrity, App Attest, or debug provider]
        FirebaseSDK[Firebase Auth and callable SDKs]
        ExportUI[Export MVI state holder]
        Job[Background export job config]
        LocalDB[(Local expense and export history DB)]
        CsvShare[Local CSV share sheet]
    end

    subgraph Firebase[Firebase and Cloud Functions]
        Auth[Firebase Authentication]
        LinkFn[Link, status, and unlink callables]
        ExportFn[exportToSheets callable]
        TokenStore[(Server-only encrypted grant store)]
    end

    subgraph Google[Google services]
        OAuth[OAuth token and revoke endpoints]
        Sheets[Sheets and Drive APIs]
    end

    IdentityUI -->|transient identity credential| FirebaseSDK
    FirebaseSDK --> Auth
    Attestation --> FirebaseSDK
    SheetsUI -->|one-time server code| ExportUI
    ExportUI -->|code through callable SDK| LinkFn
    LinkFn --> OAuth
    LinkFn --> TokenStore
    ExportUI -->|non-secret configuration| Job
    Job --> FirebaseSDK
    FirebaseSDK -->|Firebase ID token plus App Check token| ExportFn
    ExportFn --> TokenStore
    ExportFn --> OAuth
    OAuth -->|short-lived access token| ExportFn
    ExportFn --> Sheets
    Job --> LocalDB
    ExportUI -->|local rows only| CsvShare
```

The device-to-backend boundary has no path for a Google Sheets refresh or access token. The callable SDK adds Firebase
authentication and App Check attestation outside the application payload.

### App Check on Apple and Android

- Android Debug builds use Firebase's debug provider. Release builds use Play Integrity.
- Apple Debug builds and the iOS Simulator use Firebase's debug provider. Release builds use App Attest on iOS 14 and
  later, with DeviceCheck fallback for an older supported OS.
- The Apple factory is installed before FirebaseApp.configure(). The App Attest entitlement uses the production
  environment because Firebase does not accept sandbox App Attest assertions.
- Debug tokens are registered manually in Firebase Console and kept out of source control.
- Every callable sets App Check enforcement, so a valid Firebase user without a valid app attestation is rejected.
- App Check identifies a genuine app/device instance; it does not authenticate the user or replace Google/Apple sign-in.

## Credential and token lifecycle

| Value | Acquisition point | Purpose | Durable owner | Disposal or revocation |
|---|---|---|---|---|
| Google ID token | Explicit basic Google sign-in or reauthentication | Build a Firebase Google credential | None | Discard after Firebase accepts/rejects it |
| Google client access token | May be returned incidentally by Google iOS SDK | No PlzStop use | None | Keep inside platform SDK/adapter and discard |
| Google server authorization code | Explicit Sheets consent with offline access | One exchange at `linkGoogleAccount` | None | Discard after one attempt; never replay |
| Apple identity token | Explicit Sign in with Apple or reauthentication | Build a Firebase Apple credential | None | Discard after Firebase accepts/rejects it |
| Apple raw nonce | Generated immediately before each Apple request | Bind Apple response to Firebase request | None | Memory only; clear on success, error, malformed response, or cancellation |
| Firebase ID token | Firebase SDK session | Authenticate callable requests | Firebase SDK | SDK refreshes it; app never puts it in payloads |
| Firebase refresh token | Firebase sign-in | Restore and refresh Firebase session | Firebase SDK | Cleared/invalidated by sign-out, revocation, or account deletion |
| Google Sheets refresh token | Backend exchange of server code | Mint access while user is absent | Backend encrypted store | Revoke/delete on unlink and cleanup; delete when invalid |
| Google Sheets access token | Refresh grant inside `exportToSheets` | Call Sheets/Drive | Function memory | Drop when invocation ends |
| Local Google link marker | Successful link response | Non-secret UX cache | Platform local storage | Delete on unlink/reconnect; never treat as authoritative |

## Federated identity sign-in

### Google

- Android uses Credential Manager and `GetGoogleIdOption`. The request is identity-only and yields
  `GoogleSignInCredential(idToken)`.
- iOS uses Google Sign-In without additional Sheets scopes. Incidental access tokens or server codes do not enter the
  shared result. Firebase receives an ID-token-only `OAuthProvider` credential inside the Swift adapter.
- Shared auth code passes the transient ID token directly to the platform Firebase adapter. It does not connect Sheets.

### Apple

- Apple sign-in exists on iOS only.
- Swift generates a cryptographically random raw nonce and sends its SHA-256 hash to Apple.
- The returned identity token and original nonce are passed once to Firebase.
- The bridge clears the stored callbacks and raw nonce on success, provider error/cancellation, malformed response, and
  Kotlin coroutine cancellation. A later reauthentication creates a new nonce.
- PlzStop does not store or exchange Apple's authorization code and does not hold an Apple export grant.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as PlzStop sign-in UI
    participant Provider as Google or Apple UI
    participant Adapter as Platform auth adapter
    participant Firebase as Firebase Auth SDK

    User->>UI: Choose Google or Apple
    alt Google
        UI->>Provider: Request identity credential only
        Provider-->>Adapter: Google ID token
        Adapter->>Firebase: Sign in with ID-token credential
    else Apple on iOS
        Adapter->>Adapter: Generate fresh raw nonce
        UI->>Provider: Request Apple identity with nonce hash
        Provider-->>Adapter: Identity token
        Adapter->>Firebase: Sign in with identity token and raw nonce
    end
    Firebase-->>UI: Authenticated Firebase user and UID
    UI->>UI: Drop transient provider values
    Note over UI,Firebase: Firebase SDK owns session restoration and token refresh
```

At application launch, Firebase restores its own session. PlzStop does not silently reopen Google or Apple provider UI.

## Google Sheets connection

The client first checks its encrypted last-known-linked marker. A connected marker skips `hasGoogleAccountLink` and lets
`exportToSheets` validate the refresh token while starting the export. If the marker is missing or disconnected, the
client calls `hasGoogleAccountLink`; a `linked=false` result starts authorization for these two scopes and offline access
with the Web/server client ID:

- `https://www.googleapis.com/auth/spreadsheets`
- `https://www.googleapis.com/auth/drive.file`

Android uses Google Identity Services `AuthorizationClient`. iOS uses Google Sign-In with additional scopes and the server
client ID. Normal connection does not force consent. A reconnect request uses the provider-supported explicit-consent
path so Google can issue a replacement refresh token. Android sets the consent prompt; iOS disconnects the stale Google
grant before starting authorization.

The Sheets authorization result is `GoogleSheetsAuthorizationCode`; it cannot be passed to Firebase sign-in APIs. Thus an
Apple-authenticated user remains the same Firebase UID while connecting any Google account for Sheets.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as Export UI
    participant Callable as Firebase callable SDK
    participant Status as hasGoogleAccountLink
    participant Google as Google consent UI
    participant LinkFn as linkGoogleAccount
    participant OAuth as Google OAuth endpoint
    participant Store as googleOAuthAccounts

    Note over UI,Status: Cache-miss or disconnected-marker path
    UI->>Callable: hasGoogleAccountLink with empty data
    Callable->>Status: Firebase ID token attached by SDK
    Status-->>UI: linked = false
    User->>UI: Connect Google Sheets
    UI->>Google: Sheets and drive.file plus offline access
    Google-->>UI: One-time server authorization code
    UI->>Callable: linkGoogleAccount with authorizationCode
    Callable->>LinkFn: Code plus verified Firebase identity
    LinkFn->>OAuth: Exchange code using server client credentials
    OAuth-->>LinkFn: Refresh token and granted scopes
    LinkFn->>LinkFn: Require both scopes and encrypt token
    LinkFn->>Store: Upsert ciphertext under Firebase UID
    LinkFn-->>UI: linked = true
    UI->>UI: Discard code and continue pending export
```

The backend writes a replacement only after a complete successful exchange. A bad replacement code leaves an existing
valid grant unchanged.

`GOOGLE_RECONNECT_REQUIRED` and `GOOGLE_SCOPES_MISSING` export failures clear the local marker. The next foreground
export therefore returns to the connection flow instead of trusting stale last-known state.

## User export flow

```mermaid
flowchart TD
    Start[User opens Export] --> Expenses{Expenses in selected range?}
    Expenses -->|No| Empty[Show no-expenses state]
    Expenses -->|Yes| Destination{Destination}

    Destination -->|CSV| BuildCsv[Build CSV from local database]
    BuildCsv --> Share[Open platform share sheet]

    Destination -->|Google Sheets| LocalLink{Cached as linked?}
    LocalLink -->|No| LinkCheck[Call server link status]
    LocalLink -->|Yes| Enqueue
    LinkCheck -->|Lookup failed| Error[Show retryable error]
    LinkCheck -->|Unlinked| Consent[Request foreground Google consent]
    Consent -->|Cancelled| Ready[Return to export options]
    Consent -->|Server code| Exchange[Exchange and encrypt on backend]
    Exchange -->|Reconnect reason| ForceConsent[Offer explicit reconnect]
    ForceConsent --> Consent
    Exchange -->|Linked| Enqueue[Enqueue export automatically]
    LinkCheck -->|Linked| Enqueue
    Enqueue --> Started[Show export started]
```

### CSV path

`CSVExportRepository` loads expenses, category names, subcategory names, and decimal precision from the local database,
formats normalized amounts, escapes CSV fields, creates a date-range filename, and invokes the platform `DocumentSharer`.
No link check, notification permission, Firebase session, or network is involved.

### Google Sheets background path

The persisted job contains only:

| Key | Type |
|---|---|
| `exportId` | `Long` |
| `tabLayout` | `String` (`single_tab` or `separate_tabs`) |
| `startDate` | `Long` epoch milliseconds |
| `endDate` | `Long` epoch milliseconds |

Android persists this input in WorkManager with a connected-network constraint, exponential backoff starting at 30
seconds, and at most three token-endpoint attempts. A permanent auth/reconnect result is terminal and does not start
interactive UI. iOS passes the same `ExportWorkRequest` to the shared runner from the application scope; it does not put a
credential in Swift or a durable OS task. Process-termination resilience on iOS therefore remains weaker than WorkManager
and must be evaluated separately if durable iOS background execution becomes a product requirement.

`ExportWorkRunner` reads and formats local data, includes the persisted `exportId`, and calls `exportToSheets`.
Amounts cross the callable boundary as finite numbers rather than strings.

```mermaid
sequenceDiagram
    autonumber
    participant UI as Export UI
    participant Job as Platform scheduler
    participant Runner as ExportWorkRunner
    participant SDK as Firebase callable SDK
    participant Fn as exportToSheets
    participant Store as Encrypted grant store
    participant OAuth as Google OAuth endpoint
    participant Sheets as Sheets and Drive
    participant DB as Local export history

    UI->>Job: IDs, date range, and tab layout only
    Job->>Runner: Run non-secret work request
    Runner->>Runner: Read rows and reuse persisted exportId
    Runner->>SDK: Export data without OAuth credentials
    SDK->>Fn: Payload plus Firebase ID and App Check tokens
    Fn->>Fn: Validate request and enforce per-UID quota
    Fn->>Store: Claim exportId idempotency lease
    Fn->>Store: Load ciphertext by verified UID
    Fn->>Fn: Decrypt refresh token
    Fn->>OAuth: Refresh-token grant
    OAuth-->>Fn: Short-lived access token
    Fn->>Sheets: Create and populate spreadsheet
    Sheets-->>Fn: Spreadsheet URL
    Fn-->>Runner: spreadsheetUrl
    Runner->>DB: Mark success and store URL
```

Payloads larger than 100 KiB are gzip-compressed and base64-encoded. The function rejects input above 5,000 rows or
2 MiB after decompression/JSON encoding. A retry with a completed `exportId` returns the existing URL without creating a
second spreadsheet. Active leases return `EXPORT_IN_PROGRESS`; stale leases can be reclaimed.

## Link state machine

```mermaid
stateDiagram-v2
    [*] --> Unlinked
    Unlinked --> Linking: User grants offline access
    Linking --> Linked: Exchange, scope check, and encrypted write succeed
    Linking --> Unlinked: Cancelled or initial exchange rejected
    Linked --> Exporting: Authenticated export starts
    Exporting --> Linked: Refresh and spreadsheet creation succeed
    Exporting --> ReconnectRequired: Grant missing, invalid, or undecryptable
    Exporting --> Linked: Transient token endpoint failure
    ReconnectRequired --> Linking: Explicit foreground reconnect
    Linked --> Unlinking: Disconnect or privacy cleanup
    ReconnectRequired --> Unlinking: Cleanup requested
    Unlinking --> Unlinked: Stored ciphertext deleted
```

The local marker never advances this server state. Every foreground Sheets export checks the server.

## Error and recovery contract

Clients use callable status and `details.reason`; they never parse provider text.

| Status | Stable reason | Foreground behavior | Background behavior |
|---|---|---|---|
| `UNAUTHENTICATED` | `FIREBASE_SIGN_IN_REQUIRED` | Ask user to sign in | Fail terminally |
| `INVALID_ARGUMENT` | `GOOGLE_AUTH_CODE_MISSING` | Start a new authorization operation | Not applicable |
| `FAILED_PRECONDITION` | `GOOGLE_REFRESH_TOKEN_MISSING` | Explicit consent/reconnect | Not applicable |
| `FAILED_PRECONDITION` | `GOOGLE_RECONNECT_REQUIRED` | Clear cached marker and reconnect | Fail terminally; reconnect on next foreground export |
| `PERMISSION_DENIED` | `GOOGLE_SCOPES_MISSING` | Request both exact scopes | Fail terminally |
| `UNAVAILABLE` | `GOOGLE_TOKEN_ENDPOINT_UNAVAILABLE` | Keep current link and retry later | Retry with bounded backoff |

```mermaid
flowchart TD
    Failure[Callable failure] --> Reason{Stable reason}
    Reason -->|FIREBASE_SIGN_IN_REQUIRED| SignIn[Stop and require foreground sign-in]
    Reason -->|GOOGLE_RECONNECT_REQUIRED| Reconnect[Stop and show reconnect next foreground visit]
    Reason -->|GOOGLE_REFRESH_TOKEN_MISSING| Consent[Request explicit consent and a new code]
    Reason -->|GOOGLE_SCOPES_MISSING| Scopes[Request both required scopes]
    Reason -->|GOOGLE_TOKEN_ENDPOINT_UNAVAILABLE| Context{Execution context}
    Reason -->|SHEETS_TEMPORARILY_UNAVAILABLE| Retry
    Reason -->|EXPORT_IN_PROGRESS| Retry
    Context -->|Foreground| Later[Keep link and retry later]
    Context -->|Android worker| Retry[Exponential retry, maximum three attempts]
    Reason -->|Missing or unknown| Generic[Show or record generic export failure]
```

Human messages are generic. Logs may contain HTTP status and stable reason metadata, but never provider bodies, request
codes, tokens, client secrets, encryption keys, or ciphertext.

## Unlink, sign-out, and account deletion

- Explicit Sheets disconnect calls `unlinkGoogleAccount` while Firebase auth is available.
- Sign-out first attempts remote unlink, then always clears the local Google link marker, Firebase session, and Google
  identity SDK state even if unlink is offline. It preserves the local expense database.
- Account deletion first asks Firebase to delete the current user without reopening provider UI. If Firebase requires a
  recent login, the client reauthenticates with only the provider used to sign in to PlzStop and retries deletion. The
  Auth user-deletion trigger attempts revocation of any decryptable remaining grant and always removes the
  `googleOAuthAccounts/{uid}` record; account deletion does not call the unlink callable from the client.
- Google deletion reauthentication filters to accounts already authorized for PlzStop and enables automatic selection.
  Android still shows the system account chooser when more than one eligible credential exists; Firebase rejects a
  credential belonging to a different user. iOS additionally supplies the current Firebase email as the Google hint.
- Token revocation is best effort; ciphertext deletion is mandatory.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as PlzStop client
    participant Firebase as Firebase Auth
    participant Unlink as unlinkGoogleAccount
    participant Google as Google revoke endpoint
    participant Trigger as Auth deletion cleanup

    alt Sign out
        User->>App: Sign out
        App->>Unlink: Best-effort unlink while authenticated
        Unlink->>Google: Best-effort revoke
        Unlink->>Unlink: Delete ciphertext in finally path
        App->>Firebase: Clear local Firebase session
        App->>App: Clear local marker and Google identity state
    else Delete account
        User->>App: Confirm deletion
        App->>Firebase: Delete Firebase user
        opt Firebase requires a recent login
            Firebase-->>App: Reauthentication required
            App->>Firebase: Reauthenticate with current sign-in provider
            App->>Firebase: Retry Firebase user deletion
        end
        Firebase->>Trigger: User deletion event
        Trigger->>Google: Best-effort revoke remaining grant
        Trigger->>Trigger: Delete token record
        App->>App: Clear user data and identity state; preserve default categories
    end
```

## Backend storage and isolation

```text
googleOAuthAccounts/{firebaseUid}
  encryptedRefreshToken: string
  scopes: string[]
  createdAt: timestamp
  updatedAt: timestamp
```

- `firestore.rules` denies all direct client access to this collection, including own-UID reads/writes.
- Only Admin SDK code reads the record.
- The Fernet key and Web OAuth client credentials are Firebase/Google Secret Manager values.
- Only `encryptedRefreshToken` is supported. Development-only records using another field are deleted before rollout.
- A permanent `invalid_grant`, missing ciphertext, missing required scope, or decryption failure deletes the unusable record.
- A transient OAuth endpoint error leaves the record intact.

## Rollout and operational procedures

### Clean rollout with no migration

There are no production users or grant records to preserve. Before the first supported rollout, delete any
development-only `googleOAuthAccounts` records through an Admin SDK/console operation and reconnect test accounts. Deploy
the backend and supported clients together. Do not add a `refreshToken` fallback, migration script, or compatibility
window; this keeps the storage contract single-purpose from the start.

### Encryption-key rotation

1. Retain the old Secret Manager key version and record its version identifier in the restricted incident/change record.
2. Generate a new Fernet key; never print either key or put it in source control, shell history, logs, or job output.
3. Pause link/export writes or deploy a temporary dual-key version that encrypts with the new key and can decrypt with
   new-then-old.
4. Run a controlled Admin SDK re-encryption job: decrypt each ciphertext with the old key in memory, encrypt with the new
   key, write it back, and report only scanned/succeeded/failed counts.
5. Verify every record is readable with the new key before making the new Secret Manager version authoritative.
6. Keep the old key available but disabled from normal runtime during the rollback window. Roll back by restoring the old
   ciphertext set and secret version, never by mixing keys without version metadata.
7. After the rollback window and backup-retention review, destroy access to the old key according to the security policy.

Key rotation is a future ciphertext re-encryption operation and is independent of the initial clean rollout.

### Key loss or unrecoverable ciphertext

Fernet ciphertext cannot be recovered without the original key. Do not attempt to substitute a new key or expose records
for debugging. Disable Sheets export, delete affected token records through an Admin SDK operation that reports counts
only, restore service with a valid new key, and require users to reconnect. Firebase sign-in and CSV export remain usable.

### Google OAuth consent production readiness

Before production release, verify in Google Cloud Console that the OAuth consent screen is in Production/published status,
the app identity and authorized domains are correct, both required scopes are declared, and any required Google sensitive-
scope verification is complete. Testing-mode external-user grants can expire after seven days, so runtime reconnect logic
is not a substitute for publishing the consent configuration. Record console evidence and verification date in the
release checklist; this repository cannot prove console state.

## Implementation map

| Responsibility | Primary implementation |
|---|---|
| Shared credential contracts | `features/auth/google/GoogleSignInCredential.kt`, `GoogleSheetsAuthorizationCode.kt` |
| Android identity and Sheets authorization | `androidMain/.../auth/GoogleAuthUiProviderImpl.kt` |
| iOS Google/Apple provider bridge | `iosApp/iosApp/SocialAuthBridge.swift` |
| Firebase platform adapters | `AndroidFirebaseAuthProvider.kt`, `FirebaseAuthBridge.swift` |
| Link repository/use cases | `GoogleAccountRepositoryImpl.kt`, `ConnectGoogleAccountUseCase.kt` |
| Export UI/MVI | `features/export/presentation/ExportStateHolder.kt` |
| Platform scheduling | `AndroidExportWorkerScheduler.kt`, `IosExportWorkerScheduler.kt` |
| Token-free shared work | `ExportWorkRequest.kt`, `ExportWorkRunner.kt` |
| Callable error mapping | `FirebaseCallableException.kt` and platform callable adapters |
| Backend OAuth and Sheets operations | `functions-py/main.py` |
| Auth-deletion cleanup | `functions/src/userCleanup.ts` |
| Firestore isolation | `firestore.rules` |

## Verification requirements

Automated checks must cover:

- unauthenticated link/export rejection;
- encryption at rest and required-scope validation;
- missing refresh token and non-destructive failed relink;
- invalid-grant deletion and unlink cleanup;
- Firestore client denial for own UID, other UID, and all writes;
- identity-only Google/Apple Firebase session behavior;
- cancellation cleanup for Android pending authorization and iOS Apple callbacks;
- linked/unlinked/lookup-failed distinction;
- structured foreground reconnect and background retry/terminal decisions;
- exact WorkManager/shared scheduler fields and OAuth-credential-free callable payload;
- App Check client initialization and server enforcement;
- idempotent retry, strict payload bounds, numeric amounts, and formula/text separation;
- local sign-out completion when remote unlink fails;
- `encryptedRefreshToken`-only storage with no legacy-field fallback;
- Mermaid rendering for every diagram in this document.

Runtime release checks must additionally exercise Android and iOS provider UI, first consent, repeat export, forced
reconnect, sign-out offline, background execution, and spreadsheet output. The Google OAuth Production status must be
verified separately in Google Cloud Console.
