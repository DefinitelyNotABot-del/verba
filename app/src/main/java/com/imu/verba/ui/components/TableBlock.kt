package com.imu.verba.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val borderColor = MaterialTheme.colorScheme.outline
    val headerBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val cellBackground = MaterialTheme.colorScheme.surface
    val alternateRowBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

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
                        .height(IntrinsicSize.Min)
                ) {
                    tableData.headers.forEachIndexed { index, header ->
                        TableCell(
                            text = header,
                            isHeader = true,
                            showLeftBorder = index > 0,
                            borderColor = borderColor
                        )
                    }
                }
                HorizontalDivider(color = borderColor, thickness = 1.dp)
            }

            // Data rows
            tableData.rows.forEachIndexed { rowIndex, row ->
                val rowBackground = if (rowIndex % 2 == 0) {
                    cellBackground
                } else {
                    alternateRowBackground
                }
                
                Row(
                    modifier = Modifier
                        .background(rowBackground)
                        .height(IntrinsicSize.Min)
                ) {
                    // Ensure same number of cells as headers
                    val columnCount = maxOf(tableData.headers.size, row.size)
                    for (i in 0 until columnCount) {
                        TableCell(
                            text = row.getOrElse(i) { "" },
                            isHeader = false,
                            showLeftBorder = i > 0,
                            borderColor = borderColor
                        )
                    }
                }
                
                // Add bottom border except for last row
                if (rowIndex < tableData.rows.size - 1) {
                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    isHeader: Boolean,
    showLeftBorder: Boolean,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxHeight()) {
        if (showLeftBorder) {
            VerticalDivider(
                color = borderColor,
                thickness = 1.dp,
                modifier = Modifier.fillMaxHeight()
            )
        }
        
        Box(
            modifier = Modifier
                .widthIn(min = 80.dp, max = 200.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                color = if (isHeader) MaterialTheme.colorScheme.onPrimaryContainer 
                       else MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
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
