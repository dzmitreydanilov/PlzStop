# Google Sheets Export Callable API

Firebase Gen 2 HTTPS callables · Region: `europe-west1` · Runtime: Python 3.12 · Codebase: `py`

This contract covers the server-authorized Google Sheets export flow. Clients never send a Google access token or
refresh token. Firebase callable authentication identifies the PlzStop user, and the backend loads that UID's encrypted
Google grant.

## Common authentication contract

All four callables require both a Firebase Auth session and a verified Firebase App Check token. The callable SDK
automatically attaches both tokens; the client must not add either token to request data. Debug builds use registered
App Check debug tokens. Production Android uses Play Integrity and production Apple builds use App Attest with a
DeviceCheck fallback.

| Callable status | `details.reason` |
|---|---|
| `UNAUTHENTICATED` | `FIREBASE_SIGN_IN_REQUIRED` |

The backend uses only `request.auth.uid` to select `googleOAuthAccounts/{uid}`. Export does not accept an FCM
registration token or send a spreadsheet URL through a notification.

## `linkGoogleAccount`

Exchanges a one-time Google server authorization code for an offline grant and stores the refresh token encrypted.

### Request

| Field | Type | Required | Description |
|---|---|---|---|
| `authorizationCode` | `string` | Yes | One-time code returned after foreground consent; do not retry or persist it |

The authorization request must grant exactly the export capabilities PlzStop uses:

- `https://www.googleapis.com/auth/spreadsheets`
- `https://www.googleapis.com/auth/drive.file`

### Response

```json
{
  "linked": true
}
```

### Link errors

| Status | `details.reason` | Client action |
|---|---|---|
| `INVALID_ARGUMENT` | `GOOGLE_AUTH_CODE_MISSING` | Start a new foreground authorization request |
| `FAILED_PRECONDITION` | `GOOGLE_REFRESH_TOKEN_MISSING` | Reconnect with the explicit-consent option |
| `FAILED_PRECONDITION` | `GOOGLE_RECONNECT_REQUIRED` | Discard the code and start a new reconnect flow |
| `PERMISSION_DENIED` | `GOOGLE_SCOPES_MISSING` | Request both required scopes again |
| `UNAVAILABLE` | `GOOGLE_TOKEN_ENDPOINT_UNAVAILABLE` | Keep the current valid link, if any, and retry later |

A failed replacement exchange does not overwrite an existing valid grant. Provider response bodies and credential values
are not included in logs or callable errors.

## `hasGoogleAccountLink`

Returns authoritative link metadata for the authenticated Firebase UID. A network/callable failure is an unknown state;
clients must not interpret it as unlinked.

### Request

```json
{}
```

### Response

```json
{
  "linked": true
}
```

`linked` is true only when a token record and both required scope values exist. This endpoint does not refresh the Google
token; export performs that validation.

## `unlinkGoogleAccount`

Best-effort revokes the Google refresh token and always deletes the stored ciphertext, including when the token is already
invalid, cannot be decrypted, or Google's revoke endpoint is unavailable.

### Request

```json
{}
```

### Response

```json
{
  "linked": false
}
```

## `exportToSheets`

Creates a spreadsheet in the connected Google account. The function decrypts the backend refresh token, mints a Google
access token in function memory, and discards it after the invocation.

### Request

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `expenses` | `object[]` or `string` | Yes | — | Plain rows or base64-encoded gzip JSON when `compressed` is true |
| `dateRangeLabel` | `string` | No | `"Export"` | Default workbook and single-tab label |
| `exportId` | `string` | Yes | — | Idempotency and recovery key; 1–128 ASCII letters, digits, `_`, or `-` |
| `title` | `string` | No | generated | Custom spreadsheet name |
| `month` | `string` | No | `"Export"` | Optional fallback label when `dateRangeLabel` is absent |
| `tabLayout` | `string` | No | `"single_tab"` | `"single_tab"` or `"separate_tabs"` |
| `currencySymbol` | `string` | No | `""` | Text shown in the Amount column header |
| `decimalPlaces` | `int` | No | `2` | Number-format precision from 0 through 10 |
| `compressed` | `bool` | No | `false` | Whether `expenses` is gzip/base64 JSON |

Forbidden request fields include `googleAccessToken`, `accessToken`, `refreshToken`, `idToken`, `authorizationCode`, and
`nonce`. Unknown fields are ignored and are never consumed as credentials.

### Expense row

| Field | Type | Required | Description |
|---|---|---|---|
| `date` | `string` | Yes | ISO date, for example `"2026-06-15"` |
| `title` | `string` | Yes | Expense description |
| `category` | `string` | No | Category name used by summaries |
| `subcategory` | `string` | No | Subcategory name used by summaries |
| `amount` | `number` | Yes | Finite normalized-currency amount; never interpreted as a formula |
| `notes` | `string` | No | Free text |

### Response

```json
{
  "spreadsheetUrl": "https://docs.google.com/spreadsheets/d/..."
}
```

### Export errors

| Status | `details.reason` | Meaning and client action |
|---|---|---|
| `UNAUTHENTICATED` | `FIREBASE_SIGN_IN_REQUIRED` | Terminal background failure; sign in in foreground |
| `FAILED_PRECONDITION` | `GOOGLE_RECONNECT_REQUIRED` | Stored grant is absent, invalid, or undecryptable; reconnect in foreground |
| `PERMISSION_DENIED` | `GOOGLE_SCOPES_MISSING` | Stored grant metadata lacks a required scope; reconnect in foreground |
| `UNAVAILABLE` | `GOOGLE_TOKEN_ENDPOINT_UNAVAILABLE` | Retry with bounded backoff; do not open provider UI from a worker |
| `UNAVAILABLE` | `SHEETS_TEMPORARILY_UNAVAILABLE` | Sheets returned a retryable 429/5xx after bounded server retries |
| `ALREADY_EXISTS` | `EXPORT_IN_PROGRESS` | The same export is still running; retry with the same `exportId` |
| `INVALID_ARGUMENT` | `EXPORT_ID_REQUIRED` | Generate and persist an export ID before scheduling work |
| `INVALID_ARGUMENT` | absent | Expense data is missing or compressed data is invalid |
| `INTERNAL` | absent | Spreadsheet creation or write failed |

Permanent refresh-token rejection deletes the unusable record before returning `GOOGLE_RECONNECT_REQUIRED`. A transient
token-endpoint failure leaves the record unchanged.

## Spreadsheet output

### Columns

| A | B | C | D | E | F |
|---|---|---|---|---|---|
| Date | Title | Category | Subcategory | Amount (symbol) | Notes |

- `single_tab`: all expenses are sorted by date on one sheet; multi-month ranges receive a month-by-month category pivot.
- `separate_tabs`: each month receives its own sheet and category/subcategory summary.
- Header, total, date, amount, summary, freeze, and column-width formatting are applied by batch update.

## Side effects

1. The function atomically claims `exports/{uid}/history/{exportId}` before creating a spreadsheet.
2. A completed retry returns the stored URL; an active lease returns `EXPORT_IN_PROGRESS`; a stale lease can be reclaimed.
3. A spreadsheet is created in the Google Drive represented by the backend refresh token.
4. The completed URL is stored server-side and returned to the authenticated caller.
5. The client worker independently records the result in the local `export_history` table.

## Storage and security

The server-only record is:

```text
googleOAuthAccounts/{firebaseUid}
  encryptedRefreshToken: string
  scopes: string[]
  createdAt: timestamp
  updatedAt: timestamp
```

- Firestore rules deny all client reads and writes to this collection.
- The clean rollout reads and writes only `encryptedRefreshToken`; no legacy field fallback or migration is shipped.
- OAuth client credentials and the Fernet encryption key are Secret Manager secrets.
- Spreadsheet cells distinguish trusted server formulas from client text. Numeric amounts must be finite numbers.
- Input is capped at 5,000 rows, 500 characters per cell text value, and 2 MiB after decompression/JSON encoding.
- Per-UID daily quotas and explicit function concurrency/instance limits protect OAuth and Sheets capacity.
- Provider bodies, codes, tokens, client secrets, encryption material, and ciphertext values must never be logged.

See [Export feature technical specification](../features/export.md) for platform acquisition timing, lifecycle diagrams,
background execution, reconnect behavior, and operations.
