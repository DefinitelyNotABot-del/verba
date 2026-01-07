package com.imu.verba.document.model

/**
 * A single block of content within a document.
 * This is the atomic unit for rendering, editing, and TTS playback.
 *
 * @param id Unique identifier within the document (0-indexed)
 * @param text Plain text content of the block (for TTS and display)
 * @param type Semantic type (HEADING, PARAGRAPH, LIST, TABLE, CODE_BLOCK)
 * @param editable Whether this block can be edited (false for PDF)
 * @param tableData Structured table data if type is TABLE
 * @param headingLevel Heading level (1-6) if type is HEADING
 */
data class DocBlock(
    val id: Int,
    val text: String,
    val type: BlockType,
    val editable: Boolean,
    val tableData: TableData? = null,
    val headingLevel: Int = 1
)
