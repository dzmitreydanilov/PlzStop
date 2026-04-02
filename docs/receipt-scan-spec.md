# Receipt Scan — Feature Spec

## Overview

Users can scan a receipt from the Add Expense screen. The app launches a native document scanner, sends the captured image to a cloud AI for analysis, and pre-fills the expense form with extracted data (merchant name, amount, date, category).

---

## User Flow

```
Add Expense Screen
       │
       ▼
[Scan Receipt] button tap
       │
       ▼
┌─────────────────────┐
│  Native Scanner UI   │  Platform-provided (ML Kit / VisionKit)
│  - Auto-detects edges │  User sees camera with document overlay
│  - Crop & adjust     │  Can also import from gallery (Android)
│  - Review & confirm  │
└─────────────────────┘
       │ JPEG bytes (compressed to ~300KB)
       ▼
┌─────────────────────┐
│ "Analyzing receipt…" │  Loading overlay on Add Expense screen
│  loading overlay     │
└─────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  Firebase Cloud Function             │
│  analyzeReceipt (europe-west1)       │
│                                      │
│  1. Validate image + categories      │
│  2. Send to Gemini 2.5 Flash         │
│     - Image as base64                │
│     - User's categories with IDs     │
│     - Structured JSON response mode  │
│  3. Parse response                   │
│  4. Return extracted data            │
└─────────────────────────────────────┘
       │
       ▼
  ┌─────────┐
  │ Result? │
  └────┬────┘
       │
  ┌────┴──────────┬──────────────┐
  ▼               ▼              ▼
success         partial       unreadable
  │               │              │
  ▼               ▼              ▼
Form filled    Form partially   Snackbar:
with all       filled (missing  "Couldn't read
fields         fields keep      this receipt"
               current values)
```

---

## What Gets Pre-Filled

| Receipt field | Maps to | Behavior |
|---|---|---|
| Merchant name | Title | Overwrites if extracted |
| Total amount | Amount | Converted to minor units (e.g. $42.99 → 4299) |
| Date | Date picker | Parsed from YYYY-MM-DD |
| Category | Category picker | AI matches merchant/items to user's categories by ID |
| Currency | — | Ignored in MVP (amount filled as-is, user reviews before saving) |

The user always reviews and can edit all fields before saving.

---

## Error States

| Scenario | User sees |
|---|---|
| Scanner cancelled | Nothing (returns to form) |
| Camera permission denied (iOS) | Scanner fails silently |
| Image is not a receipt | "Couldn't read this receipt. Try again or enter manually." |
| No internet | "Receipt scan requires an internet connection." |
| Server error / timeout | "Service temporarily unavailable. Enter manually." |
| Blurry but partial read | Form partially filled, user completes manually |

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│ commonMain                                           │
│                                                      │
│  AddExpenseScreen                                    │
│       │ ReceiptScanned(imageBytes)                   │
│       ▼                                              │
│  AddExpenseStateHolder                               │
│       │                                              │
│       ▼                                              │
│  AnalyzeReceiptUseCase                               │
│       │ gets categories + decimalPlaces              │
│       │ from AddExpenseRepository                    │
│       ▼                                              │
│  ReceiptRepository                                   │
│       │ base64 encode, 30s timeout                   │
│       ▼                                              │
│  FirebaseCallableFunctions (interface)               │
└──────────┬──────────────────────┬────────────────────┘
           │                      │
    ┌──────▼──────┐       ┌───────▼────────┐
    │ Android     │       │ iOS            │
    │ Firebase SDK│       │ Swift bridge   │
    │ (direct)    │       │ → Firebase SDK │
    └─────────────┘       └────────────────┘
```

### Platform-specific components

| Component | Android | iOS |
|---|---|---|
| Document scanner | ML Kit Document Scanner API | VisionKit (VNDocumentCameraViewController) |
| Image compression | BitmapFactory + Bitmap.compress | UIImage + UIImageJPEGRepresentation |
| Firebase Functions | Firebase Android SDK | Swift bridge (AppFirebaseFunctionsCaller) |
| App Check | Play Integrity (automatic) | App Attest (via Swift SDK) |
| Camera permission | Handled by ML Kit internally | Handled by VisionKit internally |

### Image pipeline

```
Raw scan (~3-5MB) → compress to max 1024px longest side, JPEG quality 80%
                   → ~200-500KB → base64 encode → send to cloud function
```

---

## Cloud Function

**Name:** `analyzeReceipt`
**Region:** europe-west1
**Runtime:** Node.js 20, TypeScript
**Memory:** 512 MiB
**Timeout:** 120s (server-side), 30s (client-side)
**App Check:** Enforced

### Request

```json
{
  "imageBase64": "<base64 JPEG>",
  "categories": [
    { "id": 1, "name": "Groceries" },
    { "id": 2, "name": "Transport" }
  ]
}
```

### Response

```json
{
  "status": "success | partial | unreadable",
  "data": {
    "merchantName": "string | null",
    "totalAmount": "number | null (e.g. 42.99)",
    "currency": "string | null (ISO 4217)",
    "date": "string | null (YYYY-MM-DD)",
    "categoryId": "number | null (from user's list)"
  },
  "message": "string | null"
}
```

### Validation

- Rejects missing imageBase64 or categories
- Rejects images > 5MB (decoded)
- Returns `IMAGE_TOO_LARGE`, `INVALID_REQUEST`, `QUOTA_EXCEEDED`, `INTERNAL` as error codes

---

## Costing

**AI model:** Gemini 2.5 Flash (thinking disabled)

| Component | Tokens per scan | Rate | Cost |
|---|---|---|---|
| Input: system prompt | ~200 | $0.15 / 1M | $0.00003 |
| Input: image | ~250 | $0.15 / 1M | $0.00004 |
| Input: categories | ~50 | $0.15 / 1M | $0.000008 |
| Output: JSON response | ~80 | $0.60 / 1M | $0.00005 |
| **Total per scan** | **~580** | | **~$0.00013** |

**~7,700 scans per $1.00**

### Cloud Function cost (Firebase Blaze plan)

- Invocations: 2M/month free, then $0.40/million
- Compute: 400K GB-seconds/month free (512MB × 120s = 60 GB-sec per invocation worst case)
- Networking: 5GB/month free outbound

For a typical user scanning 2-5 receipts/week, costs are negligible.

---

## Security

- **App Check** enforced — only the real app can call the function
- **No user auth required** — receipt data is stateless, not stored server-side
- **Image not persisted** — processed in memory, discarded after response
- **Source maps disabled** — TypeScript source not exposed in deployment
- **Base64 size limit** — 5MB cap prevents abuse

---

## Limitations (MVP)

1. **Single page only** — scanner captures one page, multi-page receipts not supported
2. **No currency conversion** — if receipt currency differs from app currency, amount is filled as-is
3. **No line items** — only total amount extracted, not individual items
4. **No receipt storage** — scanned image is not saved, only extracted data is used
5. **No offline support** — requires internet for AI analysis
