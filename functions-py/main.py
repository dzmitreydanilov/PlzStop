"""Cloud Function: exportToSheets

Creates a Google Spreadsheet from expense data and notifies the user via FCM.
Deployed as Firebase Gen 2 HTTPS Callable in europe-west1.
"""

from __future__ import annotations

import base64
import gzip
import hashlib
import io
import json
import logging
import math
import re
import time
from collections import defaultdict
from datetime import date, datetime, timedelta, timezone

import gspread
import requests
from cryptography.fernet import Fernet, InvalidToken
from firebase_admin import firestore, initialize_app
from firebase_functions import https_fn, logger as functions_logger, options
from firebase_functions.params import SecretParam
from gspread.exceptions import APIError
from gspread.urls import DRIVE_FILES_API_V3_URL
from google.auth.exceptions import RefreshError, TransportError
from google.auth.transport.requests import Request as GoogleAuthRequest
from google.oauth2.credentials import Credentials

initialize_app()

logger = logging.getLogger(__name__)

GOOGLE_OAUTH_CLIENT_ID = SecretParam("GOOGLE_OAUTH_CLIENT_ID")
GOOGLE_OAUTH_CLIENT_SECRET = SecretParam("GOOGLE_OAUTH_CLIENT_SECRET")
GOOGLE_TOKEN_ENCRYPTION_KEY = SecretParam("GOOGLE_TOKEN_ENCRYPTION_KEY")
GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token"
GOOGLE_REVOKE_URI = "https://oauth2.googleapis.com/revoke"
GOOGLE_OAUTH_COLLECTION = "googleOAuthAccounts"
GOOGLE_SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"
GOOGLE_DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
REQUIRED_GOOGLE_SCOPES = frozenset({GOOGLE_SHEETS_SCOPE, GOOGLE_DRIVE_FILE_SCOPE})

ENCRYPTED_REFRESH_TOKEN_FIELD = "encryptedRefreshToken"

FIREBASE_SIGN_IN_REQUIRED = "FIREBASE_SIGN_IN_REQUIRED"
GOOGLE_AUTH_CODE_MISSING = "GOOGLE_AUTH_CODE_MISSING"
GOOGLE_REFRESH_TOKEN_MISSING = "GOOGLE_REFRESH_TOKEN_MISSING"
GOOGLE_RECONNECT_REQUIRED = "GOOGLE_RECONNECT_REQUIRED"
GOOGLE_SCOPES_MISSING = "GOOGLE_SCOPES_MISSING"
GOOGLE_TOKEN_ENDPOINT_UNAVAILABLE = "GOOGLE_TOKEN_ENDPOINT_UNAVAILABLE"
EXPORT_IN_PROGRESS = "EXPORT_IN_PROGRESS"
EXPORT_ID_REQUIRED = "EXPORT_ID_REQUIRED"
SHEETS_TEMPORARILY_UNAVAILABLE = "SHEETS_TEMPORARILY_UNAVAILABLE"

EPOCH = date(1899, 12, 30)
MAX_DECOMPRESSED_SIZE = 2 * 1024 * 1024
MAX_EXPENSES = 5_000
MAX_DECIMAL_PLACES = 10
MAX_EXPORT_ID_LENGTH = 128
MAX_TITLE_LENGTH = 200
MAX_FOLDER_NAME_LENGTH = 100
MAX_LABEL_LENGTH = 100
MAX_CELL_TEXT_LENGTH = 500
MAX_CURRENCY_SYMBOL_LENGTH = 10
MAX_FCM_TOKEN_LENGTH = 4_096
MAX_LINKS_PER_DAY = 5
MAX_STATUS_CHECKS_PER_DAY = 60
MAX_UNLINKS_PER_DAY = 10
MAX_EXPORTS_PER_DAY = 10
MAX_SHEETS_ATTEMPTS = 3
EXPORT_LEASE_DURATION = timedelta(minutes=10)
_EXPORT_ID = re.compile(r"^[A-Za-z0-9_-]+$")
# Sheets formula injection: strings starting with these are dangerous in cells
_FORMULA_PREFIX = re.compile(r"^[=+\-@]")


class _Formula:
    __slots__ = ("value",)

    def __init__(self, value: str):
        self.value = value

    value: str


def _require_uid(req: https_fn.CallableRequest) -> str:
    if req.auth is None:
        raise _callable_error(
            code=https_fn.FunctionsErrorCode.UNAUTHENTICATED,
            reason=FIREBASE_SIGN_IN_REQUIRED,
            message="Authentication required.",
        )
    return req.auth.uid


def _callable_error(code, reason: str, message: str) -> https_fn.HttpsError:
    return https_fn.HttpsError(
        code=code,
        message=message,
        details={"reason": reason},
    )


def _invalid_argument(message: str) -> https_fn.HttpsError:
    return https_fn.HttpsError(
        code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
        message=message,
    )


def _check_daily_limit(uid: str, operation: str, maximum: int) -> None:
    now = datetime.now(timezone.utc)
    day = now.date().isoformat()
    document_id = hashlib.sha256(f"{uid}:{operation}:{day}".encode()).hexdigest()
    document = firestore.client().collection("callableRateLimits").document(document_id)
    transaction = firestore.client().transaction()

    @firestore.transactional
    def increment(transaction):
        snapshot = document.get(transaction=transaction)
        count = (snapshot.to_dict() or {}).get("count", 0) if snapshot.exists else 0
        if not isinstance(count, int) or count >= maximum:
            raise https_fn.HttpsError(
                code=https_fn.FunctionsErrorCode.RESOURCE_EXHAUSTED,
                message="Daily request limit reached.",
            )
        transaction.set(
            document,
            {
                "count": count + 1,
                "operation": operation,
                "expiresAt": now + timedelta(days=2),
            },
        )

    increment(transaction)


def _oauth_document(uid: str):
    return firestore.client().collection(GOOGLE_OAUTH_COLLECTION).document(uid)


def _encrypt_refresh_token(refresh_token: str) -> str:
    return Fernet(GOOGLE_TOKEN_ENCRYPTION_KEY.value).encrypt(refresh_token.encode()).decode()


def _decrypt_refresh_token(encrypted_token: str) -> str:
    try:
        return Fernet(GOOGLE_TOKEN_ENCRYPTION_KEY.value).decrypt(encrypted_token.encode()).decode()
    except (InvalidToken, ValueError) as error:
        raise _callable_error(
            code=https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
            reason=GOOGLE_RECONNECT_REQUIRED,
            message="Google Sheets must be reconnected.",
        ) from error


def _normalize_scopes(scopes: object) -> set[str]:
    if isinstance(scopes, str):
        return set(scopes.split())
    if isinstance(scopes, (list, tuple, set, frozenset)):
        return {str(scope) for scope in scopes}
    return set()


def _has_required_scopes(scopes: object) -> bool:
    return REQUIRED_GOOGLE_SCOPES.issubset(_normalize_scopes(scopes))


def _encrypted_refresh_token(record: dict) -> str | None:
    return record.get(ENCRYPTED_REFRESH_TOKEN_FIELD)


def _is_permanent_refresh_error(error: RefreshError) -> bool:
    for detail in error.args:
        if isinstance(detail, dict) and detail.get("error") == "invalid_grant":
            return True
        if "invalid_grant" in str(detail).lower():
            return True
    return False


def _token_endpoint_unavailable() -> https_fn.HttpsError:
    return _callable_error(
        code=https_fn.FunctionsErrorCode.UNAVAILABLE,
        reason=GOOGLE_TOKEN_ENDPOINT_UNAVAILABLE,
        message="Google authorization is temporarily unavailable.",
    )


@https_fn.on_call(
    region="europe-west1",
    enforce_app_check=True,
    max_instances=5,
    concurrency=20,
    secrets=[GOOGLE_OAUTH_CLIENT_ID, GOOGLE_OAUTH_CLIENT_SECRET, GOOGLE_TOKEN_ENCRYPTION_KEY],
)
def linkGoogleAccount(req: https_fn.CallableRequest):  # noqa: N802
    uid = _require_uid(req)
    authorization_code = req.data.get("authorizationCode")
    if not authorization_code:
        raise _callable_error(
            code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            reason=GOOGLE_AUTH_CODE_MISSING,
            message="Missing Google authorization code.",
        )
    if not isinstance(authorization_code, str) or len(authorization_code) > 4_096:
        raise _invalid_argument("Invalid Google authorization code.")
    _check_daily_limit(uid=uid, operation="link", maximum=MAX_LINKS_PER_DAY)

    try:
        response = requests.post(
            GOOGLE_TOKEN_URI,
            data={
                "code": authorization_code,
                "client_id": GOOGLE_OAUTH_CLIENT_ID.value,
                "client_secret": GOOGLE_OAUTH_CLIENT_SECRET.value,
                "grant_type": "authorization_code",
                "redirect_uri": "",
            },
            timeout=15,
        )
    except requests.RequestException as error:
        raise _token_endpoint_unavailable() from error

    if not response.ok:
        logger.warning("Google authorization-code exchange failed: %s", response.status_code)
        if response.status_code == 429 or response.status_code >= 500:
            raise _token_endpoint_unavailable()
        raise _callable_error(
            code=https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
            reason=GOOGLE_RECONNECT_REQUIRED,
            message="Google Sheets must be reconnected.",
        )

    try:
        token_data = response.json()
    except ValueError as error:
        raise _token_endpoint_unavailable() from error

    refresh_token = token_data.get("refresh_token")
    if not refresh_token:
        raise _callable_error(
            code=https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
            reason=GOOGLE_REFRESH_TOKEN_MISSING,
            message="Google did not provide offline access.",
        )

    scopes = _normalize_scopes(token_data.get("scope"))
    if not REQUIRED_GOOGLE_SCOPES.issubset(scopes):
        raise _callable_error(
            code=https_fn.FunctionsErrorCode.PERMISSION_DENIED,
            reason=GOOGLE_SCOPES_MISSING,
            message="Required Google Sheets permissions were not granted.",
        )

    document = _oauth_document(uid)
    snapshot = document.get()
    record = {
        ENCRYPTED_REFRESH_TOKEN_FIELD: _encrypt_refresh_token(refresh_token),
        "scopes": sorted(scopes),
        "updatedAt": firestore.SERVER_TIMESTAMP,
    }
    if not snapshot.exists:
        record["createdAt"] = firestore.SERVER_TIMESTAMP
    document.set(record)
    logger.info("google_account_linked")
    return {"linked": True}


@https_fn.on_call(
    region="europe-west1",
    enforce_app_check=True,
    max_instances=10,
    concurrency=40,
)
def hasGoogleAccountLink(req: https_fn.CallableRequest):  # noqa: N802
    uid = _require_uid(req)
    _check_daily_limit(uid=uid, operation="status", maximum=MAX_STATUS_CHECKS_PER_DAY)
    snapshot = _oauth_document(uid).get()
    if not snapshot.exists:
        return {"linked": False}
    record = snapshot.to_dict() or {}
    linked = bool(_encrypted_refresh_token(record)) and _has_required_scopes(record.get("scopes"))
    return {"linked": linked}


@https_fn.on_call(
    region="europe-west1",
    enforce_app_check=True,
    max_instances=5,
    concurrency=10,
    secrets=[GOOGLE_TOKEN_ENCRYPTION_KEY],
)
def unlinkGoogleAccount(req: https_fn.CallableRequest):  # noqa: N802
    uid = _require_uid(req)
    _check_daily_limit(uid=uid, operation="unlink", maximum=MAX_UNLINKS_PER_DAY)
    document = _oauth_document(uid)
    snapshot = document.get()
    if snapshot.exists:
        try:
            record = snapshot.to_dict() or {}
            encrypted_token = _encrypted_refresh_token(record)
            if encrypted_token:
                try:
                    refresh_token = _decrypt_refresh_token(encrypted_token)
                except https_fn.HttpsError:
                    logger.warning("Stored Google authorization could not be decrypted during unlink")
                else:
                    try:
                        response = requests.post(
                            GOOGLE_REVOKE_URI,
                            data={"token": refresh_token},
                            timeout=15,
                        )
                        if not response.ok:
                            logger.warning("Google authorization revocation returned status %s", response.status_code)
                    except requests.RequestException:
                        logger.warning("Google authorization revocation was unavailable")
        finally:
            document.delete()
    logger.info("google_account_unlinked")
    return {"linked": False}


def _google_credentials(uid: str) -> Credentials:
    document = _oauth_document(uid)
    snapshot = document.get()
    if not snapshot.exists:
        raise _callable_error(
            code=https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
            reason=GOOGLE_RECONNECT_REQUIRED,
            message="Google Sheets must be reconnected.",
        )

    record = snapshot.to_dict() or {}
    encrypted_token = _encrypted_refresh_token(record)
    if not encrypted_token:
        document.delete()
        raise _callable_error(
            code=https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
            reason=GOOGLE_RECONNECT_REQUIRED,
            message="Google Sheets must be reconnected.",
        )
    if not _has_required_scopes(record.get("scopes")):
        document.delete()
        raise _callable_error(
            code=https_fn.FunctionsErrorCode.PERMISSION_DENIED,
            reason=GOOGLE_SCOPES_MISSING,
            message="Required Google Sheets permissions were not granted.",
        )

    try:
        refresh_token = _decrypt_refresh_token(encrypted_token)
    except https_fn.HttpsError:
        document.delete()
        raise

    credentials = Credentials(
        token=None,
        refresh_token=refresh_token,
        token_uri=GOOGLE_TOKEN_URI,
        client_id=GOOGLE_OAUTH_CLIENT_ID.value,
        client_secret=GOOGLE_OAUTH_CLIENT_SECRET.value,
    )
    try:
        credentials.refresh(GoogleAuthRequest())
    except RefreshError as error:
        is_permanent = _is_permanent_refresh_error(error)
        functions_logger.warn(
            "google_token_refresh_failed",
            stage="refresh_google_token",
            exceptionType=type(error).__name__,
            permanent=is_permanent,
        )
        if is_permanent:
            document.delete()
            raise _callable_error(
                code=https_fn.FunctionsErrorCode.FAILED_PRECONDITION,
                reason=GOOGLE_RECONNECT_REQUIRED,
                message="Google Sheets must be reconnected.",
            ) from error
        raise _token_endpoint_unavailable() from error
    except TransportError as error:
        functions_logger.warn(
            "google_token_refresh_failed",
            stage="refresh_google_token",
            exceptionType=type(error).__name__,
            permanent=False,
        )
        raise _token_endpoint_unavailable() from error
    return credentials


def _sanitize(value: str) -> str:
    """Prefix a leading apostrophe if the value could be interpreted as a formula."""
    if isinstance(value, str) and _FORMULA_PREFIX.match(value):
        return f"'{value}"
    return value


def _to_serial(iso_date: str) -> int:
    return (date.fromisoformat(iso_date) - EPOCH).days


@https_fn.on_call(
    region="europe-west1",
    timeout_sec=300,
    memory=options.MemoryOption.MB_512,
    enforce_app_check=True,
    max_instances=5,
    concurrency=4,
    secrets=[GOOGLE_OAUTH_CLIENT_ID, GOOGLE_OAUTH_CLIENT_SECRET, GOOGLE_TOKEN_ENCRYPTION_KEY],
)
def exportToSheets(req: https_fn.CallableRequest):  # noqa: N802
    started_at = time.monotonic()
    uid = _require_uid(req)
    data = _validate_export_request(req.data)
    export_id = data["exportId"]
    _check_daily_limit(uid=uid, operation="export", maximum=MAX_EXPORTS_PER_DAY)
    existing_url = _claim_export(uid=uid, export_id=export_id)
    if existing_url:
        return {"spreadsheetUrl": existing_url}

    stage = "prepare_export"
    try:
        expenses = data["expenses"]
        tab_layout = data["tabLayout"]
        currency_symbol = data["currencySymbol"]
        decimal_places = data["decimalPlaces"]
        date_range_label = data["dateRangeLabel"]
        title = data["title"]
        folder_name = data["folderName"]

        stage = "authorize_google_sheets"
        gc = gspread.authorize(_google_credentials(uid))
        all_categories = sorted({e.get("category", "") for e in expenses} - {""})
        amount_header = f"Amount ({currency_symbol})" if currency_symbol else "Amount"
        headers = ["Date", "Title", "Category", "Subcategory", amount_header, "Notes"]
        num_fmt = f"0.{'0' * decimal_places}" if decimal_places > 0 else "0"

        folder_id = None
        if folder_name:
            folder_id = _with_sheets_retry(
                operation=lambda: _resolve_drive_folder(gc, folder_name),
                failure_message="Failed to prepare Google Drive folder.",
                stage="resolve_drive_folder",
            )
        spreadsheet = _with_sheets_retry(
            operation=lambda: gc.create(title, folder_id=folder_id),
            failure_message="Failed to create spreadsheet.",
            stage="create_spreadsheet",
        )
        if tab_layout == "separate_tabs":
            _with_sheets_retry(
                operation=lambda: _build_separate_tabs(
                    spreadsheet,
                    expenses,
                    headers,
                    all_categories,
                    num_fmt,
                ),
                failure_message="Failed to write spreadsheet data.",
                stage="write_separate_tabs",
            )
        else:
            _with_sheets_retry(
                operation=lambda: _build_single_tab(
                    spreadsheet,
                    expenses,
                    headers,
                    all_categories,
                    num_fmt,
                    date_range_label,
                ),
                failure_message="Failed to write spreadsheet data.",
                stage="write_single_tab",
            )

        stage = "persist_export_result"
        spreadsheet_url = spreadsheet.url
        _complete_export(uid=uid, export_id=export_id, spreadsheet_url=spreadsheet_url)
        functions_logger.info(
            "sheets_export_completed",
            durationMs=round((time.monotonic() - started_at) * 1_000),
            expenseCount=len(expenses),
            tabLayout=tab_layout,
        )
        return {"spreadsheetUrl": spreadsheet_url}
    except https_fn.HttpsError:
        _mark_export_failed(uid=uid, export_id=export_id)
        raise
    except Exception as error:
        _mark_export_failed(uid=uid, export_id=export_id)
        functions_logger.error(
            "unexpected_export_failure",
            stage=stage,
            exceptionType=type(error).__name__,
        )
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message="Failed to export spreadsheet.",
        ) from error


# ── Helpers ──────────────────────────────────────────────────────────────────


def _validate_export_request(data: object) -> dict:
    if not isinstance(data, dict):
        raise _invalid_argument("Export request must be an object.")

    export_id = data.get("exportId")
    if export_id is None:
        raise _callable_error(
            code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            reason=EXPORT_ID_REQUIRED,
            message="Missing export ID.",
        )
    export_id = str(export_id)
    if (
        len(export_id) > MAX_EXPORT_ID_LENGTH
        or not _EXPORT_ID.fullmatch(export_id)
    ):
        raise _invalid_argument("Invalid export ID.")

    compressed = data.get("compressed", False)
    if not isinstance(compressed, bool):
        raise _invalid_argument("Invalid compression flag.")
    expenses = _parse_expenses(data=data, compressed=compressed)

    tab_layout = data.get("tabLayout", "single_tab")
    if tab_layout not in {"single_tab", "separate_tabs"}:
        raise _invalid_argument("Invalid tab layout.")

    decimal_places = data.get("decimalPlaces", 2)
    if (
        isinstance(decimal_places, bool)
        or not isinstance(decimal_places, int)
        or not 0 <= decimal_places <= MAX_DECIMAL_PLACES
    ):
        raise _invalid_argument("Invalid decimal places.")

    currency_symbol = _bounded_request_string(
        value=data.get("currencySymbol", ""),
        field="currencySymbol",
        maximum=MAX_CURRENCY_SYMBOL_LENGTH,
    )
    date_range_label = _bounded_request_string(
        value=data.get("dateRangeLabel") or data.get("month") or "Export",
        field="dateRangeLabel",
        maximum=MAX_LABEL_LENGTH,
    )
    requested_title = data.get("title")
    title = (
        _bounded_request_string(
            value=requested_title,
            field="title",
            maximum=MAX_TITLE_LENGTH,
        )
        if requested_title is not None
        else f"PlzStop Export - {date_range_label}"
    )
    requested_folder_name = data.get("folderName")
    folder_name = (
        _bounded_request_string(
            value=requested_folder_name,
            field="folderName",
            maximum=MAX_FOLDER_NAME_LENGTH,
        ).strip()
        if requested_folder_name is not None
        else None
    )

    return {
        "exportId": export_id,
        "expenses": expenses,
        "tabLayout": tab_layout,
        "decimalPlaces": decimal_places,
        "currencySymbol": currency_symbol,
        "dateRangeLabel": date_range_label,
        "title": title,
        "folderName": folder_name or None,
    }


def _bounded_request_string(value: object, field: str, maximum: int) -> str:
    if not isinstance(value, str) or len(value) > maximum:
        raise _invalid_argument(f"Invalid {field}.")
    return value


def _parse_expenses(data: dict, compressed: bool) -> list:
    if compressed:
        try:
            encoded = data["expenses"]
            if not isinstance(encoded, str):
                raise ValueError("Compressed payload must be a string")
            raw = base64.b64decode(encoded, validate=True)
            with gzip.GzipFile(fileobj=io.BytesIO(raw)) as gzip_file:
                decompressed = gzip_file.read(MAX_DECOMPRESSED_SIZE + 1)
            if len(decompressed) > MAX_DECOMPRESSED_SIZE:
                raise ValueError("Payload too large")
            expenses = json.loads(decompressed)
        except (KeyError, TypeError, ValueError, gzip.BadGzipFile, json.JSONDecodeError):
            raise https_fn.HttpsError(
                code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
                message="Invalid compressed payload.",
            )
    else:
        expenses = data.get("expenses", [])
        try:
            payload_size = len(
                json.dumps(expenses, ensure_ascii=False, separators=(",", ":")).encode()
            )
        except (TypeError, ValueError) as error:
            raise _invalid_argument("Invalid expenses payload.") from error
        if payload_size > MAX_DECOMPRESSED_SIZE:
            raise _invalid_argument("Expenses payload is too large.")

    if not isinstance(expenses, list) or not expenses:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            message="No expenses provided.",
        )
    if len(expenses) > MAX_EXPENSES:
        raise _invalid_argument("Too many expenses.")
    return [_validate_expense(expense) for expense in expenses]


def _validate_expense(expense: object) -> dict:
    if not isinstance(expense, dict):
        raise _invalid_argument("Invalid expense row.")

    date_value = expense.get("date")
    if not isinstance(date_value, str):
        raise _invalid_argument("Invalid expense date.")
    try:
        date.fromisoformat(date_value)
    except ValueError as error:
        raise _invalid_argument("Invalid expense date.") from error

    amount = expense.get("amount")
    if (
        isinstance(amount, bool)
        or not isinstance(amount, (int, float))
        or not math.isfinite(amount)
        or abs(amount) > 1_000_000_000_000
    ):
        raise _invalid_argument("Invalid expense amount.")

    normalized = {"date": date_value, "amount": amount}
    for field in ("title", "category", "subcategory", "notes"):
        normalized[field] = _bounded_request_string(
            value=expense.get(field, ""),
            field=f"expense {field}",
            maximum=MAX_CELL_TEXT_LENGTH,
        )
    return normalized


def _export_document(uid: str, export_id: str):
    return (
        firestore.client()
        .collection("exports")
        .document(uid)
        .collection("history")
        .document(export_id)
    )


def _claim_export(uid: str, export_id: str) -> str | None:
    document = _export_document(uid=uid, export_id=export_id)
    transaction = firestore.client().transaction()

    @firestore.transactional
    def claim(transaction):
        snapshot = document.get(transaction=transaction)
        if snapshot.exists:
            record = snapshot.to_dict() or {}
            spreadsheet_url = record.get("spreadsheetUrl")
            if record.get("status") == "success" and isinstance(spreadsheet_url, str):
                return spreadsheet_url
            started_at = record.get("startedAt")
            lease_is_active = (
                isinstance(started_at, datetime)
                and datetime.now(timezone.utc) - started_at < EXPORT_LEASE_DURATION
            )
            if record.get("status") == "processing" and lease_is_active:
                raise _callable_error(
                    code=https_fn.FunctionsErrorCode.ALREADY_EXISTS,
                    reason=EXPORT_IN_PROGRESS,
                    message="Export is already in progress.",
                )
        transaction.set(
            document,
            {
                "status": "processing",
                "startedAt": firestore.SERVER_TIMESTAMP,
            },
        )
        return None

    return claim(transaction)


def _complete_export(uid: str, export_id: str, spreadsheet_url: str) -> None:
    _export_document(uid=uid, export_id=export_id).set(
        {
            "spreadsheetUrl": spreadsheet_url,
            "status": "success",
            "completedAt": firestore.SERVER_TIMESTAMP,
        }
    )


def _mark_export_failed(uid: str, export_id: str) -> None:
    try:
        _export_document(uid=uid, export_id=export_id).set(
            {
                "status": "failed",
                "completedAt": firestore.SERVER_TIMESTAMP,
            },
            merge=True,
        )
    except Exception:
        logger.warning("Failed to mark export result", exc_info=True)


def _resolve_drive_folder(gc, folder_name: str) -> str:
    escaped_name = folder_name.replace("\\", "\\\\").replace("'", "\\'")
    query = (
        f"name = '{escaped_name}' and "
        "mimeType = 'application/vnd.google-apps.folder' and trashed = false"
    )
    response = gc.http_client.request(
        "get",
        DRIVE_FILES_API_V3_URL,
        params={
            "q": query,
            "fields": "files(id,name)",
            "pageSize": 1,
            "spaces": "drive",
        },
    )
    folders = response.json().get("files", [])
    if folders:
        return folders[0]["id"]

    response = gc.http_client.request(
        "post",
        DRIVE_FILES_API_V3_URL,
        json={
            "name": folder_name,
            "mimeType": "application/vnd.google-apps.folder",
        },
        params={"fields": "id"},
    )
    return response.json()["id"]


def _with_sheets_retry(operation, failure_message: str, stage: str):
    for attempt in range(MAX_SHEETS_ATTEMPTS):
        try:
            return operation()
        except APIError as error:
            status_code = getattr(getattr(error, "response", None), "status_code", None)
            retryable = status_code == 429 or (
                isinstance(status_code, int) and status_code >= 500
            )
            if not retryable or attempt == MAX_SHEETS_ATTEMPTS - 1:
                functions_logger.error(
                    "google_sheets_operation_failed",
                    stage=stage,
                    exceptionType=type(error).__name__,
                    statusCode=status_code,
                    retryable=retryable,
                    attempt=attempt + 1,
                )
                if retryable:
                    raise _callable_error(
                        code=https_fn.FunctionsErrorCode.UNAVAILABLE,
                        reason=SHEETS_TEMPORARILY_UNAVAILABLE,
                        message=failure_message,
                    ) from error
                raise https_fn.HttpsError(
                    code=https_fn.FunctionsErrorCode.INTERNAL,
                    message=failure_message,
                ) from error
            functions_logger.warn(
                "retrying_google_sheets_operation",
                stage=stage,
                statusCode=status_code,
                attempt=attempt + 1,
            )
            time.sleep(2 ** attempt)
        except Exception as error:
            functions_logger.error(
                "google_sheets_operation_failed",
                stage=stage,
                exceptionType=type(error).__name__,
                statusCode=None,
                retryable=False,
                attempt=attempt + 1,
            )
            raise https_fn.HttpsError(
                code=https_fn.FunctionsErrorCode.INTERNAL,
                message=failure_message,
            ) from error


def _expense_row(e: dict) -> list:
    date_str = e.get("date", "")
    try:
        serial = _to_serial(date_str)
    except (ValueError, TypeError):
        serial = date_str
    return [
        serial,
        _sanitize(str(e.get("title", ""))),
        _sanitize(str(e.get("category", ""))),
        _sanitize(str(e.get("subcategory", ""))),
        e.get("amount", "0"),
        _sanitize(str(e.get("notes", ""))),
    ]


def _collect_subcategories(expenses: list) -> dict[str, list[str]]:
    """Build category → sorted subcategory names mapping from expense data."""
    cat_subs: dict[str, list[str]] = {}
    for e in expenses:
        cat = e.get("category", "")
        sub = e.get("subcategory", "")
        if cat and cat not in cat_subs:
            cat_subs[cat] = []
        if cat and sub and sub not in cat_subs[cat]:
            cat_subs[cat].append(sub)
    for subs in cat_subs.values():
        subs.sort()
    return cat_subs


def _month_serial_range(year: int, month: int) -> tuple[int, int]:
    """Return (start_serial_inclusive, end_serial_exclusive) for a year/month."""
    start = date(year, month, 1)
    end = date(year + 1, 1, 1) if month == 12 else date(year, month + 1, 1)
    return (_to_serial(start.isoformat()), _to_serial(end.isoformat()))


def _detect_months(expenses: list) -> list[tuple[str, int, int]]:
    """Return sorted list of (label, start_serial, end_serial) for each month in expenses."""
    month_keys: set[str] = set()
    for e in expenses:
        d = e.get("date", "")
        if len(d) >= 7:
            month_keys.add(d[:7])

    months = []
    for key in sorted(month_keys):
        try:
            d = date.fromisoformat(key + "-01")
            start, end = _month_serial_range(d.year, d.month)
            months.append((d.strftime("%b %Y"), start, end))
        except ValueError:
            pass
    return months


def _write_sheet(ws, expenses: list, headers: list, categories: list, num_fmt: str,
                 pivot: bool = False):
    """Write expense rows + category summary to a worksheet."""
    rows, expense_count, summary_row_count = _build_sheet_rows(
        expenses=expenses,
        headers=headers,
        categories=categories,
        pivot=pivot,
    )
    requests = []
    _append_sheet_requests(
        requests=requests,
        sheet_id=ws.id,
        rows=rows,
        expense_count=expense_count,
        num_fmt=num_fmt,
        summary_row_count=summary_row_count,
    )
    ws.spreadsheet.batch_update({"requests": requests})


def _build_sheet_rows(expenses: list, headers: list, categories: list,
                      pivot: bool = False) -> tuple[list, int, int]:
    """Build expense rows + category summary for a worksheet.

    When pivot=True, the summary uses months as columns (for single-tab multi-month exports).
    """
    sorted_exp = sorted(expenses, key=lambda e: e.get("date", ""))
    n = len(sorted_exp)

    rows = [headers]
    rows.extend(_expense_row(e) for e in sorted_exp)
    rows.append(["", "", "", "", _Formula(f"=SUM(E2:E{n + 1})"), ""])

    cat_subs = _collect_subcategories(sorted_exp)

    months = _detect_months(sorted_exp) if pivot else []
    if len(months) > 1:
        _build_pivot_summary(rows, n, categories, cat_subs, headers[4], months)
    else:
        _build_simple_summary(rows, n, categories, cat_subs, headers[4])

    summary_row_count = sum(1 + len(cat_subs.get(c, [])) for c in categories)
    return rows, n, summary_row_count


def _build_simple_summary(rows: list, n: int, categories: list,
                          cat_subs: dict[str, list[str]], amount_header: str):
    """Single-column summary: Category + Amount."""
    rows.append([])
    rows.append(["Category Summary", ""])
    rows.append(["Category", amount_header])
    for cat in categories:
        safe_cat = _formula_literal(cat)
        label = _sanitize(cat)
        has_data = cat in cat_subs
        rows.append(
            [
                label,
                _Formula(f'=SUMIF(C2:C{n + 1},"{safe_cat}",E2:E{n + 1})'),
            ]
            if has_data
            else [label, ""]
        )

        for sub in cat_subs.get(cat, []):
            safe_sub = _formula_literal(sub)
            sub_label = f"  {_sanitize(sub)}"
            rows.append(
                [
                    sub_label,
                    _Formula(
                        f'=SUMIFS(E2:E{n + 1},C2:C{n + 1},"{safe_cat}",'
                        f'D2:D{n + 1},"{safe_sub}")'
                    ),
                ]
            )


def _build_pivot_summary(rows: list, n: int, categories: list,
                         cat_subs: dict[str, list[str]], amount_header: str,
                         months: list[tuple[str, int, int]]):
    """Pivot summary: months as columns, categories + subcategories as rows."""
    month_labels = [m[0] for m in months]
    rows.append([])
    rows.append(["Category Summary"] + [""] * len(months))
    rows.append(["Category"] + month_labels + ["Total"])

    er = f"E2:E{n + 1}"  # expense amount range
    cr = f"C2:C{n + 1}"  # category range
    dr = f"D2:D{n + 1}"  # subcategory range
    ar = f"A2:A{n + 1}"  # date range

    for cat in categories:
        safe_cat = _formula_literal(cat)
        label = _sanitize(cat)
        has_data = cat in cat_subs
        if has_data:
            month_cells = [
                _Formula(
                    f'=SUMIFS({er},{cr},"{safe_cat}",{ar},">="&{s},{ar},"<"&{e})'
                )
                for _, s, e in months
            ]
            total = _Formula(f'=SUMIF({cr},"{safe_cat}",{er})')
            rows.append([label] + month_cells + [total])
        else:
            rows.append([label] + [""] * (len(months) + 1))

        for sub in cat_subs.get(cat, []):
            safe_sub = _formula_literal(sub)
            sub_label = f"  {_sanitize(sub)}"
            month_cells = [
                _Formula(
                    f'=SUMIFS({er},{cr},"{safe_cat}",{dr},"{safe_sub}",'
                    f'{ar},">="&{s},{ar},"<"&{e})'
                )
                for _, s, e in months
            ]
            total = _Formula(
                f'=SUMIFS({er},{cr},"{safe_cat}",{dr},"{safe_sub}")'
            )
            rows.append([sub_label] + month_cells + [total])


def _build_single_tab(spreadsheet, expenses, headers, all_categories, num_fmt, label):
    sheet_id = spreadsheet.sheet1.id
    rows, expense_count, summary_row_count = _build_sheet_rows(
        expenses=expenses,
        headers=headers,
        categories=all_categories,
        pivot=True,
    )
    requests = [
        _update_sheet_title_request(sheet_id, label),
    ]
    _append_sheet_requests(
        requests=requests,
        sheet_id=sheet_id,
        rows=rows,
        expense_count=expense_count,
        num_fmt=num_fmt,
        summary_row_count=summary_row_count,
    )
    spreadsheet.batch_update({"requests": requests})


def _build_separate_tabs(spreadsheet, expenses, headers, all_categories, num_fmt):
    by_month = defaultdict(list)
    for e in expenses:
        d = e.get("date", "")
        by_month[d[:7] if len(d) >= 7 else "Unknown"].append(e)

    requests = []
    default_sheet_id = spreadsheet.sheet1.id

    for index, month_key in enumerate(sorted(by_month)):
        try:
            sheet_name = date.fromisoformat(month_key + "-01").strftime("%B %Y")
        except ValueError:
            sheet_name = month_key

        if index == 0:
            sheet_id = default_sheet_id
            requests.append(_update_sheet_title_request(sheet_id, sheet_name))
        else:
            sheet_id = GENERATED_SHEET_ID_START + index
            requests.append(
                {
                    "addSheet": {
                        "properties": {
                            "sheetId": sheet_id,
                            "title": sheet_name,
                            "gridProperties": {"rowCount": 1000, "columnCount": 10},
                        }
                    }
                }
            )

        rows, expense_count, summary_row_count = _build_sheet_rows(
            expenses=by_month[month_key],
            headers=headers,
            categories=all_categories,
        )
        _append_sheet_requests(
            requests=requests,
            sheet_id=sheet_id,
            rows=rows,
            expense_count=expense_count,
            num_fmt=num_fmt,
            summary_row_count=summary_row_count,
        )

    spreadsheet.batch_update({"requests": requests})


def _update_sheet_title_request(sheet_id: int, title: str) -> dict:
    return {
        "updateSheetProperties": {
            "properties": {"sheetId": sheet_id, "title": title},
            "fields": "title",
        }
    }


def _append_sheet_requests(requests: list, sheet_id: int, rows: list, expense_count: int,
                           num_fmt: str, summary_row_count: int):
    requests.append(_update_cells_request(sheet_id, rows))
    requests.extend(_formatting_requests(sheet_id, expense_count, num_fmt, summary_row_count))


def _update_cells_request(sheet_id: int, rows: list) -> dict:
    return {
        "updateCells": {
            "start": {"sheetId": sheet_id, "rowIndex": 0, "columnIndex": 0},
            "rows": [_row_data(row) for row in rows],
            "fields": "userEnteredValue",
        }
    }


def _row_data(row: list) -> dict:
    return {
        "values": [
            _cell_data(value=value, column_index=index)
            for index, value in enumerate(row)
        ]
    }


def _cell_data(value, column_index: int) -> dict:
    if value in (None, ""):
        return {}
    if isinstance(value, _Formula):
        return {"userEnteredValue": {"formulaValue": value.value}}
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        return {"userEnteredValue": {"numberValue": value}}
    if column_index == AMOUNT_COLUMN_INDEX:
        try:
            return {"userEnteredValue": {"numberValue": float(value)}}
        except (TypeError, ValueError):
            pass
    return {"userEnteredValue": {"stringValue": str(value)}}


def _formula_literal(value: str) -> str:
    return value.replace('"', '""')


def _formatting_requests(sheet_id: int, expense_count: int, num_fmt: str, category_count: int) -> list:
    n = expense_count
    total_row = n + 2
    summary_header_row = n + 4

    return [
        # Header row: bold, blue bg, white text
        {
            "repeatCell": {
                "range": {"sheetId": sheet_id, "startRowIndex": 0, "endRowIndex": 1},
                "cell": {
                    "userEnteredFormat": {
                        "backgroundColor": {"red": 0.259, "green": 0.522, "blue": 0.957},
                        "textFormat": {"bold": True, "foregroundColor": {"red": 1, "green": 1, "blue": 1}},
                    }
                },
                "fields": "userEnteredFormat(backgroundColor,textFormat)",
            }
        },
        # Total row: bold
        {
            "repeatCell": {
                "range": {"sheetId": sheet_id, "startRowIndex": total_row - 1, "endRowIndex": total_row},
                "cell": {"userEnteredFormat": {"textFormat": {"bold": True}}},
                "fields": "userEnteredFormat(textFormat)",
            }
        },
        # Category summary header: bold, light blue bg
        {
            "repeatCell": {
                "range": {"sheetId": sheet_id, "startRowIndex": summary_header_row - 1, "endRowIndex": summary_header_row},
                "cell": {
                    "userEnteredFormat": {
                        "backgroundColor": {"red": 0.91, "green": 0.918, "blue": 0.965},
                        "textFormat": {"bold": True},
                    }
                },
                "fields": "userEnteredFormat(backgroundColor,textFormat)",
            }
        },
        # Date column: date format
        {
            "repeatCell": {
                "range": {
                    "sheetId": sheet_id,
                    "startRowIndex": 1,
                    "endRowIndex": n + 1,
                    "startColumnIndex": 0,
                    "endColumnIndex": 1,
                },
                "cell": {"userEnteredFormat": {"numberFormat": {"type": "DATE", "pattern": "yyyy-mm-dd"}}},
                "fields": "userEnteredFormat(numberFormat)",
            }
        },
        # Amount column: number format
        {
            "repeatCell": {
                "range": {
                    "sheetId": sheet_id,
                    "startRowIndex": 1,
                    "endRowIndex": n + 2,
                    "startColumnIndex": 4,
                    "endColumnIndex": 5,
                },
                "cell": {"userEnteredFormat": {"numberFormat": {"type": "NUMBER", "pattern": num_fmt}}},
                "fields": "userEnteredFormat(numberFormat)",
            }
        },
        # Freeze header
        {
            "updateSheetProperties": {
                "properties": {"sheetId": sheet_id, "gridProperties": {"frozenRowCount": 1}},
                "fields": "gridProperties.frozenRowCount",
            }
        },
        # Column widths
        *[
            {
                "updateDimensionProperties": {
                    "range": {"sheetId": sheet_id, "dimension": "COLUMNS", "startIndex": i, "endIndex": i + 1},
                    "properties": {"pixelSize": w},
                    "fields": "pixelSize",
                }
            }
            for i, w in enumerate([100, 200, 150, 150, 120, 250])
        ],
    ]


GENERATED_SHEET_ID_START = 10_000
AMOUNT_COLUMN_INDEX = 4
