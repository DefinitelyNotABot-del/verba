package com.imu.verba.document.model

/**
 * Represents a parsed document ready for display and TTS.
 * All formats (Markdown, PDF, DOCX) normalize to this structure.
 *
 * @param name Display name of the document (typically filename)
 * @param blocks Ordered list of content blocks
 * @param format Original format of the document
 */
data class Document(
    val name: String,
    val blocks: List<DocBlock>,
    val format: DocumentFormat
)
