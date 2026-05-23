"""Unit tests for exportToSheets cloud function helpers."""

import base64
import gzip
import json
from datetime import date
from unittest.mock import MagicMock

import pytest

from main import (
    EPOCH,
    MAX_DECOMPRESSED_SIZE,
    _build_separate_tabs,
    _build_single_tab,
    _build_pivot_summary,
    _cell_data,
    _detect_months,
    _expense_row,
    _month_serial_range,
    _parse_expenses,
    _sanitize,
    _to_serial,
    _write_sheet,
)

# _MockHttpsError is registered in conftest.py
from firebase_functions.https_fn import HttpsError


def _get_batch_update_rows(ws):
    """Extract primitive row values from the updateCells request."""
    call = ws.spreadsheet.batch_update.call_args
    payload = call.args[0] if call.args else call.kwargs
    update_request = next(
        request["updateCells"]
        for request in payload["requests"]
        if "updateCells" in request
    )
    rows = []
    for row_data in update_request["rows"]:
        row = []
        for cell_data in row_data["values"]:
            value = cell_data.get("userEnteredValue", {})
            if "formulaValue" in value:
                row.append(value["formulaValue"])
            elif "numberValue" in value:
                row.append(value["numberValue"])
            elif "stringValue" in value:
                row.append(value["stringValue"])
            else:
                row.append("")
        rows.append(row)
    return rows


# ── _sanitize ────────────────────────────────────────────────────────────────


class TestSanitize:
    """Formula injection prevention."""

    def test_prefixes_equals(self):
        assert _sanitize("=SUM(A1)") == "'=SUM(A1)"

    def test_prefixes_plus(self):
        assert _sanitize("+1234") == "'+1234"

    def test_prefixes_minus(self):
        assert _sanitize("-1234") == "'-1234"

    def test_prefixes_at(self):
        assert _sanitize("@SUM(A1)") == "'@SUM(A1)"

    def test_normal_string_unchanged(self):
        assert _sanitize("Groceries") == "Groceries"

    def test_empty_string_unchanged(self):
        assert _sanitize("") == ""

    def test_number_as_string_unchanged(self):
        assert _sanitize("42.99") == "42.99"

    def test_non_string_passthrough(self):
        assert _sanitize(42) == 42

    def test_importrange_blocked(self):
        result = _sanitize('=IMPORTRANGE("url","Sheet1!A1")')
        assert result.startswith("'=")


# ── _to_serial ───────────────────────────────────────────────────────────────


class TestToSerial:
    """ISO date to Sheets serial number conversion."""

    def test_known_date(self):
        assert _to_serial("2024-01-01") == (date(2024, 1, 1) - EPOCH).days

    def test_epoch_date(self):
        assert _to_serial("1899-12-30") == 0

    def test_recent_date(self):
        assert _to_serial("2026-04-26") == (date(2026, 4, 26) - EPOCH).days

    def test_invalid_date_raises(self):
        with pytest.raises(ValueError):
            _to_serial("not-a-date")

    def test_empty_string_raises(self):
        with pytest.raises(ValueError):
            _to_serial("")


# ── _expense_row ─────────────────────────────────────────────────────────────


class TestExpenseRow:
    """Row building with sanitization and date conversion."""

    def test_normal_expense(self):
        expense = {
            "date": "2024-06-15",
            "title": "Coffee",
            "category": "Food",
            "subcategory": "Drinks",
            "amount": "4.50",
            "notes": "Morning coffee",
        }
        row = _expense_row(expense)
        assert row[0] == _to_serial("2024-06-15")
        assert row[1] == "Coffee"
        assert row[2] == "Food"
        assert row[3] == "Drinks"
        assert row[4] == "4.50"
        assert row[5] == "Morning coffee"

    def test_formula_injection_in_title(self):
        row = _expense_row({"title": "=CMD('calc')"})
        assert row[1] == "'=CMD('calc')"

    def test_formula_injection_in_category(self):
        row = _expense_row({"category": "+EVIL"})
        assert row[2] == "'+EVIL"

    def test_formula_injection_in_notes(self):
        row = _expense_row({"notes": "-cmd|'/C calc'!A0"})
        assert row[5] == "'-cmd|'/C calc'!A0"

    def test_invalid_date_falls_back_to_string(self):
        row = _expense_row({"date": "bad-date"})
        assert row[0] == "bad-date"

    def test_missing_fields_default(self):
        row = _expense_row({})
        assert row[0] == ""
        assert row[1] == ""
        assert row[2] == ""
        assert row[3] == ""
        assert row[4] == "0"
        assert row[5] == ""


# ── _parse_expenses ──────────────────────────────────────────────────────────


class TestParseExpenses:
    """Payload parsing: plain, compressed, and edge cases."""

    def test_plain_expenses(self):
        data = {"expenses": [{"title": "A"}, {"title": "B"}]}
        assert len(_parse_expenses(data, compressed=False)) == 2

    def test_empty_expenses_raises(self):
        with pytest.raises(HttpsError):
            _parse_expenses({"expenses": []}, compressed=False)

    def test_missing_expenses_raises(self):
        with pytest.raises(HttpsError):
            _parse_expenses({}, compressed=False)

    def test_compressed_expenses(self):
        expenses = [{"title": "Coffee", "amount": "3.50"}]
        payload = base64.b64encode(gzip.compress(json.dumps(expenses).encode())).decode()
        result = _parse_expenses({"expenses": payload}, compressed=True)
        assert len(result) == 1
        assert result[0]["title"] == "Coffee"

    def test_compressed_invalid_base64_raises(self):
        with pytest.raises(HttpsError):
            _parse_expenses({"expenses": "not-valid!!!"}, compressed=True)

    def test_compressed_too_large_raises(self):
        large = b"x" * (MAX_DECOMPRESSED_SIZE + 1)
        payload = base64.b64encode(gzip.compress(large)).decode()
        with pytest.raises(HttpsError):
            _parse_expenses({"expenses": payload}, compressed=True)


# ── _write_sheet ─────────────────────────────────────────────────────────────


class TestWriteSheet:
    """Spreadsheet row building and category summary."""

    @staticmethod
    def _make_ws():
        ws = MagicMock()
        ws.id = 0
        ws.spreadsheet = MagicMock()
        return ws

    @staticmethod
    def _get_rows(ws):
        return _get_batch_update_rows(ws)

    def test_category_labels_sanitized(self):
        ws = self._make_ws()
        expenses = [
            {"date": "2024-01-01", "category": "=EVIL", "amount": "10"},
            {"date": "2024-01-02", "category": "Food", "amount": "5"},
        ]
        headers = ["Date", "Title", "Category", "Subcategory", "Amount", "Notes"]

        _write_sheet(ws, expenses, headers, ["=EVIL", "Food"], "0.00")

        rows = self._get_rows(ws)
        first_cols = [r[0] for r in rows if r]
        assert "'=EVIL" in first_cols
        assert "Food" in first_cols

    def test_sumif_escapes_quotes(self):
        ws = self._make_ws()
        expenses = [{"date": "2024-01-01", "category": 'Food "Organic"', "amount": "10"}]
        headers = ["Date", "Title", "Category", "Subcategory", "Amount", "Notes"]

        _write_sheet(ws, expenses, headers, ['Food "Organic"'], "0.00")

        rows = self._get_rows(ws)
        # Find the SUMIF formula row for this category
        sumif_cells = [str(cell) for row in rows if row for cell in row if "SUMIF" in str(cell)]
        assert sumif_cells, "Expected a SUMIF formula"
        assert '\\"' in sumif_cells[0], f"Expected escaped quotes in: {sumif_cells[0]}"

    def test_total_row_formula(self):
        ws = self._make_ws()
        expenses = [
            {"date": "2024-01-01", "amount": "10"},
            {"date": "2024-01-02", "amount": "20"},
        ]
        headers = ["Date", "Title", "Category", "Subcategory", "Amount", "Notes"]

        _write_sheet(ws, expenses, headers, [], "0.00")

        rows = self._get_rows(ws)
        # Row 0 = headers, rows 1-2 = expenses, row 3 = total
        total_row = rows[3]
        assert total_row[4] == "=SUM(E2:E3)"

    def test_category_summary_present(self):
        """Category summary section should list all passed categories."""
        ws = self._make_ws()
        expenses = [
            {"date": "2024-01-01", "category": "Food", "amount": "10"},
            {"date": "2024-01-02", "category": "Transport", "amount": "5"},
        ]
        headers = ["Date", "Title", "Category", "Subcategory", "Amount", "Notes"]

        _write_sheet(ws, expenses, headers, ["Food", "Transport"], "0.00")

        rows = self._get_rows(ws)
        first_cols = [r[0] for r in rows if r]
        assert "Food" in first_cols
        assert "Transport" in first_cols
        assert "Category Summary" in first_cols

    def test_subcategory_breakdown(self):
        """Subcategories should appear indented under their parent category."""
        ws = self._make_ws()
        expenses = [
            {"date": "2024-01-01", "category": "Food", "subcategory": "Groceries", "amount": "50"},
            {"date": "2024-01-02", "category": "Food", "subcategory": "Drinks", "amount": "20"},
            {"date": "2024-01-03", "category": "Transport", "subcategory": "Fuel", "amount": "30"},
            {"date": "2024-01-04", "category": "Transport", "subcategory": "", "amount": "10"},
        ]
        headers = ["Date", "Title", "Category", "Subcategory", "Amount", "Notes"]

        _write_sheet(ws, expenses, headers, ["Food", "Transport"], "0.00")

        rows = self._get_rows(ws)
        first_cols = [r[0] for r in rows if r]

        # Parent categories present
        assert "Food" in first_cols
        assert "Transport" in first_cols

        # Subcategories indented with 2 spaces
        assert "  Drinks" in first_cols
        assert "  Groceries" in first_cols
        assert "  Fuel" in first_cols

        # Empty subcategory not included
        assert "  " not in first_cols

    def test_subcategory_uses_sumifs(self):
        """Subcategory rows should use SUMIFS with both category and subcategory criteria."""
        ws = self._make_ws()
        expenses = [
            {"date": "2024-01-01", "category": "Food", "subcategory": "Groceries", "amount": "50"},
        ]
        headers = ["Date", "Title", "Category", "Subcategory", "Amount", "Notes"]

        _write_sheet(ws, expenses, headers, ["Food"], "0.00")

        rows = self._get_rows(ws)
        sumifs_cells = [str(cell) for row in rows if row for cell in row if "SUMIFS" in str(cell)]
        assert sumifs_cells, "Expected a SUMIFS formula for subcategory"
        assert "Food" in sumifs_cells[0]
        assert "Groceries" in sumifs_cells[0]

    def test_subcategory_sanitized(self):
        """Subcategory names with formula prefixes should be sanitized."""
        ws = self._make_ws()
        expenses = [
            {"date": "2024-01-01", "category": "Food", "subcategory": "=EVIL", "amount": "10"},
        ]
        headers = ["Date", "Title", "Category", "Subcategory", "Amount", "Notes"]

        _write_sheet(ws, expenses, headers, ["Food"], "0.00")

        rows = self._get_rows(ws)
        first_cols = [r[0] for r in rows if r]
        assert "  '=EVIL" in first_cols

    def test_formula_cell_data_uses_formula_value(self):
        assert _cell_data("=SUM(E2:E3)", 4) == {
            "userEnteredValue": {"formulaValue": "=SUM(E2:E3)"}
        }

    def test_amount_cell_data_uses_number_value(self):
        assert _cell_data("12.34", 4) == {
            "userEnteredValue": {"numberValue": 12.34}
        }

    def test_single_tab_uses_one_spreadsheet_batch(self):
        spreadsheet = MagicMock()
        spreadsheet.sheet1.id = 0

        _build_single_tab(
            spreadsheet=spreadsheet,
            expenses=[{"date": "2024-01-01", "category": "Food", "amount": "10"}],
            headers=["Date", "Title", "Category", "Subcategory", "Amount", "Notes"],
            all_categories=["Food"],
            num_fmt="0.00",
            label="Jan 2024",
        )

        spreadsheet.batch_update.assert_called_once()
        requests = spreadsheet.batch_update.call_args.args[0]["requests"]
        assert any("updateCells" in request for request in requests)

    def test_separate_tabs_uses_one_spreadsheet_batch(self):
        spreadsheet = MagicMock()
        spreadsheet.sheet1.id = 0

        _build_separate_tabs(
            spreadsheet=spreadsheet,
            expenses=[
                {"date": "2024-01-01", "category": "Food", "amount": "10"},
                {"date": "2024-02-01", "category": "Food", "amount": "20"},
            ],
            headers=["Date", "Title", "Category", "Subcategory", "Amount", "Notes"],
            all_categories=["Food"],
            num_fmt="0.00",
        )

        spreadsheet.batch_update.assert_called_once()
        requests = spreadsheet.batch_update.call_args.args[0]["requests"]
        assert sum(1 for request in requests if "updateCells" in request) == 2
        assert sum(1 for request in requests if "addSheet" in request) == 1


# ── Pivot summary ────────────────────────────────────────────────────────────


class TestMonthHelpers:

    def test_detect_months(self):
        expenses = [
            {"date": "2024-01-15"},
            {"date": "2024-01-20"},
            {"date": "2024-03-05"},
        ]
        months = _detect_months(expenses)
        assert len(months) == 2
        assert months[0][0] == "Jan 2024"
        assert months[1][0] == "Mar 2024"

    def test_detect_months_single(self):
        expenses = [{"date": "2024-06-01"}, {"date": "2024-06-15"}]
        months = _detect_months(expenses)
        assert len(months) == 1
        assert months[0][0] == "Jun 2024"

    def test_month_serial_range(self):
        start, end = _month_serial_range(2024, 1)
        assert start == _to_serial("2024-01-01")
        assert end == _to_serial("2024-02-01")

    def test_month_serial_range_december(self):
        start, end = _month_serial_range(2024, 12)
        assert start == _to_serial("2024-12-01")
        assert end == _to_serial("2025-01-01")


class TestPivotSummary:

    def test_pivot_header_has_months_and_total(self):
        ws = MagicMock()
        ws.id = 0
        ws.spreadsheet = MagicMock()

        expenses = [
            {"date": "2024-01-10", "category": "Food", "subcategory": "Groceries", "amount": "50"},
            {"date": "2024-02-15", "category": "Food", "subcategory": "Drinks", "amount": "20"},
            {"date": "2024-03-01", "category": "Transport", "amount": "30"},
        ]
        headers = ["Date", "Title", "Category", "Subcategory", "Amount", "Notes"]

        _write_sheet(ws, expenses, headers, ["Food", "Transport"], "0.00", pivot=True)

        rows = _get_batch_update_rows(ws)

        # Find the header row of the summary (contains "Category")
        summary_header = None
        for r in rows:
            if r and r[0] == "Category":
                summary_header = r
                break

        assert summary_header is not None
        assert "Jan 2024" in summary_header
        assert "Feb 2024" in summary_header
        assert "Mar 2024" in summary_header
        assert "Total" in summary_header

    def test_pivot_category_row_has_sumifs_per_month(self):
        ws = MagicMock()
        ws.id = 0
        ws.spreadsheet = MagicMock()

        expenses = [
            {"date": "2024-01-10", "category": "Food", "amount": "50"},
            {"date": "2024-02-15", "category": "Food", "amount": "20"},
        ]
        headers = ["Date", "Title", "Category", "Subcategory", "Amount", "Notes"]

        _write_sheet(ws, expenses, headers, ["Food"], "0.00", pivot=True)

        rows = _get_batch_update_rows(ws)

        # Find the Food category row
        food_row = None
        for r in rows:
            if r and r[0] == "Food":
                food_row = r
                break

        assert food_row is not None
        # Should have: label + 2 month SUMIFS + 1 total SUMIF
        assert len(food_row) == 4
        assert "SUMIFS" in str(food_row[1])  # Jan
        assert "SUMIFS" in str(food_row[2])  # Feb
        assert "SUMIF" in str(food_row[3])   # Total

    def test_pivot_subcategory_row(self):
        ws = MagicMock()
        ws.id = 0
        ws.spreadsheet = MagicMock()

        expenses = [
            {"date": "2024-01-10", "category": "Food", "subcategory": "Groceries", "amount": "50"},
            {"date": "2024-02-15", "category": "Food", "subcategory": "Groceries", "amount": "20"},
        ]
        headers = ["Date", "Title", "Category", "Subcategory", "Amount", "Notes"]

        _write_sheet(ws, expenses, headers, ["Food"], "0.00", pivot=True)

        rows = _get_batch_update_rows(ws)

        # Find indented subcategory row
        sub_row = None
        for r in rows:
            if r and str(r[0]).strip() == "Groceries":
                sub_row = r
                break

        assert sub_row is not None
        assert sub_row[0] == "  Groceries"
        # All cells should reference both category and subcategory
        for cell in sub_row[1:]:
            assert "Food" in str(cell)
            assert "Groceries" in str(cell)

    def test_single_month_uses_simple_summary(self):
        """When only one month, pivot=True should still use simple summary."""
        ws = MagicMock()
        ws.id = 0
        ws.spreadsheet = MagicMock()

        expenses = [
            {"date": "2024-01-10", "category": "Food", "amount": "50"},
            {"date": "2024-01-20", "category": "Food", "amount": "20"},
        ]
        headers = ["Date", "Title", "Category", "Subcategory", "Amount", "Notes"]

        _write_sheet(ws, expenses, headers, ["Food"], "0.00", pivot=True)

        rows = _get_batch_update_rows(ws)

        # Summary header should be simple (Category + Amount), no month columns
        summary_header = None
        for r in rows:
            if r and r[0] == "Category":
                summary_header = r
                break

        assert summary_header is not None
        assert len(summary_header) == 2  # Category + Amount, no months
