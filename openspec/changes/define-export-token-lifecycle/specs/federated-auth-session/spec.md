## ADDED Requirements

### Requirement: Provider authentication is user initiated
The system SHALL request a Google or Apple provider credential only after an explicit user sign-in or reauthentication
action and SHALL NOT start a provider flow merely to restore an existing app session.

#### Scenario: Existing Firebase session is restored
- **WHEN** the app starts with a restorable Firebase session
- **THEN** the Firebase SDK restores or refreshes that session without opening Google or Apple UI

#### Scenario: User starts Google sign-in
- **WHEN** the user taps the Google sign-in control
- **THEN** the client requests a fresh Google identity credential

#### Scenario: User starts Apple sign-in on iOS
- **WHEN** the user taps the Apple sign-in control on iOS
- **THEN** the client starts a fresh AuthenticationServices request

### Requirement: Google sign-in exchanges an identity token with Firebase
The system SHALL use the Google ID token only to create a Firebase Google credential and SHALL keep basic Google sign-in
free of Google Sheets and Drive authorization scopes.

#### Scenario: Google sign-in succeeds
- **WHEN** Google returns a valid ID token for a basic sign-in request
- **THEN** the platform Firebase Auth adapter exchanges that ID token and establishes a Firebase user session

#### Scenario: Google SDK returns additional provider values
- **WHEN** the Google platform SDK also returns an access token or server authorization code during basic sign-in
- **THEN** the client discards those additional values and does not use them for export

#### Scenario: Firebase rejects a Google ID token
- **WHEN** Firebase rejects the Google credential
- **THEN** the client clears the provider result and leaves the user unauthenticated

### Requirement: Apple sign-in binds the identity token to a nonce
The iOS client SHALL generate a cryptographically random raw nonce for every Apple sign-in request, send its SHA-256 hash
to Apple, and pass the returned Apple identity token with the original raw nonce to Firebase Authentication.

#### Scenario: Apple sign-in succeeds
- **WHEN** Apple returns an identity token for the request containing the hashed nonce
- **THEN** the client submits the identity token and raw nonce to Firebase and establishes a Firebase user session

#### Scenario: Apple sign-in is cancelled
- **WHEN** the user cancels Apple sign-in
- **THEN** the client clears the raw nonce and creates no Firebase credential

#### Scenario: Apple callback has no identity token
- **WHEN** AuthenticationServices completes without a usable identity token
- **THEN** the client clears the raw nonce, reports an authentication failure, and creates no Firebase session

### Requirement: Firebase SDK owns the app session
The system SHALL delegate Firebase ID-token and refresh-token persistence and renewal to the platform Firebase Auth SDK and
SHALL NOT copy those tokens into shared models, app preferences, databases, worker input, or custom bearer-token storage.

#### Scenario: Callable is invoked by a signed-in user
- **WHEN** a signed-in client invokes a Firebase callable through the platform SDK
- **THEN** the SDK attaches a current Firebase ID token and the callable runtime exposes the verified Firebase UID

#### Scenario: Firebase ID token has expired
- **WHEN** a callable is invoked after the current Firebase ID token's lifetime
- **THEN** the Firebase SDK refreshes the ID token using its SDK-managed session before or as part of the call

#### Scenario: Firebase session cannot be refreshed
- **WHEN** the Firebase session is absent, disabled, revoked, or otherwise cannot be refreshed
- **THEN** the protected callable fails as unauthenticated and no provider UI is started from background work

### Requirement: App identity is independent from Sheets authorization
The system SHALL preserve the current Firebase UID while a signed-in user connects a Google account for Sheets export and
SHALL NOT treat the Google credential from that connection as a new PlzStop sign-in.

#### Scenario: Apple-authenticated user connects Google Sheets
- **WHEN** an Apple-authenticated user completes Google Sheets authorization
- **THEN** the Google authorization is associated with the existing Apple user's Firebase UID

#### Scenario: Google-authenticated user has not connected Sheets
- **WHEN** a user is signed in to PlzStop with Google but has never granted the export scopes
- **THEN** the user remains authenticated but Google Sheets export reports that a separate connection is required

### Requirement: Provider credentials are transient and redacted
The system MUST keep Google ID tokens, Google client access tokens, Google server authorization codes, Apple identity
tokens, and Apple nonces in memory only for the operation that acquired them and MUST redact them from all telemetry.

#### Scenario: Provider credential operation completes
- **WHEN** provider-to-Firebase exchange succeeds, fails, or is cancelled
- **THEN** all provider credentials and nonce values from that operation become unreachable from UI and domain state

#### Scenario: Authentication failure is recorded
- **WHEN** the app records a log, analytics event, crash report, or error message for an auth failure
- **THEN** the record contains only a stable reason and contains no credential value or provider response body

### Requirement: Reauthentication uses a fresh provider credential
The system SHALL obtain a new Google ID token or Apple identity-token-and-nonce pair for an operation that Firebase marks
as requiring recent authentication and SHALL NOT replay a credential captured during an earlier sign-in.

#### Scenario: Account deletion requires recent Google authentication
- **WHEN** Firebase rejects Google-authenticated account deletion because the session is stale
- **THEN** the foreground client requests a fresh Google identity credential and calls Firebase reauthentication

#### Scenario: Account deletion requires recent Apple authentication
- **WHEN** Firebase rejects Apple-authenticated account deletion because the session is stale
- **THEN** the iOS client performs a new nonce-bound Apple request and calls Firebase reauthentication

### Requirement: Sign-out clears local identity sessions
The system SHALL clear the Firebase Auth session and the applicable Google identity credential state after sign-out local
cleanup begins, even if remote Google Sheets grant cleanup cannot complete.

#### Scenario: Remote Sheets unlink succeeds during sign-out
- **WHEN** a signed-in user signs out and the remote unlink completes
- **THEN** the client clears Firebase and Google identity SDK state

#### Scenario: Remote Sheets unlink is unavailable during sign-out
- **WHEN** a signed-in user signs out while the unlink callable cannot be reached
- **THEN** the client still clears local Firebase and Google identity SDK state and does not trap the user in the session
