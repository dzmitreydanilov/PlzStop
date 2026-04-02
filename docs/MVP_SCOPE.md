# Expense Tracker — MVP Scope

## Product Vision

A lightweight, local-first personal expense tracker that helps users understand where their money goes. Track expenses manually or by snapping a receipt photo. See spending trends at a glance.

**Target platforms:** iOS, Android, Web (Compose Multiplatform)

**Data philosophy:** All data is stored on-device only. No cloud accounts, no sync. The local database is encrypted at rest (SQLCipher) to protect sensitive financial data.

---

## MVP Screens & Functional Requirements

### 1. Onboarding

A linear 4-step flow (horizontal pager with dot indicators). User must complete all steps before accessing the app. No bottom navigation bar during onboarding.

If the user kills the app before completing onboarding, the next launch resumes where they left off (completed fields preserved).

**Step 1 — Welcome**
- App logo + tagline
- Optional display name field (max 24 characters). If left empty, app greets with "Hi there"
- "Next" button always enabled (name is optional)

**Step 2 — Default Currency**
- Searchable scrollable list of currencies (bundled JSON, no network dependency)
- Shows currency code + symbol + full name
- Popular currencies pinned at top (USD, EUR, GBP, JPY, CNY, INR)
- Single selection, required
- "Next" button disabled until a currency is selected

**Step 3 — Monthly Budget**
- Numeric input field with the currency symbol from step 2
- Required, must be > 0, max 9,999,999
- Decimal places enforced per currency (2 for USD, 0 for JPY, etc.)
- Reassurance text: "You can always change this in Settings"

**Step 4 — Expense Categories**
- Grid of default categories (bundled in app): Food, Transport, Housing, Entertainment, Groceries, Health, Shopping, Education, Subscriptions, Other
- All categories start unselected — user must actively choose
- User must select at least 3
- User can create up to 5 custom categories (name + icon)
- "Get Started" button saves all data atomically and navigates to Home

**After completion:** navigate to Home screen, clear onboarding back stack. Onboarding never shows again on this device.

**Navigation rules:**
- Forward: button-driven only (forward swipe disabled to enforce validation)
- Backward: swipe and back button enabled on steps 2–4
- Back on step 1: exits the app

---

### 2. Home Screen

The main screen for browsing categories and logging expenses.

**Header**
- Greeting: "Hi, {name}" with initial-letter avatar (or "Hi there" if no name set)
- Total spending for the current month: "$1,240 spent this month"
- Profile avatar taps to Settings

**Categories Grid**
- Grid of all user's active categories (3 columns on phone, 4–6 on tablet/web)
- Each tile: icon, category name, amount spent this month
- Categories with $0 spent appear but are visually muted
- Sorted by user's custom order (set in Settings > Manage Categories)
- Tapping a category tile navigates to a filtered transaction list (see section 6 below)

**Floating Action Button (FAB)**
- "+" button, always visible
- Tapping opens Add Expense screen

**Empty State**
- All selected categories shown at $0 (muted)
- Centered illustration + "Start tracking! Tap + to add your first expense."
- FAB has a subtle pulse animation to draw attention

---

### 3. Add Expense

Full-screen form for recording an expense. Two entry modes: **Manual** and **Photo Analysis**.

#### 3a. Manual Entry

**Amount Input**
- Large numeric display at top of screen (custom styled, no standard outline)
- Currency symbol shown (from user settings)
- Custom in-app numeric keyboard (0–9, decimal, backspace, done) — no system keyboard. Opens automatically in add mode (not in edit mode)
- Required, must be > 0, max 9,999,999
- Decimal places enforced per currency

**Title / Merchant**
- Free text, required
- Max 60 characters
- Character counter shown after 50 characters
- Examples: "Starbucks", "Uber ride", "Grocery run"

**Category Picker**
- Grid of selectable tiles (3 columns on phone, 4–6 on tablet/web)
- Shows only the user's active categories
- Single-select, required
- Default: none pre-selected

**Date & Time**
- Defaults to current date and time
- Tapping date opens M3 date picker; tapping time opens M3 time picker
- Cannot select future dates
- No lower bound — user can log forgotten expenses from any past date

**Notes (optional)**
- Multiline text field
- Max 250 characters
- Character counter shown after 200 characters

**Save Button**
- Label: "Add Expense" (add mode) or "Save Changes" (edit mode)
- Disabled until all required fields are valid
- Shows progress indicator during save
- On save: writes to local DB, returns to previous screen, Home spending total updates immediately
- Shows brief success snackbar

**Unsaved Changes**
- If user taps back and form has changes: confirmation dialog "Discard changes?"
- If form is untouched: back navigates immediately

**Edit Mode**
- Reached by tapping a transaction in the filtered transaction list (see section 6)
- Pre-fills all fields from the existing expense
- "Delete expense" text button (error color) below save button
- Delete: confirmation dialog → soft-delete → navigate back with undo snackbar (5 seconds)

#### 3b. Photo Analysis (Receipt Scan)

**Entry Point**
- "Scan Receipt" button with camera icon on the Add Expense form
- Active and tappable (not grayed out — this is an MVP feature)

**Camera / Gallery Access**
- Opens device camera or photo gallery
- User takes/selects a photo of a receipt

**AI Processing**
- App sends receipt image to cloud vision LLM for extraction
- Shows loading state ("Analyzing receipt...")
- Extracts: merchant name, total amount, date, individual line items (if readable)

**Review & Confirm**
- Pre-fills the manual entry form with extracted data
- All fields are editable — user can correct any AI mistakes
- Category is suggested based on merchant name (user can change)
- User must explicitly tap "Save" — nothing is auto-saved

**Error Handling**
- If image is unreadable: "Couldn't read this receipt. Try again or enter manually."
- If extraction is partial: fill what was found, leave rest for user
- If no network: "Receipt scan requires an internet connection. Please enter manually."
- Always allow fallback to full manual entry

---

### 4. Analytics

Visual insights into spending patterns and budget health. Read-only — no editing or adding expenses happens here.

**Time Period Selector**
- Segmented button row: Week | Month (default) | Custom
- Week/Month: navigation arrows to browse previous/next periods
- Custom: opens date range picker (min 1 day, max 365 days)
- Forward navigation capped at current week/month (no future periods)
- Selection resets to "Month (current)" when leaving and returning to the tab

**Budget Ring** (shown only in Month view)
- Circular progress indicator showing % of budget spent
- Color coding: 0–60% green, 60–85% yellow, 85%+ red
- Center shows spent amount / total budget (e.g., "$1,240 / $2,000")
- Below ring: remaining amount, or "Over budget by $X" in red when exceeded

**Spending Over Time (Bar Chart)**
- Vertical bars showing spending aggregated by sub-period:
  - Week view → 7 daily bars (Mon–Sun)
  - Month view → 4–5 weekly bars (W1–W5)
  - Custom → daily, weekly, or monthly bars depending on range length
- Tapping a bar shows tooltip with period label and amount
- Total spent shown below chart

**vs Last Period Comparison Card**
- Shows percentage change vs equivalent prior period
- Up arrow (red) = spending increased; down arrow (green) = spending decreased
- Shows absolute amounts: "$1,500 vs $1,340"
- If no prior data: "Not enough data yet — keep tracking to see trends"

**Average Daily Spend Card**
- Total spent / days in period
- For current (partial) month/week: uses days elapsed, not full period length
- Shows number of days in period

**Category Breakdown (Donut Chart)**
- Ring chart showing spending proportions by category
- Each segment colored by category color
- Categories under 3% grouped into "Other" segment
- Tapping a segment: pulls outward, center label updates to show category name + amount + %, corresponding ranking row highlights
- Center shows total spent when no segment is selected

**Category Ranking**
- List below donut, sorted by amount descending
- Each row: rank number, icon, category name, amount, percentage, progress bar
- All categories with spend > $0 shown (including those grouped into "Other" in the donut)
- Tapping a row highlights the corresponding donut segment
- Categories with $0 in the selected period are excluded

**Empty State**
- "No spending data yet. Add some expenses to see your analytics here."
- If data exists in other periods: "No expenses in this period. Try selecting a different time range."

---

### 5. Settings

Accessed from bottom navigation tab.

**Profile**
- Initial-letter avatar, display name (editable via bottom sheet, max 24 characters)
- Name change: bottom sheet with text field

**Budget & Currency**
- Edit monthly budget (bottom sheet, same input as onboarding)
- Change default currency (bottom sheet with searchable list)
- Warning on currency change: "Changing currency will not convert existing transactions. New transactions will use the selected currency."
- Budget changes take effect immediately for current and future months

**Categories**
- "Manage categories" → full-screen category management view
- View all categories (active + inactive sections)
- Add new custom category (name + icon + color, no limit unlike onboarding's 5)
- Edit any category (name, icon, color — including renaming defaults)
- Delete category: soft-delete with confirmation. Transactions keep their category label. Category moves to "Inactive" section with "Restore" button
- Reorder categories via drag-and-drop
- Cannot delete the last active category

**Appearance**
- Theme: System default / Light / Dark
- Applied instantly, stored locally

**Data & Privacy**
- Export data as CSV (all transactions, all months, UTF-8 with BOM)
- Erase all data: two-step confirmation (first dialog explains consequences, second requires typing "DELETE"). Permanently deletes all local data and resets to onboarding

**About**
- App version
- Links: Open-source licenses

---

### 6. Filtered Transaction List (Lightweight)

A minimal transaction list screen accessed by tapping a category tile on the Home screen. Not a full tab — this is a pushed screen.

**Content**
- Filtered by the tapped category for the current month
- Grouped by day: "Today", "Yesterday", "Mar 25, 2026"
- Each row: category icon, title, time, amount
- Tapping a row opens Add Expense in edit mode

**Header**
- Back button → returns to Home
- Title: "{Category Name}" (e.g., "Food")
- Subtitle: month + total for this category (e.g., "March 2026 — $320")

**Category change in edit mode**
- If the user edits an expense and changes its category, on return the list refreshes and the moved expense disappears from the current filter. This is expected behavior — no special handling needed.

**Empty State**
- "No expenses in {category} this month."

> **Note:** This is a simplified version of the full Transactions screen (TRANSACTIONS.md). No search bar, no filter chips, no sort options, no swipe-to-delete. Those are v1.1 features.

---

## Navigation Structure

**Bottom Navigation Bar (MVP):**

| Tab | Label | Screen |
|-----|-------|--------|
| 1 | Home | Home Screen |
| 2 | Analytics | Analytics |
| 3 | Settings | Settings |

- FAB overlaid on bottom bar for "Add Expense" (visible on Home tab)
- On tablet/web: navigation rail instead of bottom bar

**Navigation graph:**

```
Onboarding (4 steps) → Home
                        ├── FAB → Add Expense (full screen, push)
                        ├── Category tile → Filtered Transaction List (push)
                        │                   └── Transaction row → Add Expense (edit mode, push)
                        └── Avatar → Settings tab (tab switch)

Analytics (tab) — self-contained, no outbound navigation

Settings (tab) ├── Profile → Edit Name (bottom sheet)
               ├── Budget → Edit Budget (bottom sheet)
               ├── Currency → Edit Currency (bottom sheet)
               ├── Categories → Category Management (push)
               ├── Export → Loading → Share sheet
               └── Erase All Data → Confirmation flow → Onboarding
```

---

## Data Storage

All data is stored on-device. No cloud sync, no accounts, no network dependency for core features.

- **Database:** SQLDelight with SQLCipher encryption at rest (iOS/Android). Web uses IndexedDB — encryption strategy TBD (browser has no secure keychain equivalent)
- **Encryption key (mobile):** generated on first launch, stored in platform keychain (iOS Keychain / Android Keystore)
- **Adding/editing/deleting expenses:** writes to local encrypted DB immediately
- **Analytics:** computed entirely from local data
- **Export:** reads from local DB
- **Data portability:** CSV export is the only way to move data off-device
- **Erase all data:** deletes the encrypted DB, clears keychain entry, resets to onboarding
- **Network required only for:** receipt photo AI analysis

---

## Out of MVP Scope

The following features are deferred to later releases:

| Feature | Version | Notes |
|---------|---------|-------|
| Transactions tab (full history, search, filters, swipe-to-delete) | v1.1 | Spec exists in TRANSACTIONS.md. MVP has a lightweight filtered list instead |
| User accounts (Google/Apple Sign-In) | v1.1 | Required for cloud sync |
| Cloud sync (Firestore) | v1.1 | Cross-device data sync, backup/restore |
| Multiple wallets / accounts | v1.2 | Add, delete, switch wallets; wallet summary screen |
| Push notifications (budget alerts) | v1.2 | 80% warning, exceeded alert. Notification toggles in Settings deferred too |
| Bank account sync | v2.0 | Integration with banking APIs (Plaid, Salt Edge, etc.) |
| Voice transaction tracking | v2.0 | "Spent 15 dollars on coffee" → auto-creates expense |
| Receipt line-item splitting | v2.0 | Split one receipt across sub-categories (e.g., Auchan → sweets, household goods) |
| AI trend analysis & optimization tips | v2.0 | "You spent 30% more on dining this month" — personalized actionable advice |

---

## Success Criteria

The MVP is shippable when a user can:

1. Complete onboarding (name, currency, budget, categories) and start using the app immediately
2. Add an expense manually with amount, title, category, and date
3. Add an expense by photographing a receipt (AI extracts data, user confirms)
4. See total monthly spending on the Home screen and budget health (ring + remaining) in Analytics
5. See all spending categories at a glance and tap into a filtered list
6. Edit or delete an existing expense
7. View spending analytics: bar chart over time, category donut, period comparison, daily average
8. Switch time periods (week/month/custom) in Analytics
9. Adjust budget, currency, categories, and display name in Settings
10. Switch between light/dark themes
11. Export all data as CSV
12. Trust that their financial data is encrypted on-device and never leaves the device without explicit action (export/receipt scan)
