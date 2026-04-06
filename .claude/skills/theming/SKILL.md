---
name: theming
description: App theming system — custom colors, responsive spacing, typography, CompositionLocals. Use when styling components, choosing colors/spacing, or working with the design system.
---

# Theming

## AppTheme

Wraps `MaterialExpressiveTheme`. Automatically selects light/dark colors and responsive dimens based on window size.

## Custom colors — `LocalAppColors.current`

Access via `LocalAppColors.current` (not `MaterialTheme.colorScheme` for app-specific colors).

```kotlin
@Immutable
data class AppColors(
    val onboardingBackground: Color,
    val onboardingGradientTop: Color,
    val onboardingGradientMid: Color,
    val cardGlass: Color,
    val cardGlassBorder: Color,
    val chartColors: ImmutableList<Color>,        // 6-color palette for charts
    val headerGradient: Brush,
    val categoryGradients: ImmutableList<Brush>,  // 6 gradient brushes
)
```

Brand palette: Sage (`0xFF6B8F71`) primary, Clay (`0xFFA07855`) secondary, Wheat (`0xFFC4A265`) tertiary.

Standard Material 3 colors (primary, surface, etc.) are available via `MaterialTheme.colorScheme` as usual.

## Responsive spacing — `LocalAppDimens.current`

```kotlin
val dimens = LocalAppDimens.current
Modifier.padding(dimens.small2)
```

| Token | Compact (phone) | Medium (tablet) | Expanded (desktop) |
|-------|-----------------|-----------------|-------------------|
| `extraSmall` | 8dp | 10dp | 12dp |
| `small1` | 12dp | 14dp | 18dp |
| `small2` | 16dp | 18dp | 24dp |
| `small3` | 20dp | 22dp | 30dp |
| `medium1` | 28dp | 30dp | 40dp |
| `medium2` | 36dp | 38dp | 48dp |
| `medium3` | 44dp | 46dp | 56dp |
| `large` | 54dp | 56dp | 66dp |
| `extraLarge` | 64dp | — | — |

Always use dimen tokens instead of hardcoded `dp` values for spacing/sizing.

## Typography

Font: **Space Grotesk** (Regular, Medium, SemiBold, Bold).

Access via `MaterialTheme.typography` as usual. Key sizes:
- `displayLarge` (64sp) — currency amounts
- `headlineLarge` (24sp) — section headers
- `bodyLarge` (20sp) — calculator keys, main body
- `labelLarge` (14sp) — meta-information, UI pills

## Compose previews

- Place in the same file as the composable.
- Private functions, named with `*Preview` suffix.
- Multiple previews per composable for different states.

```kotlin
@Preview
@Composable
private fun FooDefaultPreview() {
    AppTheme {
        FooComponent(/* default state */)
    }
}

@Preview
@Composable
private fun FooLoadingPreview() {
    AppTheme {
        FooComponent(/* loading state */)
    }
}
```
