"""Cloud Function: exportToSheets

Creates a Google Spreadsheet from expense data and notifies the user via FCM.
Deployed as Firebase Gen 2 HTTPS Callable in europe-west1.
"""

from __future__ import annotations

import base64
import gzip
import json
import logging
import re
import urllib.parse
from collections import defaultdict
from datetime import date

import gspread
from firebase_admin import firestore, initialize_app, messaging
from firebase_functions import https_fn, options
from google.oauth2.credentials import Credentials

initialize_app()

logger = logging.getLogger(__name__)

EPOCH = date(1899, 12, 30)
MAX_DECOMPRESSED_SIZE = 10 * 1024 * 1024  # 10 MB
MAX_DECIMAL_PLACES = 10
# Sheets formula injection: strings starting with these are dangerous in cells
_FORMULA_PREFIX = re.compile(r"^[=+\-@]")


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
    memory=options.MemoryOption.MB_256,
)
def exportToSheets(req: https_fn.CallableRequest):  # noqa: N802
    if req.auth is None:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.UNAUTHENTICATED,
            message="Authentication required.",
        )

    data = req.data
    access_token = data.get("googleAccessToken")
    if not access_token:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.UNAUTHENTICATED,
            message="INVALID_TOKEN",
        )

    export_id = data.get("exportId")
    fcm_token = data.get("fcmToken")
    tab_layout = data.get("tabLayout", "single_tab")
    currency_symbol = data.get("currencySymbol", "")
    decimal_places = min(int(data.get("decimalPlaces", 2)), MAX_DECIMAL_PLACES)
    compressed = data.get("compressed", False)

    expenses = _parse_expenses(data, compressed)

    gc = gspread.authorize(Credentials(token=access_token))
    all_categories = sorted({e.get("category", "") for e in expenses} - {""})
    amount_header = f"Amount ({currency_symbol})" if currency_symbol else "Amount"
    headers = ["Date", "Title", "Category", "Subcategory", amount_header, "Notes"]
    num_fmt = f"0.{'0' * decimal_places}" if decimal_places > 0 else "0"

    date_range_label = data.get("dateRangeLabel") or data.get("month") or "Export"
    title = data.get("title") or f"PlzStop Export - {date_range_label}"

    try:
        spreadsheet = gc.create(title)
    except Exception:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message="Failed to create spreadsheet.",
        )

    try:
        if tab_layout == "separate_tabs":
            _build_separate_tabs(spreadsheet, expenses, headers, all_categories, num_fmt)
        else:
            _build_single_tab(spreadsheet, expenses, headers, all_categories, num_fmt, date_range_label)
    except Exception:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INTERNAL,
            message="Failed to write spreadsheet data.",
        )

    spreadsheet_url = spreadsheet.url

    _save_export_result(req.auth.uid, export_id, spreadsheet_url)
    _send_fcm_notification(fcm_token, export_id, spreadsheet_url)

    return {"spreadsheetUrl": spreadsheet_url}


# ── Helpers ──────────────────────────────────────────────────────────────────


def _parse_expenses(data: dict, compressed: bool) -> list:
    if compressed:
        try:
            raw = base64.b64decode(data["expenses"])
            decompressed = gzip.decompress(raw)
            if len(decompressed) > MAX_DECOMPRESSED_SIZE:
                raise ValueError("Payload too large")
            expenses = json.loads(decompressed)
        except Exception:
            raise https_fn.HttpsError(
                code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
                message="Invalid compressed payload.",
            )
    else:
        expenses = data.get("expenses", [])

    if not expenses:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            message="No expenses provided.",
        )
    return expenses


def _save_export_result(uid: str, export_id: str | None, spreadsheet_url: str):
    """Write export result to Firestore so the client can recover if the worker is killed."""
    if not export_id:
        return
    try:
        db = firestore.client()
        db.collection("exports").document(uid).collection("history").document(str(export_id)).set({
            "spreadsheetUrl": spreadsheet_url,
            "status": "success",
            "completedAt": firestore.SERVER_TIMESTAMP,
        })
    except Exception:
        logger.warning("Failed to save export result to Firestore", exc_info=True)


def _send_fcm_notification(fcm_token: str | None, export_id: str | None, spreadsheet_url: str):
    if not fcm_token:
        return
    try:
        url_encoded = urllib.parse.quote(spreadsheet_url, safe="")
        data = {
            "deepLink": f"plzstop://open?url={url_encoded}",
            "spreadsheetUrl": spreadsheet_url,
        }
        if export_id:
            data["exportId"] = str(export_id)

        msg = messaging.Message(
            token=fcm_token,
            notification=messaging.Notification(
                title="Your export is ready",
                body="Tap to open your Google Spreadsheet",
            ),
            data=data,
            apns=messaging.APNSConfig(
                payload=messaging.APNSPayload(aps=messaging.Aps(sound="default")),
            ),
        )
        messaging.send(msg)
    except Exception:
        logger.warning("FCM notification failed", exc_info=True)


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
    rows.append(["", "", "", "", f"=SUM(E2:E{n + 1})", ""])

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
        safe_cat = cat.replace('"', '\\"')
        label = _sanitize(cat)
        has_data = cat in cat_subs
        rows.append([label, f'=SUMIF(C2:C{n + 1},"{safe_cat}",E2:E{n + 1})'] if has_data else [label, ""])

        for sub in cat_subs.get(cat, []):
            safe_sub = sub.replace('"', '\\"')
            sub_label = f"  {_sanitize(sub)}"
            rows.append([sub_label, f'=SUMIFS(E2:E{n + 1},C2:C{n + 1},"{safe_cat}",D2:D{n + 1},"{safe_sub}")'])


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
        safe_cat = cat.replace('"', '\\"')
        label = _sanitize(cat)
        has_data = cat in cat_subs
        if has_data:
            month_cells = [
                f'=SUMIFS({er},{cr},"{safe_cat}",{ar},">="&{s},{ar},"<"&{e})'
                for _, s, e in months
            ]
            total = f'=SUMIF({cr},"{safe_cat}",{er})'
            rows.append([label] + month_cells + [total])
        else:
            rows.append([label] + [""] * (len(months) + 1))

        for sub in cat_subs.get(cat, []):
            safe_sub = sub.replace('"', '\\"')
            sub_label = f"  {_sanitize(sub)}"
            month_cells = [
                f'=SUMIFS({er},{cr},"{safe_cat}",{dr},"{safe_sub}",{ar},">="&{s},{ar},"<"&{e})'
                for _, s, e in months
            ]
            total = f'=SUMIFS({er},{cr},"{safe_cat}",{dr},"{safe_sub}")'
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
    if isinstance(value, str) and value.startswith("="):
        return {"userEnteredValue": {"formulaValue": value}}
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        return {"userEnteredValue": {"numberValue": value}}
    if column_index == AMOUNT_COLUMN_INDEX:
        try:
            return {"userEnteredValue": {"numberValue": float(value)}}
        except (TypeError, ValueError):
            pass
    return {"userEnteredValue": {"stringValue": str(value)}}


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
