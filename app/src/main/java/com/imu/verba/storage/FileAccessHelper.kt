package com.imu.verba.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Helper for file access using Storage Access Framework (SAF).
 * Never assumes file paths - always uses URIs.
 */
object FileAccessHelper {

    /**
     * MIME types for supported document formats.
     */
    object MimeTypes {
        const val MARKDOWN = "text/markdown"
        const val MARKDOWN_ALT = "text/plain" // Some systems use text/plain for .md
        const val PDF = "application/pdf"
        const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

        val ALL_SUPPORTED = arrayOf(MARKDOWN, MARKDOWN_ALT, PDF, DOCX)
        val MARKDOWN_TYPES = arrayOf(MARKDOWN, MARKDOWN_ALT)
    }

    /**
     * Read text content from a URI.
     * Suitable for Markdown and other text-based formats.
     *
     * @param context Application context
     * @param uri Content URI from SAF
     * @return File content as string, or null if read fails
     */
    fun readTextContent(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the display name of a file from its URI.
     *
     * @param context Application context
     * @param uri Content URI
     * @return Display name or "Unknown" if not available
     */
    fun getFileName(context: Context, uri: Uri): String {
        var name = "Unknown"
        
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                }
            }
        } else {
            name = uri.lastPathSegment ?: name
        }
        
        return name
    }

    /**
     * Determine document format from filename or MIME type.
     *
     * @param fileName Name of the file
     * @param mimeType MIME type if known
     * @return Detected format, or null if unsupported
     */
    fun detectFormat(fileName: String, mimeType: String? = null): String? {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".md") -> "markdown"
            lowerName.endsWith(".pdf") -> "pdf"
            lowerName.endsWith(".docx") -> "docx"
            mimeType == MimeTypes.MARKDOWN -> "markdown"
            mimeType == MimeTypes.PDF -> "pdf"
            mimeType == MimeTypes.DOCX -> "docx"
            else -> null
        }
    }

    /**
     * Write text content to a URI.
     * Used for saving edited Markdown/DOCX files.
     *
     * @param context Application context
     * @param uri Content URI to write to
     * @param content Text content to write
     * @return true if write succeeded
     */
    fun writeTextContent(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
