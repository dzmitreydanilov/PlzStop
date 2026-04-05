# Deploying Firebase Cloud Functions

## Prerequisites

1. **Node.js 22** — matches the runtime in `functions/package.json`
2. **Firebase CLI** — install globally:
   ```bash
   npm install -g firebase-tools
   ```
3. **Authenticated session**:
   ```bash
   firebase login
   ```
4. **Correct project selected** (project ID: `pleasest-e3424`):
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
├── firebase.json          # Firebase config — declares functions source
├── functions/
│   ├── package.json       # Dependencies, Node 22 engine, build/deploy scripts
│   ├── tsconfig.json      # TypeScript config, outputs to lib/
│   ├── src/
│   │   ├── index.ts       # Cloud functions (analyzeReceipt, analyzeReceiptGemini, rateLimitCleanup)
│   │   ├── receipt.ts     # Shared logic (prompt, types, request prep, response parsing)
│   │   ├── rateLimit.ts   # Firestore-backed rate limiting + global daily cap
│   │   └── validation.ts  # Input validation and sanitization
│   └── lib/               # Compiled JS output (gitignored)
```

## Deploy Steps

### 1. Install dependencies (first time or after changing deps)

```bash
cd functions
npm install
```

### 2. Build TypeScript

```bash
npm run build
```

This runs `tsc` and outputs compiled JS to `functions/lib/`.

### 3. Deploy

```bash
# From project root
firebase deploy --only functions
```

Or using the npm script from `functions/`:

```bash
cd functions
npm run deploy
```

### All-in-one

```bash
cd functions && npm install && npm run build && cd .. && firebase deploy --only functions
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
│     ├─ Per-IP burst (10/min)                     │
│     └─ Per-IP daily (50/day)                     │
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
| App Check | Disabled (TODO for production) |
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
| App Check | Disabled (TODO for production) |
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

### Per-IP limits (both functions)

| Limit | Value | Scope |
|---|---|---|
| Burst | 10 requests/minute | Per IP |
| Daily | 50 requests/day | Per IP |

- Document ID is SHA-256 hash of the client IP (no raw IPs stored)
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
- Checked **before** per-IP limits and LLM calls (cheapest check first)
- Does **not** apply to `analyzeReceipt` (Vertex AI) — that function has no global cap

### Check order for `analyzeReceiptGemini`

1. Global daily limit (500/day total)
2. Per-IP burst limit (10/min)
3. Per-IP daily limit (50/day)
4. Input validation
5. LLM call

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
firebase functions:delete analyzeReceipt analyzeReceiptGemini rateLimitCleanup --region europe-west1
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
