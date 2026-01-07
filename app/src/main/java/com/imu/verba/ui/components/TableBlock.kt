package com.imu.verba.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imu.verba.document.model.TableData

/**
 * Renders a table with VS Code-style bordered cells.
 * Each cell is a distinct box with visible borders.
 */
@Composable
fun TableBlock(
    tableData: TableData,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val headerBackground = MaterialTheme.colorScheme.surfaceVariant
    val cellBackground = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        color = cellBackground
    ) {
        Column(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            // Header row
            if (tableData.headers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .background(headerBackground)
                        .width(IntrinsicSize.Max)
                ) {
                    tableData.headers.forEach { header ->
                        TableCell(
                            text = header,
                            isHeader = true,
                            borderColor = borderColor
                        )
                    }
                }
            }

            // Data rows
            tableData.rows.forEachIndexed { rowIndex, row ->
                val rowBackground = if (rowIndex % 2 == 0) {
                    cellBackground
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }
                
                Row(
                    modifier = Modifier
                        .background(rowBackground)
                        .width(IntrinsicSize.Max)
                ) {
                    // Ensure same number of cells as headers
                    val columnCount = maxOf(tableData.headers.size, row.size)
                    for (i in 0 until columnCount) {
                        TableCell(
                            text = row.getOrElse(i) { "" },
                            isHeader = false,
                            borderColor = borderColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    isHeader: Boolean,
    borderColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(0.5.dp, borderColor)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .width(IntrinsicSize.Max)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(IntrinsicSize.Max)
        )
    }
}

/**
 * Renders a code block with monospace font and dark background.
 */
@Composable
fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
    }
}
