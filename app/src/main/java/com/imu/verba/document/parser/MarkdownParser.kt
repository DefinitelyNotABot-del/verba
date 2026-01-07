package com.imu.verba.document.parser

import com.imu.verba.document.model.BlockType
import com.imu.verba.document.model.DocBlock
import com.imu.verba.document.model.Document
import com.imu.verba.document.model.DocumentFormat
import com.imu.verba.document.model.TableData
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text
import org.commonmark.parser.Parser

/**
 * Parses Markdown content into the normalized Document model.
 * Uses CommonMark library with GFM tables extension.
 */
class MarkdownParser {

    private val parser: Parser = Parser.builder()
        .extensions(listOf(TablesExtension.create()))
        .build()

    /**
     * Parse markdown text into a Document.
     *
     * @param content Raw markdown string
     * @param fileName Name of the file for display
     * @return Parsed Document with blocks
     */
    fun parse(content: String, fileName: String): Document {
        val node = parser.parse(content)
        val blocks = mutableListOf<DocBlock>()
        var blockId = 0

        node.accept(object : AbstractVisitor() {
            override fun visit(heading: Heading) {
                val text = extractText(heading)
                if (text.isNotBlank()) {
                    blocks.add(
                        DocBlock(
                            id = blockId++,
                            text = text,
                            type = BlockType.HEADING,
                            editable = true,
                            headingLevel = heading.level
                        )
                    )
                }
            }

            override fun visit(paragraph: Paragraph) {
                if (paragraph.parent is ListItem) {
                    return
                }
                val text = extractText(paragraph)
                if (text.isNotBlank()) {
                    blocks.add(
                        DocBlock(
                            id = blockId++,
                            text = text,
                            type = BlockType.PARAGRAPH,
                            editable = true
                        )
                    )
                }
            }

            override fun visit(bulletList: BulletList) {
                visitListItems(bulletList, false)
            }

            override fun visit(orderedList: OrderedList) {
                visitListItems(orderedList, true)
            }

            override fun visit(fencedCodeBlock: FencedCodeBlock) {
                val text = fencedCodeBlock.literal?.trim() ?: ""
                if (text.isNotBlank()) {
                    blocks.add(
                        DocBlock(
                            id = blockId++,
                            text = text,
                            type = BlockType.CODE_BLOCK,
                            editable = true
                        )
                    )
                }
            }

            override fun visit(indentedCodeBlock: IndentedCodeBlock) {
                val text = indentedCodeBlock.literal?.trim() ?: ""
                if (text.isNotBlank()) {
                    blocks.add(
                        DocBlock(
                            id = blockId++,
                            text = text,
                            type = BlockType.CODE_BLOCK,
                            editable = true
                        )
                    )
                }
            }

            override fun visit(customBlock: org.commonmark.node.CustomBlock) {
                if (customBlock is TableBlock) {
                    val tableData = parseTable(customBlock)
                    if (tableData != null) {
                        blocks.add(
                            DocBlock(
                                id = blockId++,
                                text = tableData.toSpeechText(),
                                type = BlockType.TABLE,
                                editable = true,
                                tableData = tableData
                            )
                        )
                    }
                } else {
                    visitChildren(customBlock)
                }
            }

            private fun visitListItems(listNode: Node, isOrdered: Boolean) {
                var child = listNode.firstChild
                var itemNumber = 1
                while (child != null) {
                    if (child is ListItem) {
                        val text = extractText(child)
                        if (text.isNotBlank()) {
                            val prefix = if (isOrdered) "${itemNumber}." else "•"
                            blocks.add(
                                DocBlock(
                                    id = blockId++,
                                    text = "$prefix $text",
                                    type = BlockType.LIST,
                                    editable = true
                                )
                            )
                            itemNumber++
                        }
                    }
                    child = child.next
                }
            }
        })

        return Document(
            name = fileName,
            blocks = blocks,
            format = DocumentFormat.MARKDOWN
        )
    }

    /**
     * Parse a CommonMark TableBlock into our TableData model.
     */
    private fun parseTable(tableBlock: TableBlock): TableData? {
        val headers = mutableListOf<String>()
        val rows = mutableListOf<List<String>>()

        var child = tableBlock.firstChild
        while (child != null) {
            when (child) {
                is TableHead -> {
                    var headRow = child.firstChild
                    while (headRow != null) {
                        if (headRow is TableRow) {
                            var cell = headRow.firstChild
                            while (cell != null) {
                                if (cell is TableCell) {
                                    headers.add(extractText(cell).trim())
                                }
                                cell = cell.next
                            }
                        }
                        headRow = headRow.next
                    }
                }
                is TableBody -> {
                    var bodyRow = child.firstChild
                    while (bodyRow != null) {
                        if (bodyRow is TableRow) {
                            val rowCells = mutableListOf<String>()
                            var cell = bodyRow.firstChild
                            while (cell != null) {
                                if (cell is TableCell) {
                                    rowCells.add(extractText(cell).trim())
                                }
                                cell = cell.next
                            }
                            if (rowCells.isNotEmpty()) {
                                rows.add(rowCells)
                            }
                        }
                        bodyRow = bodyRow.next
                    }
                }
            }
            child = child.next
        }

        return if (headers.isNotEmpty() || rows.isNotEmpty()) {
            TableData(headers, rows)
        } else {
            null
        }
    }

    /**
     * Extract plain text from a node and all its descendants.
     */
    private fun extractText(node: Node): String {
        val textBuilder = StringBuilder()
        node.accept(object : AbstractVisitor() {
            override fun visit(text: Text) {
                textBuilder.append(text.literal)
            }

            override fun visit(code: Code) {
                textBuilder.append(code.literal)
            }

            override fun visit(softLineBreak: SoftLineBreak) {
                textBuilder.append(" ")
            }
        })
        return textBuilder.toString().trim()
    }
}

/**
 * Convert TableData to natural speech text.
 * Reads like: "Flask, version 3.1.2, used for Backend web framework."
 */
private fun TableData.toSpeechText(): String {
    if (rows.isEmpty()) return "Empty table"
    
    val sb = StringBuilder()
    sb.append("Table with ${rows.size} items. ")
    
    rows.forEach { row ->
        row.forEachIndexed { index, value ->
            if (index < headers.size && headers[index].isNotBlank()) {
                val header = headers[index]
                when {
                    header.equals("Version", ignoreCase = true) -> {
                        sb.append("version $value, ")
                    }
                    header.equals("Purpose", ignoreCase = true) || 
                    header.equals("Description", ignoreCase = true) -> {
                        sb.append("for $value. ")
                    }
                    header.equals("Name", ignoreCase = true) ||
                    header.equals("Library", ignoreCase = true) ||
                    header.equals("Framework", ignoreCase = true) ||
                    header.contains("Framework", ignoreCase = true) -> {
                        sb.append("$value, ")
                    }
                    else -> {
                        sb.append("$header $value, ")
                    }
                }
            } else {
                sb.append("$value, ")
            }
        }
        if (!sb.endsWith(". ")) {
            sb.append(". ")
        }
    }
    
    return sb.toString().replace(", .", ".").trim()
}
