"""Unit tests for exportToSheets cloud function helpers."""

import base64
import gzip
import json
from datetime import date, datetime, timezone
from unittest.mock import MagicMock

import pytest
from cryptography.fernet import Fernet

import main
from main import (
    EPOCH,
    MAX_DECOMPRESSED_SIZE,
    _build_separate_tabs,
    _build_single_tab,
    _build_pivot_summary,
    _cell_data,
    _claim_export,
    _detect_months,
    _decrypt_refresh_token,
    _encrypt_refresh_token,
    _expense_row,
    _month_serial_range,
    _parse_expenses,
    _resolve_drive_folder,
    _Formula,
    _sanitize,
    _to_serial,
    _validate_export_request,
    _write_sheet,
)

# _MockHttpsError is registered in conftest.py
from firebase_functions.https_fn import HttpsError


@pytest.fixture(autouse=True)
def bypass_firestore_rate_limits(monkeypatch):
    monkeypatch.setattr(main, "_check_daily_limit", MagicMock())


class TestGoogleOAuthStorage:
    def setup_method(self):
        main.GOOGLE_OAUTH_CLIENT_ID = MagicMock()
        main.GOOGLE_OAUTH_CLIENT_SECRET = MagicMock()
        main.GOOGLE_TOKEN_ENCRYPTION_KEY = MagicMock()
        main.GOOGLE_TOKEN_ENCRYPTION_KEY.value = Fernet.generate_key().decode()

    def test_refresh_token_is_encrypted_at_rest(self):
        encrypted = _encrypt_refresh_token("refresh-token")

        assert encrypted != "refresh-token"
        assert _decrypt_refresh_token(encrypted) == "refresh-token"

    def test_link_stores_encrypted_refresh_token(self, monkeypatch):
        main.GOOGLE_OAUTH_CLIENT_ID.value = "client-id"
        main.GOOGLE_OAUTH_CLIENT_SECRET.value = "client-secret"
        response = MagicMock(ok=True)
        response.json.return_value = {
            "refresh_token": "refresh-token",
            "scope": f"{main.GOOGLE_SHEETS_SCOPE} {main.GOOGLE_DRIVE_FILE_SCOPE}",
        }
        monkeypatch.setattr(main.requests, "post", MagicMock(return_value=response))
        document = MagicMock()
        document.get.return_value.exists = False
        monkeypatch.setattr(main, "_oauth_document", MagicMock(return_value=document))
        request = MagicMock(data={"authorizationCode": "one-time-code"})
        request.auth.uid = "firebase-uid"

        assert main.linkGoogleAccount(request) == {"linked": True}
        stored = document.set.call_args.args[0]
        assert stored[main.ENCRYPTED_REFRESH_TOKEN_FIELD] != "refresh-token"
        assert _decrypt_refresh_token(stored[main.ENCRYPTED_REFRESH_TOKEN_FIELD]) == "refresh-token"
        assert set(stored["scopes"]) == main.REQUIRED_GOOGLE_SCOPES
        assert "createdAt" in stored
        assert document.set.call_args.kwargs == {}

    def test_link_rejects_missing_scope_without_overwriting_existing_grant(self, monkeypatch):
        response = MagicMock(ok=True)
        response.json.return_value = {
            "refresh_token": "new-refresh-token",
            "scope": main.GOOGLE_SHEETS_SCOPE,
        }
        monkeypatch.setattr(main.requests, "post", MagicMock(return_value=response))
        document = MagicMock()
        monkeypatch.setattr(main, "_oauth_document", MagicMock(return_value=document))
        request = MagicMock(data={"authorizationCode": "one-time-code"})
        request.auth.uid = "firebase-uid"

        with pytest.raises(HttpsError) as error:
            main.linkGoogleAccount(request)

        assert error.value.details == {"reason": main.GOOGLE_SCOPES_MISSING}
        document.set.assert_not_called()

    def test_link_rejects_missing_refresh_token(self, monkeypatch):
        response = MagicMock(ok=True)
        response.json.return_value = {
            "scope": f"{main.GOOGLE_SHEETS_SCOPE} {main.GOOGLE_DRIVE_FILE_SCOPE}",
        }
        monkeypatch.setattr(main.requests, "post", MagicMock(return_value=response))
        document = MagicMock()
        monkeypatch.setattr(main, "_oauth_document", MagicMock(return_value=document))
        request = MagicMock(data={"authorizationCode": "one-time-code"})
        request.auth.uid = "firebase-uid"

        with pytest.raises(HttpsError) as error:
            main.linkGoogleAccount(request)

        assert error.value.details == {"reason": main.GOOGLE_REFRESH_TOKEN_MISSING}
        document.set.assert_not_called()

    def test_link_requires_firebase_auth_before_token_exchange(self, monkeypatch):
        post = MagicMock()
        monkeypatch.setattr(main.requests, "post", post)
        request = MagicMock(data={"authorizationCode": "one-time-code"})
        request.auth = None

        with pytest.raises(HttpsError) as error:
            main.linkGoogleAccount(request)

        assert error.value.details == {"reason": main.FIREBASE_SIGN_IN_REQUIRED}
        post.assert_not_called()

    def test_link_reports_missing_authorization_code(self):
        request = MagicMock(data={})
        request.auth.uid = "firebase-uid"

        with pytest.raises(HttpsError) as error:
            main.linkGoogleAccount(request)

        assert error.value.details == {"reason": main.GOOGLE_AUTH_CODE_MISSING}

    def test_link_maps_token_endpoint_outage_without_logging_body(self, monkeypatch, caplog):
        response = MagicMock(ok=False, status_code=503)
        response.text = "sensitive-provider-response"
        monkeypatch.setattr(main.requests, "post", MagicMock(return_value=response))
        request = MagicMock(data={"authorizationCode": "one-time-code"})
        request.auth.uid = "firebase-uid"

        with pytest.raises(HttpsError) as error:
            main.linkGoogleAccount(request)

        assert error.value.details == {"reason": main.GOOGLE_TOKEN_ENDPOINT_UNAVAILABLE}
        assert "sensitive-provider-response" not in caplog.text

    def test_link_status_accepts_encrypted_ciphertext_field(self, monkeypatch):
        snapshot = MagicMock(exists=True)
        snapshot.to_dict.return_value = {
            main.ENCRYPTED_REFRESH_TOKEN_FIELD: "ciphertext",
            "scopes": list(main.REQUIRED_GOOGLE_SCOPES),
        }
        document = MagicMock()
        document.get.return_value = snapshot
        monkeypatch.setattr(main, "_oauth_document", MagicMock(return_value=document))
        request = MagicMock()
        request.auth.uid = "firebase-uid"

        assert main.hasGoogleAccountLink(request) == {"linked": True}

    def test_link_status_does_not_accept_development_legacy_field(self, monkeypatch):
        snapshot = MagicMock(exists=True)
        snapshot.to_dict.return_value = {
            "refreshToken": "development-only-ciphertext",
            "scopes": list(main.REQUIRED_GOOGLE_SCOPES),
        }
        document = MagicMock()
        document.get.return_value = snapshot
        monkeypatch.setattr(main, "_oauth_document", MagicMock(return_value=document))
        request = MagicMock()
        request.auth.uid = "firebase-uid"

        assert main.hasGoogleAccountLink(request) == {"linked": False}

    def test_expired_refresh_token_removes_link(self, monkeypatch):
        encrypted = _encrypt_refresh_token("expired-refresh-token")
        snapshot = MagicMock(exists=True)
        snapshot.to_dict.return_value = {
            main.ENCRYPTED_REFRESH_TOKEN_FIELD: encrypted,
            "scopes": list(main.REQUIRED_GOOGLE_SCOPES),
        }
        document = MagicMock()
        document.get.return_value = snapshot
        monkeypatch.setattr(main, "_oauth_document", MagicMock(return_value=document))
        credentials = MagicMock()
        credentials.refresh.side_effect = main.RefreshError("invalid_grant")
        monkeypatch.setattr(main, "Credentials", MagicMock(return_value=credentials))

        with pytest.raises(HttpsError) as error:
            main._google_credentials("firebase-uid")

        assert error.value.details == {"reason": main.GOOGLE_RECONNECT_REQUIRED}
        document.delete.assert_called_once_with()

    def test_transient_refresh_failure_keeps_link(self, monkeypatch):
        encrypted = _encrypt_refresh_token("refresh-token")
        snapshot = MagicMock(exists=True)
        snapshot.to_dict.return_value = {
            main.ENCRYPTED_REFRESH_TOKEN_FIELD: encrypted,
            "scopes": list(main.REQUIRED_GOOGLE_SCOPES),
        }
        document = MagicMock()
        document.get.return_value = snapshot
        monkeypatch.setattr(main, "_oauth_document", MagicMock(return_value=document))
        credentials = MagicMock()
        credentials.refresh.side_effect = main.RefreshError("temporarily unavailable")
        monkeypatch.setattr(main, "Credentials", MagicMock(return_value=credentials))

        with pytest.raises(HttpsError) as error:
            main._google_credentials("firebase-uid")

        assert error.value.details == {"reason": main.GOOGLE_TOKEN_ENDPOINT_UNAVAILABLE}
        document.delete.assert_not_called()

    def test_unlink_deletes_undecryptable_grant(self, monkeypatch):
        snapshot = MagicMock(exists=True)
        snapshot.to_dict.return_value = {
            main.ENCRYPTED_REFRESH_TOKEN_FIELD: "not-fernet-ciphertext",
            "scopes": list(main.REQUIRED_GOOGLE_SCOPES),
        }
        document = MagicMock()
        document.get.return_value = snapshot
        monkeypatch.setattr(main, "_oauth_document", MagicMock(return_value=document))
        request = MagicMock()
        request.auth.uid = "firebase-uid"

        assert main.unlinkGoogleAccount(request) == {"linked": False}
        document.delete.assert_called_once_with()

    def test_unlink_posts_refresh_token_in_request_body(self, monkeypatch):
        encrypted = _encrypt_refresh_token("refresh-token")
        snapshot = MagicMock(exists=True)
        snapshot.to_dict.return_value = {
            main.ENCRYPTED_REFRESH_TOKEN_FIELD: encrypted,
            "scopes": list(main.REQUIRED_GOOGLE_SCOPES),
        }
        document = MagicMock()
        document.get.return_value = snapshot
        monkeypatch.setattr(main, "_oauth_document", MagicMock(return_value=document))
        post = MagicMock(return_value=MagicMock(ok=True))
        monkeypatch.setattr(main.requests, "post", post)
        request = MagicMock()
        request.auth.uid = "firebase-uid"

        main.unlinkGoogleAccount(request)

        assert post.call_args.kwargs["data"] == {"token": "refresh-token"}
        assert "params" not in post.call_args.kwargs

    def test_export_requires_firebase_auth_before_google_access(self, monkeypatch):
        google_credentials = MagicMock()
        monkeypatch.setattr(main, "_google_credentials", google_credentials)
        request = MagicMock(data={"expenses": [{"title": "Coffee"}]})
        request.auth = None

        with pytest.raises(HttpsError) as error:
            main.exportToSheets(request)

        assert error.value.details == {"reason": main.FIREBASE_SIGN_IN_REQUIRED}
        google_credentials.assert_not_called()

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
        data = {
            "expenses": [
                {"date": "2026-01-01", "title": "A", "amount": 1.0},
                {"date": "2026-01-02", "title": "B", "amount": 2.0},
            ]
        }
        assert len(_parse_expenses(data, compressed=False)) == 2

    def test_empty_expenses_raises(self):
        with pytest.raises(HttpsError):
            _parse_expenses({"expenses": []}, compressed=False)

    def test_missing_expenses_raises(self):
        with pytest.raises(HttpsError):
            _parse_expenses({}, compressed=False)

    def test_compressed_expenses(self):
        expenses = [{"date": "2026-01-01", "title": "Coffee", "amount": 3.50}]
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


class TestValidateExportRequest:
    def test_normalizes_valid_request(self):
        result = _validate_export_request(
            {
                "exportId": "42",
                "dateRangeLabel": "January 2026",
                "tabLayout": "single_tab",
                "decimalPlaces": 2,
                "title": "January expenses",
                "folderName": "PlzStop exports",
                "expenses": [
                    {
                        "date": "2026-01-01",
                        "title": "Coffee",
                        "category": "Food",
                        "subcategory": "",
                        "amount": 3.5,
                        "notes": "",
                    }
                ],
            }
        )

        assert result["exportId"] == "42"
        assert result["expenses"][0]["amount"] == 3.5
        assert result["title"] == "January expenses"
        assert result["folderName"] == "PlzStop exports"

    def test_blank_folder_name_uses_drive_root(self):
        result = _validate_export_request(
            {
                "exportId": "42",
                "folderName": "   ",
                "expenses": [
                    {"date": "2026-01-01", "title": "Coffee", "amount": 3.5}
                ],
            }
        )

        assert result["folderName"] is None

    def test_requires_idempotency_key(self):
        with pytest.raises(HttpsError) as error:
            _validate_export_request(
                {
                    "expenses": [
                        {"date": "2026-01-01", "title": "Coffee", "amount": 3.5}
                    ]
                }
            )

        assert error.value.details == {"reason": main.EXPORT_ID_REQUIRED}

    def test_rejects_formula_amount(self):
        with pytest.raises(HttpsError):
            _validate_export_request(
                {
                    "exportId": "42",
                    "expenses": [
                        {
                            "date": "2026-01-01",
                            "title": "Coffee",
                            "amount": '=IMPORTDATA("https://example.com")',
                        }
                    ],
                }
            )

    def test_rejects_invalid_layout(self):
        with pytest.raises(HttpsError):
            _validate_export_request(
                {
                    "exportId": "42",
                    "tabLayout": "unknown",
                    "expenses": [
                        {"date": "2026-01-01", "title": "Coffee", "amount": 3.5}
                    ],
                }
            )


class TestDriveFolder:
    def test_reuses_existing_exact_name_folder(self):
        gc = MagicMock()
        response = MagicMock()
        response.json.return_value = {
            "files": [{"id": "existing-folder", "name": "PlzStop exports"}]
        }
        gc.http_client.request.return_value = response

        result = _resolve_drive_folder(gc, "PlzStop exports")

        assert result == "existing-folder"
        gc.http_client.request.assert_called_once()

    def test_creates_folder_when_no_match_exists(self):
        gc = MagicMock()
        search_response = MagicMock()
        search_response.json.return_value = {"files": []}
        create_response = MagicMock()
        create_response.json.return_value = {"id": "new-folder"}
        gc.http_client.request.side_effect = [search_response, create_response]

        result = _resolve_drive_folder(gc, "PlzStop exports")

        assert result == "new-folder"
        create_call = gc.http_client.request.call_args_list[1]
        assert create_call.args[:2] == ("post", main.DRIVE_FILES_API_V3_URL)
        assert create_call.kwargs["json"] == {
            "name": "PlzStop exports",
            "mimeType": "application/vnd.google-apps.folder",
        }


class TestExportIdempotency:
    def test_completed_export_returns_existing_url(self, monkeypatch):
        snapshot = MagicMock(exists=True)
        snapshot.to_dict.return_value = {
            "status": "success",
            "spreadsheetUrl": "https://docs.google.com/spreadsheets/existing",
        }
        document = MagicMock()
        document.get.return_value = snapshot
        transaction = MagicMock()
        monkeypatch.setattr(main, "_export_document", MagicMock(return_value=document))
        main.firestore.client.return_value.transaction.return_value = transaction

        result = _claim_export(uid="firebase-uid", export_id="42")

        assert result == "https://docs.google.com/spreadsheets/existing"
        transaction.set.assert_not_called()

    def test_active_export_returns_structured_in_progress_error(self, monkeypatch):
        snapshot = MagicMock(exists=True)
        snapshot.to_dict.return_value = {
            "status": "processing",
            "startedAt": datetime.now(timezone.utc),
        }
        document = MagicMock()
        document.get.return_value = snapshot
        transaction = MagicMock()
        monkeypatch.setattr(main, "_export_document", MagicMock(return_value=document))
        main.firestore.client.return_value.transaction.return_value = transaction

        with pytest.raises(HttpsError) as error:
            _claim_export(uid="firebase-uid", export_id="42")

        assert error.value.details == {"reason": main.EXPORT_IN_PROGRESS}
        transaction.set.assert_not_called()


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
        assert '""' in sumif_cells[0], f"Expected escaped quotes in: {sumif_cells[0]}"

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
        assert _cell_data(_Formula("=SUM(E2:E3)"), 4) == {
            "userEnteredValue": {"formulaValue": "=SUM(E2:E3)"}
        }

    def test_untrusted_formula_string_is_written_as_text(self):
        assert _cell_data('=IMPORTDATA("https://example.com")', 4) == {
            "userEnteredValue": {
                "stringValue": '=IMPORTDATA("https://example.com")'
            }
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
