# Verba — Session History

> **Data-only ledger.** This file records completed work sessions. Rules and authority reside in `.github/copilot-instructions.md`.

---

## Skip To

- [Session 1 — Initial Setup](#session-1)

---

## Session 1

**Started at:** Pre-recorded (Development began before session logging)

**Duration:** Multiple sessions prior to formal logging

### Summary

Built the foundational Verba Android app — an offline-first document reader with Text-to-Speech support for Markdown files. Established the core architecture following Clean Architecture principles with MVVM pattern.

### Tasks Completed

#### Phase 1 — Core Infrastructure
- [x] Created document model (`DocBlock`, `Document`, `BlockType`, `DocumentFormat`)
- [x] Implemented CommonMark-based Markdown parser with GFM tables extension
- [x] Built TextToSpeechController with block-by-block playback
- [x] Implemented SAF-based FileAccessHelper
- [x] Created ReaderViewModel with TTS state management

#### Phase 2 — UI Implementation
- [x] Built ReaderScreen with document block rendering
- [x] Implemented file picker via OpenDocument contract
- [x] Created Obsidian-style SettingsScreen with voice/speed selection
- [x] Added navigation between Reader and Settings
- [x] Designed slim bottom TTS control bar

#### Phase 3 — Table Support
- [x] Added TABLE and CODE_BLOCK to BlockType
- [x] Created TableData model for structured tables
- [x] Implemented VS Code-style TableBlock composable
- [x] Added CodeBlock composable for code rendering

### Files Created

| File | Purpose |
|------|---------|
| `document/model/DocBlock.kt` | Atomic document content unit |
| `document/model/Document.kt` | Document container with blocks |
| `document/model/BlockType.kt` | Block type enumeration |
| `document/model/DocumentFormat.kt` | Supported format enum |
| `document/model/TableData.kt` | Structured table data |
| `document/parser/MarkdownParser.kt` | CommonMark MD to DocBlock |
| `tts/TextToSpeechController.kt` | TTS engine wrapper |
| `storage/FileAccessHelper.kt` | SAF file operations |
| `viewmodel/ReaderViewModel.kt` | Reader state management |
| `ui/reader/ReaderScreen.kt` | Document display UI |
| `ui/settings/SettingsScreen.kt` | Obsidian-style settings |
| `ui/components/TableBlock.kt` | Table/code rendering |
| `.github/copilot-instructions.md` | AI coding guidelines |

### Files Modified

- `MainActivity.kt` — Added file picker, navigation, settings
- `gradle/libs.versions.toml` — Added CommonMark dependencies
- `app/build.gradle.kts` — Added library dependencies

### Final State

Core reading functionality operational with Markdown support. TTS plays blocks sequentially with voice and speed selection. Settings screen provides Obsidian-style grouped preferences. Ready for UI refinements and additional features.

---

Session 1 end
