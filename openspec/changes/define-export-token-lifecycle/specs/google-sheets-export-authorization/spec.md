## ADDED Requirements

### Requirement: Server link state is authoritative
The system SHALL determine Google Sheets connection state through an authenticated server lookup keyed by the verified
Firebase UID and SHALL NOT use a local boolean as proof of a usable Google OAuth grant.

#### Scenario: Server has a link record
- **WHEN** `hasGoogleAccountLink` finds a valid record for the authenticated Firebase UID
- **THEN** it returns `linked = true` without returning any token or ciphertext

#### Scenario: Server has no link record
- **WHEN** `hasGoogleAccountLink` finds no record for the authenticated Firebase UID
- **THEN** it returns `linked = false`

#### Scenario: Link lookup fails
- **WHEN** the authenticated link lookup fails because of network or server unavailability
- **THEN** the client reports an error or unknown state and does not reinterpret the failure as an unlinked account

### Requirement: Sheets authorization is incremental and foreground-only
The system SHALL request Google Sheets authorization only when a signed-in user chooses Google Sheets export and the server
does not have a usable link, and the request SHALL ask for offline access with only `spreadsheets` and `drive.file`.

#### Scenario: First Sheets export has no link
- **WHEN** a foreground user chooses Google Sheets and the server reports `linked = false`
- **THEN** the client presents Google consent for the two required scopes and offline access

#### Scenario: Server link already exists
- **WHEN** a foreground user chooses Google Sheets and the server reports `linked = true`
- **THEN** the client enqueues the export without opening Google authorization UI

#### Scenario: Background worker discovers that reconnect is required
- **WHEN** a background export receives `GOOGLE_RECONNECT_REQUIRED`
- **THEN** it records a terminal reconnect-required result and does not try to present provider UI

### Requirement: One-time authorization code is exchanged by an authenticated callable
The system SHALL send the Google server authorization code to `linkGoogleAccount` through the Firebase callable SDK, and
the callable SHALL exchange it using the Web/server OAuth client credentials only after verifying Firebase authentication.

#### Scenario: Valid code is exchanged
- **WHEN** an authenticated user supplies a fresh authorization code whose grant includes both required scopes
- **THEN** the callable exchanges it once, stores the encrypted refresh token under that Firebase UID, and returns linked

#### Scenario: Link call is unauthenticated
- **WHEN** `linkGoogleAccount` has no valid Firebase authentication context
- **THEN** it rejects the call as `UNAUTHENTICATED` without contacting the Google token endpoint

#### Scenario: Authorization code is missing
- **WHEN** an authenticated link request omits the authorization code
- **THEN** the callable rejects it as `INVALID_ARGUMENT` with reason `GOOGLE_AUTH_CODE_MISSING`

#### Scenario: Exchange omits a refresh token
- **WHEN** Google accepts the code but does not return a refresh token
- **THEN** the callable stores no new grant and returns `FAILED_PRECONDITION` with reason `GOOGLE_REFRESH_TOKEN_MISSING`

#### Scenario: Exchange omits a required scope
- **WHEN** the token exchange does not confirm both required scopes
- **THEN** the callable stores no new grant and returns `PERMISSION_DENIED` with reason `GOOGLE_SCOPES_MISSING`

#### Scenario: Authorization code is replayed
- **WHEN** a client retries a server authorization code that has already been exchanged or rejected
- **THEN** the callable does not create a link and instructs the client to restart foreground authorization

### Requirement: Google refresh token has backend-only custody
The system MUST encrypt a Google refresh token before writing it to Firestore, store it only under the verified Firebase
UID, and prevent direct client reads or writes to the token collection.

#### Scenario: Refresh token is persisted
- **WHEN** a link exchange returns a valid refresh token and required scopes
- **THEN** Firestore contains ciphertext, scope metadata, and timestamps but no plaintext Google token

#### Scenario: Client attempts to read token storage
- **WHEN** an Android or iOS client directly reads `googleOAuthAccounts`
- **THEN** Firestore Security Rules deny the request

#### Scenario: Replacement link fails
- **WHEN** a UID already has a usable link and a new authorization-code exchange fails
- **THEN** the callable leaves the previous encrypted refresh token unchanged

#### Scenario: Encryption key is unavailable
- **WHEN** a callable cannot encrypt or decrypt a stored refresh token
- **THEN** it does not expose the token, returns a non-secret failure, and requires operational recovery or user reconnect

### Requirement: Export jobs and requests contain no OAuth credential
The system SHALL persist and send only export data and non-secret configuration from the client; Google and Apple tokens,
authorization codes, nonces, and Firebase tokens SHALL NOT be fields in worker input or `exportToSheets` request data.

#### Scenario: Android export survives process death
- **WHEN** WorkManager persists a Google Sheets export
- **THEN** its input contains export identifiers and configuration but no credential or token field

#### Scenario: iOS export is scheduled
- **WHEN** the iOS scheduler starts a Google Sheets export
- **THEN** it passes export identifiers and configuration but no credential or token to shared work

#### Scenario: Export callable is invoked
- **WHEN** the worker calls `exportToSheets`
- **THEN** Firebase authentication is carried by the callable protocol and `googleAccessToken` is absent from request data

### Requirement: Every Sheets export mints access inside the callable
The `exportToSheets` callable SHALL authenticate the Firebase caller, load that UID's encrypted Google refresh token, and
mint a short-lived Google access token inside the invocation before calling Google Sheets or Drive.

#### Scenario: Linked user exports successfully
- **WHEN** an authenticated user has a decryptable valid refresh token
- **THEN** the callable refreshes an access token in memory, creates the spreadsheet, and returns its URL

#### Scenario: Firebase authentication is absent
- **WHEN** `exportToSheets` is invoked without a verified Firebase user
- **THEN** it returns `UNAUTHENTICATED` before reading token storage or calling Google

#### Scenario: UID has no Google link
- **WHEN** an authenticated Firebase UID has no backend Google token record
- **THEN** the callable returns `FAILED_PRECONDITION` with reason `GOOGLE_RECONNECT_REQUIRED`

#### Scenario: Google refresh token is invalid
- **WHEN** Google rejects the refresh token with `invalid_grant` or equivalent permanent invalidation
- **THEN** the callable deletes the unusable record and returns reason `GOOGLE_RECONNECT_REQUIRED`

#### Scenario: Google access token is created
- **WHEN** the OAuth refresh succeeds
- **THEN** the access token remains in function memory only and is discarded when the invocation ends

### Requirement: Reconnect obtains a new offline grant
The system SHALL recover from a missing or invalid Google refresh token only through an explicit foreground reconnect that
can obtain a new one-time authorization code and refresh token.

#### Scenario: User responds to reconnect-required state
- **WHEN** the foreground UI observes `GOOGLE_RECONNECT_REQUIRED`
- **THEN** it clears any cached linked marker and offers a Google reconnect action

#### Scenario: Reconnect requires renewed consent
- **WHEN** Google would otherwise issue an authorization response without a new refresh token
- **THEN** the platform authorization adapter requests explicit consent using the provider-supported reconnect flow

#### Scenario: Reconnect succeeds
- **WHEN** the user grants the scopes and the server stores a new encrypted refresh token
- **THEN** a later export runs without carrying a client access token

### Requirement: Disconnect and account cleanup revoke backend access
The system SHALL attempt to revoke the Google refresh token and SHALL delete its stored ciphertext on explicit disconnect,
PlzStop sign-out, and account deletion cleanup.

#### Scenario: User disconnects Google Sheets
- **WHEN** an authenticated user selects Disconnect Google Sheets
- **THEN** the callable attempts Google revocation, deletes the UID's token record, and returns `linked = false`

#### Scenario: Google says the token is already invalid
- **WHEN** Google revocation reports an already invalid or absent grant
- **THEN** the callable still deletes the stored ciphertext and reports the account unlinked

#### Scenario: User deletes the PlzStop account
- **WHEN** account deletion completes or its server cleanup trigger runs
- **THEN** no Google refresh-token record remains for that Firebase UID

#### Scenario: Sign-out cannot reach unlink
- **WHEN** the client cannot complete remote unlink during sign-out
- **THEN** local sign-out still completes and server-side or subsequent-session cleanup remains responsible for the record

### Requirement: Export errors are structured and non-secret
The system SHALL expose a stable recovery reason in callable error details and SHALL NOT include tokens, authorization
codes, provider response bodies, client secrets, or encryption material in client-visible messages or telemetry.

#### Scenario: Client maps a reconnect error
- **WHEN** a callable returns `FAILED_PRECONDITION` with reason `GOOGLE_RECONNECT_REQUIRED`
- **THEN** the client maps the structured reason to reconnect UX without parsing a provider message

#### Scenario: Google token endpoint fails
- **WHEN** the Google token endpoint returns an error
- **THEN** logs contain only status and stable reason metadata and contain no request or response credential value

### Requirement: Export has no client-supplied notification channel
The system SHALL NOT accept an FCM registration token or send a spreadsheet URL through notification data and SHALL
return the result only through the authenticated callable response and server idempotency record.

#### Scenario: Export completes
- **WHEN** a spreadsheet export succeeds
- **THEN** the callable stores the URL under the verified Firebase UID and export ID and returns it to the caller

### Requirement: Callables require app attestation
The system SHALL require a valid Firebase App Check token in addition to Firebase authentication for every export and
Google-account callable.

#### Scenario: Authenticated unofficial client calls export
- **WHEN** a valid Firebase user calls without valid App Check attestation
- **THEN** the callable rejects the request before reading OAuth state or calling Google

### Requirement: Export creation is idempotent
The system SHALL use the client export ID as a required idempotency key and SHALL create at most one spreadsheet for a
successfully completed key.

#### Scenario: Successful response is lost
- **WHEN** the client retries a completed export with the same export ID
- **THEN** the callable returns the stored spreadsheet URL without creating another spreadsheet
