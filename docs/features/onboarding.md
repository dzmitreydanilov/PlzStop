# Onboarding

## Purpose

Multi-step setup wizard that runs on first launch. Collects user preferences (currency, name, budget) and provisions the account before granting access to the main app.

## Process Flow

```mermaid
flowchart TD
    start((Start)) --> load[Load currencies]
    load --> loadOk{Success?}
    loadOk -->|No| err[/Error state/]
    err --> retry[Retry]
    retry --> load
    loadOk -->|Yes| welcome[Welcome screen]
    welcome --> next1[Next tapped]
    next1 --> detect[Detect device currency]
    detect --> currency[Currency selection]
    currency --> selectCur{Currency selected?}
    selectCur -->|No| currency
    selectCur -->|Yes| next2[Next tapped]
    next2 --> name[Name input]
    name --> nameChoice{Next or Skip?}
    nameChoice -->|Skip| budget[Budget input]
    nameChoice -->|Next| budget
    budget --> budgetChoice{Next or Skip?}
    budgetChoice -->|Skip| save[Save profile + provision defaults]
    budgetChoice -->|Next| save
    save --> saveOk{Success?}
    saveOk -->|No| saveErr[/Error overlay/]
    saveErr --> save
    saveOk -->|Yes| home((Home Screen))
```

### Steps

| # | Step | Required | Back | Description |
|---|------|----------|------|-------------|
| 1 | Welcome | Yes | No | Introductory screen |
| 2 | Currency | Yes | No | Select primary currency for all expenses |
| 3 | Name | No (skippable) | Yes | Display name (max 24 chars) |
| 4 | Budget | No (skippable) | Yes | Monthly budget target (0 -- 9,999,999 minor units) |

## Features

### Currency Selection
- Full list of currencies fetched from the server.
- Device locale auto-detection: on transitioning from Welcome to Currency, the app detects the device currency and pre-selects it.
- Bottom sheet picker with search.
- Shows symbol, code, and full name.

### Name Input
- Free-text, trimmed to 24 characters.
- Optional -- user can skip.

### Monthly Budget
- Numeric input respecting the selected currency's decimal places.
- Range: 0 to 9,999,999.
- Optional -- user can skip.

### Completion
- Saves user profile (display name, currency, budget) to the backend.
- Marks onboarding as complete locally so it doesn't re-appear.
- Provisions default categories and subcategories for the account.
- Navigates to the Home screen.

## Error Handling

| Context | Trigger | Behaviour |
|---------|---------|-----------|
| Load currencies | Network/server failure | Error state with retry button |
| Save onboarding | Network/server failure | Error overlay, user can dismiss and retry |

## UI States

| State | When |
|-------|------|
| Loading | Initial data fetch |
| Content | Active wizard with step indicator |
| Error | Unrecoverable load failure |

## Domain Operations

| Operation | Description |
|-----------|-------------|
| Load onboarding data | Fetch available currencies |
| Detect device currency | Read device locale, match to currency list |
| Complete onboarding | Persist profile, mark complete, provision defaults |
