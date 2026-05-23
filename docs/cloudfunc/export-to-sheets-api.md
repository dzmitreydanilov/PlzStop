# exportToSheets — API Contract

Firebase Gen 2 HTTPS Callable · Region: `europe-west1` · Runtime: Python 3.12 · Codebase: `py`

## Authentication

Requires Firebase Auth. Unauthenticated calls receive `UNAUTHENTICATED` error.

## Request

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `googleAccessToken` | `string` | Yes | — | User's Google OAuth access token with Drive/Sheets scope |
| `expenses` | `object[]` or `string` | Yes | — | Expense data. Plain array or base64-encoded gzip JSON (see `compressed`) |
| `exportId` | `string` | No | `null` | Local export ID. If provided, result is written to Firestore for recovery |
| `title` | `string` | No | `null` | Custom spreadsheet name. Falls back to `"PlzStop Export – {month}"` |
| `month` | `string` | No | `"Export"` | Label used in default title and single-tab sheet name |
| `tabLayout` | `string` | No | `"single_tab"` | `"single_tab"` or `"separate_tabs"` |
| `currencySymbol` | `string` | No | `""` | Shown in the Amount column header, e.g. `"$"`, `"EUR"` |
| `decimalPlaces` | `int` | No | `2` | Number format precision (max 10) |
| `compressed` | `bool` | No | `false` | If `true`, `expenses` is a base64-encoded gzip JSON string (max 10 MB decompressed) |
| `fcmToken` | `string` | No | `null` | Device FCM token for push notification on completion |

### Expense object

| Field | Type | Required | Description |
|---|---|---|---|
| `date` | `string` | Yes | ISO date (`"2024-06-15"`) — converted to Sheets date serial |
| `title` | `string` | Yes | Expense description |
| `category` | `string` | No | Category name (used in summary grouping + SUMIF) |
| `subcategory` | `string` | No | Subcategory name (used in summary breakdown + SUMIFS) |
| `amount` | `string` | Yes | Numeric string, e.g. `"450"` (minor units) |
| `notes` | `string` | No | Free text |
| `originalAmount` | `string` | No | Amount in original currency (not yet used in spreadsheet) |
| `originalCurrency` | `string` | No | Original currency code (not yet used in spreadsheet) |

## Response

```json
{
  "spreadsheetUrl": "https://docs.google.com/spreadsheets/d/..."
}
```

## Errors

| Code | Condition |
|---|---|
| `UNAUTHENTICATED` | No Firebase Auth token |
| `INVALID_ARGUMENT` | Missing/empty expenses, invalid compressed payload |
| `INTERNAL` | No email in auth token, spreadsheet creation/write failure |

## Spreadsheet Output

### Columns

| A | B | C | D | E | F |
|---|---|---|---|---|---|
| Date | Title | Category | Subcategory | Amount ({symbol}) | Notes |

### Tab layouts

**`single_tab`** — All expenses on one sheet, sorted by date.

Category summary uses pivot format when data spans multiple months:

```
Category        Jan 2024    Feb 2024    Total
Food            ...         ...         ...
  Groceries     ...         ...         ...
  Drinks        ...         ...         ...
Transport       ...         ...         ...
```

Falls back to simple 2-column summary (Category + Amount) when data is single-month.

**`separate_tabs`** — One sheet per month (e.g. "January 2024"). Each sheet has its own simple Category + Subcategory summary.

### Formatting

- Header row: bold, blue background, white text, frozen
- Total row: bold
- Category summary header: bold, light blue background
- Date column: `yyyy-mm-dd` format
- Amount column: number format based on `decimalPlaces`
- Column widths: Date 100, Title 200, Category 150, Subcategory 150, Amount 120, Notes 250

## Side Effects

1. **Spreadsheet shared** with the authenticated user's email as owner
2. **Firestore write** (if `exportId` provided): `exports/{uid}/history/{exportId}` with `{ spreadsheetUrl, status, completedAt }`
3. **FCM notification** (if `fcmToken` provided): push with deep link `plzstop://open?url={spreadsheetUrl}` — best effort, failure is non-fatal

## Security

- All string fields in expense rows are sanitized against formula injection (`=`, `+`, `-`, `@` prefixed with `'`)
- Category names in SUMIF/SUMIFS formulas have `"` escaped
- Compressed payloads limited to 10 MB decompressed
- `decimalPlaces` capped at 10
- Error messages are generic — no internal details exposed
