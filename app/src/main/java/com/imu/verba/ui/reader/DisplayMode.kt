package com.imu.verba.ui.reader

/**
 * Display mode for the reader screen.
 * Like VS Code/Obsidian, allows switching between viewing and editing.
 */
enum class DisplayMode {
    /** Read-only mode - optimized for reading with TTS */
    READ,
    /** Edit mode - allows editing document content (future feature) */
    EDIT
}
