# Deploying Firebase Cloud Functions

## Prerequisites

1. **Node.js 22** — matches the runtime in `functions/package.json`
2. **Python 3.12+** — for Python functions in `functions-py/`
3. **Firebase CLI** — install globally:
   ```bash
   npm install -g firebase-tools
   ```
4. **Authenticated session**:
   ```bash
   firebase login
   ```
5. **Correct project selected** (project ID: `pleasest-e3424`):
   ```bash
   firebase use pleasest-e3424
   ```
   Or check current project:
   ```bash
   firebase projects:list
   ```

## Project Structure

```
PlzStop/
├── firebase.json            # Declares both codebases (ts + py)
├── functions/               # TypeScript codebase ("ts")
│   ├── package.json         # Dependencies, Node 22 engine, build/deploy scripts
│   ├── tsconfig.json        # TypeScript config, outputs to lib/
│   ├── src/
│   │   ├── index.ts         # analyzeReceipt, analyzeReceiptGemini, rateLimitCleanup
│   │   ├── receipt.ts       # Shared logic (prompt, types, request prep, response parsing)
│   │   ├── rateLimit.ts     # Firestore-backed rate limiting + global daily cap
│   │   └── validation.ts    # Input validation and sanitization
│   └── lib/                 # Compiled JS output (gitignored)
├── functions-py/            # Python codebase ("py")
│   ├── main.py              # exportToSheets
│   └── requirements.txt     # Python dependencies
```

## Deploy Steps

### Deploy all functions (both codebases)

```bash
# From project root
firebase deploy --only functions
```

### Deploy only TypeScript functions

```bash
cd functions && npm install && npm run build && cd .. && firebase deploy --only functions:ts
```

### Deploy only Python functions

```bash
firebase deploy --only functions:py
```

### Deploy a single function by name

```bash
firebase deploy --only functions:ts:analyzeReceipt
firebase deploy --only functions:py:exportToSheets
```

## Adding a New Function

### TypeScript

1. Export a new function from `functions/src/index.ts`:
   ```typescript
   export const myNewFunction = onCall(
     { region: "europe-west1" },
     async (req) => { /* ... */ }
   );
   ```
2. Build and deploy:
   ```bash
   cd functions && npm run build && cd .. && firebase deploy --only functions:ts:myNewFunction
   ```

### Python

1. Add a new decorated function in `functions-py/main.py`:
   ```python
   @https_fn.on_call(region="europe-west1")
   def myNewFunction(req: https_fn.CallableRequest):
       # ...
   ```
   Firebase auto-discovers all decorated functions in `main.py`.

2. Add any new dependencies to `functions-py/requirements.txt`.

3. Deploy:
   ```bash
   firebase deploy --only functions:py:myNewFunction
   ```

## Architecture

### Receipt Analysis Flow

```
┌─────────────┐
│  Mobile App  │
│              │
│ Takes photo  │
│ of receipt   │
└──────┬───────┘
       │
       │  imageBase64 + categories[]
       │
       ▼
┌──────────────────────────────────────────────────┐
│  Firebase Cloud Function                         │
│  (analyzeReceipt OR analyzeReceiptGemini)        │
│                                                  │
│  1. Rate Limit Check (Firestore)                 │
│     ├─ Global daily cap (Gemini API only, 500/d) │
│     ├─ Per-UID burst (10/min)                    │
│     └─ Per-UID daily (3/day)                     │
│                                                  │
│  2. Input Validation                             │
│     ├─ Base64 format + size ≤ 5MB                │
│     ├─ Category/subcategory ID types             │
│     └─ Name sanitization                         │
│                                                  │
│  3. LLM Call                                     │
│     ├─ Sends receipt image + categories list     │
│     └─ Model reads image (built-in OCR)          │
│                                                  │
│  4. Response Parsing + Normalization             │
│     └─ Validates types, sanitizes output         │
└──────────────────────┬───────────────────────────┘
                       │
                       │  Structured JSON
                       ▼
                ┌──────────────┐
                │  Mobile App  │
                │              │
                │ Shows items  │
                │ for review   │
                └──────────────┘
```

### What the model does with the receipt image

```
┌─────────────────────────────────┐
│  Receipt Image                  │
│                                 │
│  LIDL                           │
│  2026-04-05                     │
│                                 │
│  Oat milk          2.49         │
│  Lactose free milk 3.19         │
│  Sourdough bread   1.89         │
│  Craft beer        4.50         │
│  Lager beer        3.80         │
│                                 │
│  TOTAL            15.87         │
└────────────────┬────────────────┘
                 │
                 │  Gemini reads image
                 │  (multimodal OCR)
                 ▼
┌─────────────────────────────────┐
│  Model Processing               │
│                                 │
│  1. Extract merchant → "Lidl"   │
│  2. Extract date → "2026-04-05" │
│  3. Extract total → 15.87       │
│  4. Read all line items         │
│  5. Group similar items:        │
│     Oat milk + Lactose free     │
│       milk → "Milk" (5.68)      │
│     Sourdough bread             │
│       → "Bread" (1.89)          │
│     Craft beer + Lager beer     │
│       → "Beer" (8.30)           │
│  6. Match each group to         │
│     provided categories list    │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│  JSON Response                  │
│                                 │
│  merchantName: "Lidl"           │
│  totalAmount: 15.87             │
│  currency: "EUR"                │
│  date: "2026-04-05"            │
│  items:                         │
│    Milk   → 5.68  (cat: Food)  │
│    Bread  → 1.89  (cat: Food)  │
│    Beer   → 8.30  (cat: Drink) │
└─────────────────────────────────┘
```

### Two backends, same contract

```
                    ┌─────────────┐
                    │  Mobile App │
                    └──────┬──────┘
                           │
                    Which function?
                    (app chooses)
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
┌──────────────────────┐  ┌──────────────────────┐
│  analyzeReceipt      │  │  analyzeReceiptGemini │
│                      │  │                       │
│  Vertex AI           │  │  Gemini API           │
│  GCP billing         │  │  Free: 500 req/day    │
│  No global cap       │  │  Global daily cap     │
│  ~$0.0004/scan       │  │  $0 within free tier  │
│                      │  │                       │
│  Same request  ◄─────┼──┼─► Same request        │
│  Same response ◄─────┼──┼─► Same response       │
└──────────────────────┘  └───────────────────────┘
              │                         │
              └────────────┬────────────┘
                           │
                    Shared modules:
                    receipt.ts (prompt, parsing)
                    rateLimit.ts (Firestore)
                    validation.ts (input checks)
```

### Scheduled cleanup

```
┌──────────────────────────────┐
│  rateLimitCleanup            │
│  Runs every 24 hours         │
│                              │
│  Firestore: rateLimits/*     │
│  ┌────┐ ┌────┐ ┌────┐       │
│  │ IP │ │ IP │ │ IP │ ...   │
│  │hash│ │hash│ │hash│       │
│  └──┬─┘ └──┬─┘ └──┬─┘       │
│     │      │      │         │
│  expired? ──► DELETE         │
│  active?  ──► KEEP          │
└──────────────────────────────┘
```

## Functions

### `analyzeReceipt` (Vertex AI)

Callable function that extracts structured data from receipt images using Gemini 2.5 Flash via **Vertex AI** (GCP billing, pay-per-use).

| Property | Value |
|---|---|
| Type | Callable (`onCall`) |
| Region | `europe-west1` |
| Runtime | Node.js 22 |
| Memory | 512 MiB |
| Timeout | 120 seconds |
| Max instances | 10 |
| App Check | Enforced |
| Backend | Vertex AI (`@google-cloud/vertexai`) |
| Billing | GCP pay-per-use |

### `analyzeReceiptGemini` (Gemini API)

Same functionality as `analyzeReceipt` but uses the **Gemini API** (free tier: 500 requests/day).

| Property | Value |
|---|---|
| Type | Callable (`onCall`) |
| Region | `europe-west1` |
| Runtime | Node.js 22 |
| Memory | 512 MiB |
| Timeout | 120 seconds |
| Max instances | 10 |
| App Check | Enforced |
| Backend | Gemini API (`@google/generative-ai`) |
| Billing | Free tier (500 req/day), then pay-per-use |
| Secret | `GEMINI_API_KEY` (Firebase secret) |

**Setup:** The API key must be set before deploying:
```bash
firebase functions:secrets:set GEMINI_API_KEY
```
Get the key from [Google AI Studio](https://aistudio.google.com/apikey).

### Shared Request/Response (both functions)

**Request payload:**

| Field | Type | Required | Notes |
|---|---|---|---|
| `imageBase64` | `string` | Yes | Base64-encoded JPEG, max 5 MB decoded |
| `categories` | `Category[]` | Yes | 1–50 items, each with integer `id` and `name` |
| `subcategories` | `Subcategory[]` | No | 0–200 items, each with integer `id`, `parentCategoryId`, and `name` |

**Response payload:**

```json
{
  "status": "success | partial | not_receipt | unreadable | error",
  "data": {
    "merchantName": "string | null",
    "totalAmount": 42.99,
    "currency": "EUR",
    "date": "2026-04-05",
    "items": [
      {
        "name": "Milk",
        "amount": 5.68,
        "categoryId": 1,
        "subcategoryId": 3
      }
    ]
  },
  "message": "string | null"
}
```

- `items` groups similar receipt line items (e.g. "Oat milk" + "Lactose free milk" → "Milk" with summed amount)
- Each item has its own `categoryId`/`subcategoryId` assigned from the provided categories list
- `items` can be empty if no individual items are readable
- `data` is `null` for `not_receipt`, `unreadable`, and `error` statuses

### `exportToSheets` (Python)

Creates an idempotent Google Spreadsheet export from strictly validated expense data.

| Property | Value |
|---|---|
| Type | Callable (`onCall`) |
| Region | `europe-west1` |
| Runtime | Python 3.13 |
| Memory | 512 MiB |
| Timeout | 300 seconds |
| Max instances | 5 |
| Concurrency | 4 requests per instance |
| App Check | Enforced |
| Codebase | `py` (`functions-py/main.py`) |

Uses a server-side Google OAuth refresh token to create and share a spreadsheet via the Google Sheets API. Refresh
tokens are encrypted before being stored in the private `googleOAuthAccounts` Firestore collection.

Set the required secrets before deploying the Python functions:

```bash
npx -y firebase-tools@latest functions:secrets:set GOOGLE_OAUTH_CLIENT_ID
npx -y firebase-tools@latest functions:secrets:set GOOGLE_OAUTH_CLIENT_SECRET
npx -y firebase-tools@latest functions:secrets:set GOOGLE_TOKEN_ENCRYPTION_KEY
```

`GOOGLE_TOKEN_ENCRYPTION_KEY` must be a Fernet-compatible, URL-safe base64-encoded 32-byte key. The OAuth client ID
must be the Web application client configured as the Android server client ID and as `GIDServerClientID` on iOS.

The encryption key is not directly replaceable while ciphertext exists. Retain the old Secret Manager version during a
controlled decrypt-and-re-encrypt operation, verify aggregate success counts with the new key, and keep the old version
through the rollback window. If the old key is lost, delete the affected token records and require reconnect; ciphertext
cannot be recovered. The complete clean-rollout, rotation, key-loss, and OAuth consent procedures are in
[Export Feature Technical Specification](../features/export.md#rollout-and-operational-procedures).

### `rateLimitCleanup`

Scheduled function that removes expired rate limit documents from Firestore.

| Property | Value |
|---|---|
| Type | Scheduled (`onSchedule`) |
| Schedule | Every 24 hours |
| Region | `europe-west1` |

## Input Validation

Performed in `validation.ts` before any LLM call:

- **imageBase64**: must be a valid base64 string (`[A-Za-z0-9+/]+=*`, length divisible by 4), decoded size ≤ 5 MB
- **Category IDs**: must be integers (`typeof === "number"` and `Number.isInteger()`)
- **Subcategory IDs and parentCategoryId**: must be integers
- **Names**: control characters stripped, trimmed, max 100 chars
- **Array sizes**: max 50 categories, max 200 subcategories

## Rate Limiting

Implemented in `rateLimit.ts` using Firestore (collection: `rateLimits`).

### Per-user limits (both functions)

| Limit | Value | Scope |
|---|---|---|
| Burst | 10 requests/minute | Per Firebase UID |
| Daily | 3 requests/day | Per Firebase UID |

- Document ID is a SHA-256 hash derived from the verified Firebase UID
- Uses Firestore transactions to avoid race conditions across function instances
- Works correctly with `maxInstances: 10` (shared state, not in-memory)
- Expired documents cleaned up daily by `rateLimitCleanup`

### Global daily limit (`analyzeReceiptGemini` only)

| Limit | Value | Scope |
|---|---|---|
| Daily | 500 requests/day | All users combined |

- Tracks total daily calls in a single Firestore document (`rateLimits/globalDailyCounter`)
- Matches the Gemini API free tier (500 req/day) to prevent billing overages
- Resets at midnight UTC
- Checked only after App Check, Firebase authentication, input validation, and per-UID limits
- Does **not** apply to `analyzeReceipt` (Vertex AI) — that function has no global cap

### Check order for `analyzeReceiptGemini`

1. App Check enforcement
2. Firebase authentication
3. Input validation
4. Per-UID burst/daily limit
5. Global daily limit (500/day total)
6. LLM call

## Error Handling

- Internal errors and quota errors are logged server-side via `console.error` and return generic messages to the client
- No internal details (stack traces, API error strings) are exposed in responses

## Verifying Deployment

After deploy, verify in the Firebase Console:
```
https://console.firebase.google.com/project/pleasest-e3424/functions
```

Or via CLI:
```bash
firebase functions:list
```

## Local Testing with Emulator

```bash
cd functions
npm run serve
```

This builds and starts the Firebase emulator for functions only.

## Troubleshooting

### "Permission denied" or auth errors
```bash
firebase login --reauth
```

### Build errors
Check TypeScript compilation:
```bash
cd functions && npx tsc --noEmit
```

### Viewing function logs
```bash
firebase functions:log --only analyzeReceipt
```

Or with tail:
```bash
firebase functions:log --only analyzeReceipt --follow
```

### Shutting down all functions

Delete all deployed functions:
```bash
firebase functions:delete analyzeReceipt analyzeReceiptGemini rateLimitCleanup exportToSheets --region europe-west1
```

Or delete one at a time:
```bash
firebase functions:delete analyzeReceiptGemini --region europe-west1
```

To temporarily disable without deleting, set `maxInstances: 0` in the function config and redeploy.

### Outdated firebase-functions SDK warning
```bash
cd functions
npm install --save firebase-functions@latest
```
Note: This may introduce breaking changes — check the [migration guide](https://firebase.google.com/docs/functions/manage-functions) before upgrading.
