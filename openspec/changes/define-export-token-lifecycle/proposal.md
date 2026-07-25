## Why

The export implementation currently mixes two token designs: the Cloud Function already uses a server-held Google
refresh token, while UI models and the published API contract still describe passing a Google access token from the
client. A single, explicit token lifecycle is needed so Google and Apple sign-in remain identity-only and background
Sheets exports are secure, resumable, and testable.

## What Changes

- Define separate credential domains for Google identity, Apple identity, Firebase sessions, and Google Sheets OAuth.
- Keep Google and Apple sign-in credentials transient and exchange them with Firebase Authentication on the client.
- Let Firebase SDKs own Firebase ID-token refresh and automatically authenticate callable requests.
- Request Google Sheets consent only when the user connects Google Sheets, then send the one-time server authorization
  code to an authenticated callable.
- Store only the Google refresh token on the backend, encrypted at rest and scoped to the authenticated Firebase UID.
- Mint a short-lived Google access token inside `exportToSheets` for each export; never put Google tokens in UI state,
  local storage, worker input, export history, logs, analytics, or the export payload.
- Define reconnect, unlink, sign-out, account-deletion, cancellation, expiry, and retry behavior for every token type.
- Remove the stale client-access-token contract and align code, tests, and export documentation with the server-side
  refresh-token design.

## Capabilities

### New Capabilities

- `federated-auth-session`: Google and Apple sign-in credential exchange, Firebase session ownership, and transient-token
  handling rules.
- `google-sheets-export-authorization`: Incremental Google Sheets authorization, backend refresh-token custody, export-time
  access-token minting, and reconnect/revocation behavior.

### Modified Capabilities

None. The repository has no existing OpenSpec capability specifications.

## Impact

- Android Credential Manager, Google Identity Services AuthorizationClient, and Firebase Auth integration.
- iOS Google Sign-In, AuthenticationServices, Firebase Auth, and Swift/Kotlin bridges.
- Shared auth/export models, StateHolders, repositories, background schedulers, and worker payloads.
- Python callable functions `linkGoogleAccount`, `hasGoogleAccountLink`, `unlinkGoogleAccount`, and `exportToSheets`.
- Firestore `googleOAuthAccounts/{uid}`, Firebase Secret Manager configuration, security logging, and token cleanup.
- Export API documentation and automated tests for token absence, ownership, refresh, revocation, and reconnect flows.
