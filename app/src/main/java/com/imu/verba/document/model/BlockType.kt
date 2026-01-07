package com.imu.verba.document.model

/**
 * Represents the semantic type of a document block.
 * Used for rendering style decisions and TTS pacing.
 */
enum class BlockType {
    HEADING,
    PARAGRAPH,
    LIST,
    TABLE,
    CODE_BLOCK
}
