---
name: string-resources
description: String resource conventions — naming, accessibility strings, localization patterns. Use when adding UI text, labels, content descriptions, or any user-visible strings.
---

# String Resources

## Files

- `composeResources/values/strings.xml` — all UI text
- `composeResources/values/accessibility_strings.xml` — content descriptions and a11y-only strings

## Naming convention

`snake_case` with feature prefix:

```
home_add_expense
onboarding_select_currency
add_expense_receipt_not_receipt
analytics_tab
content_desc_navigate_back
```

Pattern: `feature_component_element` or `content_desc_action` for accessibility.

## Usage in composables

```kotlin
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.home_add_expense

Text(text = stringResource(Res.string.home_add_expense))

// With parameters (%1$s, %1$d in XML)
Text(text = stringResource(Res.string.home_greeting, userName))
```

## Accessibility strings

Separate file for a11y content descriptions. Key patterns:

- Navigation: `content_desc_navigate_back`, `content_desc_close`, `content_desc_search`
- State: `content_desc_selected`, `content_desc_not_selected`, `content_desc_loading`
- Progress: `content_desc_step_progress` ("Step %1$d of %2$d")

Usage with semantics:

```kotlin
// Icons — always provide contentDescription
Icon(
    imageVector = vectorResource(Res.drawable.ic_add),
    contentDescription = stringResource(Res.string.content_desc_add),
)

// Decorative images — null contentDescription
Icon(imageVector = icon, contentDescription = null)

// State descriptions
Modifier.semantics {
    stateDescription = stringResource(
        if (isSelected) Res.string.content_desc_selected
        else Res.string.content_desc_not_selected
    )
}

// Loading announcements
Modifier.semantics {
    contentDescription = stringResource(Res.string.content_desc_loading)
    liveRegion = LiveRegionMode.Polite
}
```

## Rules

- No hardcoded strings in composables — always use `stringResource`.
- New a11y strings go in `accessibility_strings.xml`, not `strings.xml`.
- Parametrized strings use `%1$s` (string), `%1$d` (int) format.
