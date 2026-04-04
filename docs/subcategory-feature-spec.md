# Subcategory Feature — Functional Specification

## 1. Feature Objective

Allow the AI-powered receipt scanner to map line items to a two-tier structure (**Category > Subcategory**), providing users with more detailed spending analytics (e.g., instead of just "Grocery," seeing "Grocery > Dairy").

Gated behind Firebase Remote Config flag `subcategories_enabled`. When **OFF**, the app behaves exactly as today.

---

## 2. Functional Requirements

### A. Hierarchical Data Structure

| Concept | Description |
|---------|-------------|
| **Parent Categories** | Existing global categories (Food, Transport, Housing, etc.) |
| **Child Subcategories** | Specific labels nested under parents (e.g., "Dairy," "Produce," "Meat" under "Food") |
| **Fallback Logic** | If the AI is unsure of the subcategory, it defaults to `null` (parent category only) |

### B. AI Scanning & Classification Logic

- **Line-Item Extraction:** OCR continues to identify individual items and prices.
- **NLP Mapping:** Classification engine returns a pair of IDs `(categoryId, subcategoryId?)` for every item based on keywords.
  - Example: "Whole Milk 1L" → Grocery (Parent) → Dairy (Sub)
- **Backward Compatibility:** Cloud Function receives subcategories list only when flag is ON. When OFF, request format is unchanged.

### C. User Interface (UX)

- **Category + Subcategory Picker:** After selecting a parent category, a second picker appears for subcategory selection (optional).
- **Manual Overrides:** Users can tap to change either parent or subcategory. Changing the parent clears and re-filters the subcategory list.
- **Home/Analytics:** Spending views group by parent category first, with expandable subcategory breakdown when available.
- **Management Screen** *(Phase 5, deferred)*: Settings page to add, rename, or delete custom subcategories under parent categories.

---

## 3. Data Model

### Parent Categories (existing)

| ID | Name | Icon |
|----|------|------|
| 1 | Food | ic_food |
| 2 | Transport | ic_transport |
| 3 | Housing | ic_housing |
| 4 | Entertainment | ic_entertainment |
| 5 | Groceries | ic_groceries |
| 6 | Health | ic_health |
| 7 | Shopping | ic_shopping |
| 8 | Education | ic_education |
| 9 | Subscriptions | ic_subscriptions |
| 10 | Other | ic_other |

### Example Subcategories

| Parent Category | Subcategories |
|----------------|---------------|
| Food | Restaurants, Coffee, Fast Food, Delivery |
| Groceries | Produce, Dairy, Frozen, Snacks, Beverages, Meat |
| Housing | Furniture, Cleaning Supplies, Decor, Repairs |
| Health | Pharmacy, Gym, Supplements, Doctor Visits |
| Shopping | Clothing, Skincare, Electronics, Accessories |
| Transport | Fuel, Public Transit, Parking, Ride Share |
| Entertainment | Movies, Games, Concerts, Streaming |
| Education | Books, Courses, Supplies |
| Subscriptions | Software, Streaming, Memberships |
| Other | Gifts, Donations, Miscellaneous |

### Database Schema (v4)

**New table: `subcategory`**

| Column | Type | Constraints |
|--------|------|------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT |
| `parentCategoryId` | INTEGER | NOT NULL, FK → category(id) **ON DELETE CASCADE** |
| `name` | TEXT | NOT NULL |
| `iconKey` | TEXT | NOT NULL |
| `isDefault` | INTEGER | NOT NULL (1 = bundled default, 0 = user-created) |
| `sortOrder` | INTEGER | NOT NULL |

**Modified table: `expense`**

| Column | Type | Change |
|--------|------|--------|
| `subcategoryId` | INTEGER | **NEW**, nullable, FK → subcategory(id) **ON DELETE SET NULL** |

**Deletion behavior:**

| Action | Result |
|--------|--------|
| User deletes a subcategory | All linked expenses → `subcategoryId = NULL`, parent `categoryId` unchanged |
| User deletes a parent category | All its subcategories cascade-deleted → linked expenses get `subcategoryId = NULL` |
| User adds custom subcategory | Inserted with `isDefault = 0` |
| Feature flag OFF | Table exists but is unused; all expenses have `subcategoryId = NULL` |

**Migration SQL (v3 → v4):**

```sql
-- New table
CREATE TABLE IF NOT EXISTS subcategory (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    parentCategoryId INTEGER NOT NULL,
    name TEXT NOT NULL,
    iconKey TEXT NOT NULL,
    isDefault INTEGER NOT NULL,
    sortOrder INTEGER NOT NULL,
    FOREIGN KEY (parentCategoryId) REFERENCES category(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS index_subcategory_parentCategoryId
    ON subcategory (parentCategoryId);

-- Expense gets nullable subcategory reference
ALTER TABLE expense ADD COLUMN subcategoryId INTEGER DEFAULT NULL
    REFERENCES subcategory(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS index_expense_subcategoryId
    ON expense (subcategoryId);
```

---

## 4. Limits & Validation

All limits are enforced **on the app side** (use case layer), not in the database.

| Limit | Value | Enforced in |
|-------|-------|-------------|
| Default categories | 10 (fixed, not deletable) | Bundled JSON; UI hides delete for `isDefault = true` |
| Custom categories | Up to **10** | `AddCategoryUseCase` — check count before insert |
| Default subcategories per category | 3 (fixed, not deletable) | Bundled JSON; UI hides delete for `isDefault = true` |
| Custom subcategories per category | Up to **7** | `AddSubcategoryUseCase` — check count before insert |
| Total subcategories per category | **10** (3 default + 7 custom) | |

### ID Uniqueness Guard

Categories and subcategories live in **separate tables** with independent `AUTOINCREMENT` primary keys. Their ID spaces can overlap (e.g., category `id=3` and subcategory `id=3` can coexist). To avoid ambiguity when sending IDs to the AI model:

- **App side:** Always pass IDs as typed pairs — `categoryId` and `subcategoryId` are never mixed in the same field.
- **Cloud Function prompt:** Categories and subcategories are listed in a nested structure (subcategories indented under their parent), so the model always sees which ID belongs to which level.
- **Cloud Function response:** Returns `categoryId` and `subcategoryId` as separate fields — never a single combined ID.
- **UI / StateHolders:** `selectedCategoryId` and `selectedSubcategoryId` are separate fields in `ExpenseFormInput`. Changing `selectedCategoryId` always clears `selectedSubcategoryId`.

This separation ensures there is **no path** where a category ID is confused with a subcategory ID, despite overlapping AUTOINCREMENT sequences.

---

## 5. Feature Flag

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `subcategories_enabled` | Boolean | `false` | Enables two-tier category hierarchy across the app |

When flag is OFF:
- No subcategory picker in expense form
- Receipt AI receives categories only (no subcategories)
- Home/Analytics show flat category view
- Subcategory DB table exists but is unused

---

## 6. Receipt AI — Cloud Function Changes

### Current flow (no subcategories)

**Request** (app → Cloud Function):
```json
{
  "imageBase64": "...",
  "categories": [
    {"id": 1, "name": "Food"},
    {"id": 5, "name": "Groceries"}
  ]
}
```

**User prompt** sent to Gemini:
```
Categories:
- ID: 1, Name: "Food"
- ID: 5, Name: "Groceries"
```

**Response schema:**
```json
{
  "status": "success",
  "data": {
    "merchantName": "...",
    "totalAmount": 42.99,
    "currency": "USD",
    "date": "2026-04-04",
    "categoryId": 5
  },
  "message": null
}
```

### Updated flow (with subcategories)

**Request** (app → Cloud Function, when flag ON):
```json
{
  "imageBase64": "...",
  "categories": [
    {"id": 1, "name": "Food"},
    {"id": 5, "name": "Groceries"}
  ],
  "subcategories": [
    {"id": 1, "parentCategoryId": 1, "name": "Restaurants"},
    {"id": 2, "parentCategoryId": 1, "name": "Coffee"},
    {"id": 3, "parentCategoryId": 5, "name": "Dairy"},
    {"id": 4, "parentCategoryId": 5, "name": "Produce"}
  ]
}
```

When flag is OFF, `subcategories` key is **omitted entirely** — the request is identical to today.

**User prompt** sent to Gemini (nested format):
```
Categories:
- ID: 1, Name: "Food"
  Subcategories: [ID: 1 "Restaurants"], [ID: 2 "Coffee"]
- ID: 5, Name: "Groceries"
  Subcategories: [ID: 3 "Dairy"], [ID: 4 "Produce"]
- ID: 7, Name: "Shopping"
  (no subcategories)
```

The nested format helps the model understand the hierarchy and pick a valid `(categoryId, subcategoryId)` pair.

**Updated system prompt** additions:
```
- subcategoryId: if subcategories are provided, pick the best matching subcategory ID
  under the chosen category. Only use subcategory IDs listed under that category.
  Return null if no subcategory fits or if no subcategories are provided.
```

**Updated response schema:**
```json
{
  "status": "success",
  "data": {
    "merchantName": "Whole Foods",
    "totalAmount": 42.99,
    "currency": "USD",
    "date": "2026-04-04",
    "categoryId": 5,
    "subcategoryId": 3
  },
  "message": null
}
```

`subcategoryId` is nullable. When the model is unsure or no subcategories were sent, it returns `null`.

### Token impact estimate

| Component | Current | With subcategories (worst case) |
|-----------|---------|--------------------------------|
| System prompt | ~200 tokens | ~230 tokens (+1 rule) |
| Categories text | ~100 tokens (10 cats) | ~200 tokens (20 cats) |
| Subcategories text | 0 | ~2,100 tokens (20 × 7 = 140 subs) |
| **Total prompt text** | **~300 tokens** | **~2,530 tokens** |
| Image | ~250 tokens (Gemini) | ~250 tokens (unchanged) |

**Impact: negligible.** The image dominates processing time. Adding ~2.2K tokens of structured text will not noticeably affect Gemini 2.5 Flash latency or quality with `thinkingBudget: 0`.

### Cloud Function file changes (`functions/src/index.ts`)

1. **Interface:** Add `Subcategory` type with `id`, `parentCategoryId`, `name`
2. **Request parsing:** Destructure optional `subcategories` from `request.data`
3. **Prompt building:** Build nested category+subcategory text when subcategories are present
4. **System prompt:** Add `subcategoryId` rule to `SYSTEM_PROMPT`
5. **Response schema:** Add `subcategoryId: number | null` to `ReceiptResponse.data`
6. **Response parsing:** Extract `parsed.data?.subcategoryId` and include in return

---

## 7. Implementation Phases

### Phase 1: Feature Flag + Data Layer
- Extend `RemoteConfigDataSource` with `fetchBoolean`
- Create `SubcategoryEntity`, `SubcategoryDao`
- DB migration v3 → v4
- Default subcategories JSON bundle
- `SubcategoryRepository` + impl

### Phase 2: Domain Layer
- `ExpenseSubcategory` model, extend `AddExpenseFormData`
- Add `subcategoryId` to expense save/update flow
- Update `ReceiptData` and receipt analysis to support subcategories
- Conditional subcategory seeding during onboarding
- Add limit validation in `AddCategoryUseCase` and `AddSubcategoryUseCase`

### Phase 3: Expense Form UI
- `SubcategoryUiModel` in state, `SubcategorySelected` event
- Subcategory picker sheet (filtered by selected parent)
- Auto-clear subcategory when parent changes
- Receipt scan result applies subcategory

### Phase 4: Home & Analytics
- Subcategory spending aggregation query
- Expandable subcategory breakdown in home screen
- Nested slices in analytics charts

### Phase 5: Cloud Function Update
- Update `functions/src/index.ts` with subcategory support
- Update system prompt, request parsing, response schema
- Deploy independently — client handles `subcategoryId = null` gracefully

### Phase 6 (Deferred): Management Screen
- Settings entry to manage subcategories
- Add/rename/delete custom subcategories per parent
- Prerequisite: Phases 1–5 merged and stable

---

## 8. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Room KSP reprocessing on entity changes | Build time increase | Run both Android + iOS builds in CI before merging Phase 1 |
| Remote Config cold start delays form loading | Slow expense form open | Cache flag value per session (same pattern as currency cache) |
| SQLite FK enforcement on older Android | No enforcement for ALTER TABLE added FKs | Acceptable — existing rows legitimately have no subcategory |
| Cloud Function must be updated | Receipt AI won't return subcategoryId until backend deploys | Client handles null gracefully; ship client first |
| Overlapping AUTOINCREMENT IDs | Category ID = Subcategory ID confusion | Separate fields everywhere; nested prompt format; never mixed |
| Model picks wrong subcategory | Incorrect classification | Subcategory is always optional; user can override manually |
