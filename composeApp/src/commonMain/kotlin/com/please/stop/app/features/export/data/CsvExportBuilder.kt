package com.please.stop.app.features.export.data

import com.please.stop.app.features.export.domain.model.ExportExpenseRow

class CsvExportBuilder {

    fun build(rows: List<ExportExpenseRow>): String {
        return buildString {
            appendLineCsv(HEADER)
            rows.forEach { row ->
                appendLineCsv(
                    listOf(
                        row.date,
                        row.title,
                        row.category,
                        row.subcategory,
                        row.amount,
                        row.notes,
                    )
                )
            }
        }
    }

    private fun StringBuilder.appendLineCsv(values: List<String>) {
        append(values.joinToString(separator = ",") { it.escapeCsv() })
        append(CRLF)
    }

    private fun String.escapeCsv(): String {
        val needsEscaping = any { it == ',' || it == '"' || it == '\r' || it == '\n' }
        if (!needsEscaping) return this
        return "\"${replace("\"", "\"\"")}\""
    }

    private companion object {
        const val CRLF = "\r\n"

        val HEADER = listOf(
            "Date",
            "Title",
            "Category",
            "Subcategory",
            "Amount",
            "Notes",
        )
    }
}
