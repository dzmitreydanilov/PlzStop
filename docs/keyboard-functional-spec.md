# Numeric Keyboard — Functional Specification

## 1. Layout

4 rows × 5 columns. **Left column** (operators), **middle 3 columns** (digits + currency), **right column** (actions). Save/= button spans the bottom 2 rows vertically.

```
┌─────┬─────┬─────┬─────┬────────┐
│  ÷  │  7  │  8  │  9  │   ⌫    │
├─────┼─────┼─────┼─────┼────────┤
│  ×  │  4  │  5  │  6  │   📅   │
├─────┼─────┼─────┼─────┼────────┤
│  -  │  1  │  2  │  3  │        │
├─────┼─────┼─────┼─────┤  ✓/=   │
│  +  │  ¤  │  0  │  ,  │        │
└─────┴─────┴─────┴─────┴────────┘
```

### Left column — Operators (top to bottom)
`÷`, `×`, `-`, `+`

### Middle section — Digit pad
Calculator-style layout: `7-8-9` top row down to `0` bottom row. Bottom-left cell shows the **active currency sign** (e.g. `$`, `€`, `£`) — this is a display-only button (non-interactive). Bottom-right cell shows `,` (decimal separator).

### Right column — Actions
- **⌫ (Backspace):** delete from right to left, character by character.
- **📅 (Calendar):** opens the date picker.
- **✓ / = (OK/Save):** spans rows 3–4 vertically. Context-dependent:
  - Shows **✓** (checkmark) in normal mode → triggers **Save**.
  - Shows **=** in expression mode (pending operator exists) → triggers **Evaluate**.

## 2. Architecture

### `KeyboardCalculator` — pure calculation logic

Replaces `NumericAmountInput`. A **pure, stateful class** (no ViewModel, no coroutines) that holds expression state and processes key inputs. All methods are synchronous — this keeps it trivially unit-testable.

```kotlin
class KeyboardCalculator(private val decimalPlaces: Int) {
    fun processKey(key: NumericKey): KeyboardState
    fun getState(): KeyboardState
    fun setFromAmount(amountInput: String)   // restore from saved state
    fun getAmountForSave(): String           // final operand value for persistence
    fun parseToMinorUnits(): Long            // convert current value to minor units
}
```

```kotlin
data class KeyboardState(
    val displayExpression: String,   // full expression: "$ 25 + $ 25"
    val isInExpressionMode: Boolean, // true when pending operator exists
    val currentValue: String,        // raw numeric value of current operand
)
```

### Event flow — follows existing StateHolder pattern

The keyboard does **not** have its own ViewModel. It integrates into `BaseExpenseStateHolder` via the existing event pipeline:

1. `NumericKeyboard` composable emits `AddExpenseEvent.KeyPressed(key)`.
2. `BaseExpenseStateHolder.resolveEventResult` handles the event.
3. For digit/operator/decimal/backspace/equals: delegates to `KeyboardCalculator.processKey()`, updates `AddExpenseState` with new expression display.
4. For `Calendar`: emits a result that triggers the date picker.
5. For `Equals` when not in expression mode (acts as Save): delegates to existing `handleSave()` flow.

This keeps the unidirectional data flow: **UI → Event → StateHolder → State → UI**.

### Internal state (inside `KeyboardCalculator`)

| Field              | Type       | Description                                               |
|--------------------|------------|-----------------------------------------------------------|
| `accumulator`      | `Double`   | Running result of evaluated sub-expressions.              |
| `pendingOperator`  | `Operator?`| Operator waiting to be applied.                           |
| `currentOperand`   | `String`   | Digits being entered for the current operand.             |
| `lastResult`       | `Double?`  | Result after `=` was pressed; cleared on next digit input.|
| `isNegativePrefix` | `Boolean`  | `true` if user typed `-` at the very start.               |

### Operator enum

```kotlin
enum class KeyboardOperator { ADD, SUBTRACT, MULTIPLY, DIVIDE }
```

## 3. Calculation logic

### 3.1 Digit / Decimal input

- Appends to `currentOperand`.
- Validation: max `decimalPlaces` after `.`, max value `9 999 999`.
- If `lastResult != null` (user just pressed `=`): clear all state and start fresh with the new digit.

### 3.2 Operator input (`+`, `-`, `×`, `÷`)

#### Case 1 — `-` at the very beginning (empty expression, no accumulator)
- Sets `isNegativePrefix = true`.
- Displays `-` in the input field.
- Next digits build a negative number.

#### Case 2 — `currentOperand` is non-empty AND `pendingOperator` exists
- Evaluate: `accumulator = apply(pendingOperator, accumulator, currentOperand.toDouble())`.
- If operator was `÷` and result `< 0` → reset accumulator to `0`.
- Set `pendingOperator = newOperator`, clear `currentOperand`.
- Display intermediate result in the input field.

#### Case 3 — `currentOperand` is non-empty AND no `pendingOperator`
- `accumulator = currentOperand.toDouble()` (accounting for `isNegativePrefix`).
- Set `pendingOperator = newOperator`, clear `currentOperand`, reset `isNegativePrefix`.
- Display `accumulator` in the input field.

#### Case 4 — `currentOperand` is empty (user just pressed another operator)
- Replace `pendingOperator` with the new operator (operator switching).

### 3.3 Equals (`=`) / OK-Save button

#### When `isInExpressionMode == true`
1. Evaluate: `result = apply(pendingOperator, accumulator, currentOperand.toDouble())`.
2. If operator was `÷` and result `< 0` → reset to `0`.
3. Format `result` to `decimalPlaces` and display in input field.
4. Clear `pendingOperator`, set `currentOperand = formatted(result)`, set `lastResult = result`.
5. `isInExpressionMode` becomes `false` → button reverts to ✓.

#### When `isInExpressionMode == false`
- Triggers **Save** (same as current save behavior).

### 3.4 Backspace (⌫)
- Removes the last character from `currentOperand` (right-to-left, standard behavior).
- If `currentOperand` is already empty and `pendingOperator` exists: cancel the pending operator, restore `accumulator` display into `currentOperand`.
- If everything is empty: no-op.

### 3.5 Calendar button
- Emits an event to open the date picker (same as tapping the date field today).

## 4. Negative values

- Negative values are **only** accepted if the user explicitly types `-` as the **first character** when the expression is completely empty.
- Mid-expression subtraction producing a negative intermediate result is allowed (valid intermediate value).
- Only **division** producing a result `< 0` resets to `0`.

## 5. Display integration

### Input field layout

Shows the full expression with currency symbols before each operand:

```
    $ 25 + $ 25
```

- Each operand is prefixed with the **currency sign**.
- Operators are displayed inline between operands.
- When the input is empty, show `$ 0` with reduced opacity.
- After pressing `=`, the expression collapses to the result: `$ 50`.
- The input field is **read-only** — it is NOT a text field. No system keyboard, no cursor, no selection. All editing is done exclusively via the custom keyboard.

### Max value constraint
- Individual operands are capped at `9 999 999`.
- After evaluating an expression (`=` or chained operator), if the result exceeds `9 999 999`, it is clamped to `9 999 999`.
- Digit input is rejected if appending the digit would cause the current operand to exceed the max.
- The display should accommodate the longest possible expression (e.g. `$ 9999999 + $ 9999999`) — use auto-sizing text or horizontal scroll if needed.

## 6. Responsive layout

### Button sizing
- All buttons are **square** with **rounded corners** (`RoundedCornerShape`).
- Each button occupies equal width via `Modifier.weight(1f)` — height matches width (`aspectRatio(1f)`).
- **Save/= button** is the only exception: same width as other buttons (`1 weight`), but **2× height** (spans 2 rows vertically).
- Spacing between buttons is uniform (e.g. `6.dp` gap).
- Keyboard sits at the bottom of the screen; the scrollable form above gets remaining space via `weight`.
- Minimum touch target: **48dp** (Material guidelines).

## 7. Key type extensions

```kotlin
sealed interface NumericKey {
    data class Digit(val value: Int) : NumericKey
    data object Decimal : NumericKey          // the "," button
    data object Backspace : NumericKey
    data class Operator(val op: KeyboardOperator) : NumericKey
    data object Equals : NumericKey
    data object Calendar : NumericKey
    data object CurrencySymbol : NumericKey   // no-op or future use, displays currency sign
}
```

## 8. Integration map

| Current                                    | New                                                                 |
|--------------------------------------------|---------------------------------------------------------------------|
| `NumericAmountInput`                       | Replaced by `KeyboardCalculator`                                    |
| `NumericKey` (3 variants)                  | Extended with `Operator`, `Equals`, `Calendar`                      |
| `AddExpenseEvent.KeyPressed`               | Unchanged — still passes `NumericKey`                               |
| `BaseExpenseStateHolder.handleKeyPress`    | Delegates to `KeyboardCalculator`, which returns new display text   |
| `NumericKeyboard` composable               | Rewritten to 5-column layout                                       |
| Top-bar Save button                        | Removed — save moved into keyboard ✓ button                        |
| Date field tap                             | Also triggerable via keyboard 📅 button                            |

## 9. Unit tests

`KeyboardCalculator` is pure and synchronous — all tests instantiate it directly, call `processKey()`, and assert on the returned `KeyboardState`.

### Test categories

#### Digit input
- Single digit → `displayExpression` shows `$ 5`
- Multiple digits → `$ 123`
- Leading zero rejected (except `0.`)
- Max value `9 999 999` — digit rejected when exceeding

#### Decimal input
- `,` adds decimal point → `$ 0.`
- Second `,` ignored
- Respects `decimalPlaces` limit
- `,` when `decimalPlaces == 0` → ignored

#### Operators
- Digit then `+` → expression mode, display shows `$ 25 +`
- Chain: `5 + 3 +` → evaluates to `$ 8 +` (intermediate result)
- Operator switching: `5 + ×` → replaces `+` with `×`
- `-` as first key → negative prefix, display `- $`
- `-` when not first → normal subtraction

#### Equals
- `5 + 3 =` → `$ 8`, expression mode off
- `=` with no pending operator → no-op (save handled by state holder)
- Division result `< 0` → resets to `$ 0`
- Result exceeding max → clamped to `$ 9999999`

#### Backspace
- Remove last digit → `$ 12` → `$ 1`
- Empty operand with pending operator → cancels operator, restores accumulator
- Fully empty → no-op

#### Negative values
- `-` as first input then digits → `- $ 25`
- Mid-expression negative result from subtraction → allowed
- Division yielding `< 0` → reset to `0`

#### State restore
- `setFromAmount("25.50")` → `displayExpression` shows `$ 25.50`
- `parseToMinorUnits()` returns correct value after input and after `=`

#### Edge cases
- Rapid operator switching: `5 + - × ÷` → only last operator kept
- `=` immediately after operator (no second operand) → uses `0` as second operand or no-op
- Multiple `=` presses → idempotent
- Decimal only: `,` then digits → `$ 0.5`

### Test file
`KeyboardCalculatorTest.kt` in `commonTest`, same package as the class.

## 10. Files to change

1. **Delete** `NumericAmountInput.kt` → **Create** `KeyboardCalculator.kt` (same package).
2. **Rewrite** `NumericKeyboard.kt` — new 5-column layout with operators and actions.
3. **Update** `AddExpenseEvent.kt` — extend `NumericKey` with `Operator`, `Equals`, `Calendar`.
4. **Update** `BaseExpenseStateHolder.kt` — replace `numericInput` with `KeyboardCalculator`, update `handleKeyPress`, wire calendar and save events.
5. **Update** `AddExpenseScreen.kt` — remove top-bar save button, connect calendar key event, update `AmountDisplay` for expression mode.
