package com.imu.verba.document.model

/**
 * Represents a table cell with content and optional header flag.
 */
data class TableCell(
    val content: String,
    val isHeader: Boolean = false
)

/**
 * Represents a row in a table.
 */
data class TableRow(
    val cells: List<TableCell>
)

/**
 * Structured table data for rendering tables with proper borders.
 */
data class TableData(
    val headers: List<String>,
    val rows: List<List<String>>
) {
    /**
     * Convert table to plain text for TTS.
     */
    fun toPlainText(): String {
        val sb = StringBuilder()
        if (headers.isNotEmpty()) {
            sb.append("Table headers: ")
            sb.append(headers.joinToString(", "))
            sb.append(". ")
        }
        rows.forEachIndexed { index, row ->
            sb.append("Row ${index + 1}: ")
            row.forEachIndexed { cellIndex, cell ->
                if (cellIndex < headers.size) {
                    sb.append("${headers[cellIndex]}: $cell. ")
                } else {
                    sb.append("$cell. ")
                }
            }
        }
        return sb.toString().trim()
    }
}
