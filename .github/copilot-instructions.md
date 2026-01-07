# Verba - AI Coding Instructions

## Project Overview
**Verba** is an offline-first Android document reader/editor with Text-to-Speech (TTS) support. Opens Markdown (.md), PDF (.pdf), and Word (.docx) files with read/edit modes and block-by-block TTS playback.

## Architecture (Layered, Clean)
```
app/src/main/java/com/imu/verba/
├── ui/               → Compose screens (ReaderScreen, EditorScreen)
├── viewmodel/        → State & logic (ReaderViewModel, EditorViewModel)
├── document/
│   ├── parser/       → Format-specific parsers (MarkdownParser, PdfParser, DocxParser)
│   └── model/        → Document data structures (DocBlock, Document)
├── tts/              → TextToSpeechController (Android TTS wrapper)
└── storage/          → File access via SAF (FileAccessHelper)
```

## Core Document Model (Single Source of Truth)
All formats MUST normalize to this model for rendering, editing, and TTS:
```kotlin
data class DocBlock(val id: Int, val text: String, val type: BlockType, val editable: Boolean)
data class Document(val name: String, val blocks: List<DocBlock>, val format: DocumentFormat)
enum class BlockType { HEADING, PARAGRAPH, LIST }
enum class DocumentFormat { MARKDOWN, PDF, DOCX }
```

## Technology Stack
| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose + Material 3 |
| Build | Gradle KTS with version catalog (`gradle/libs.versions.toml`) |
| Min SDK | 33 (Android 13) |
| TTS | Android `TextToSpeech` (offline, no cloud) |
| File Access | Storage Access Framework (SAF) - never assume file paths |
| Markdown | CommonMark parser (`org.commonmark:commonmark`) |
| Word | Apache POI XWPF |
| PDF | Android PdfRenderer (read-only) |

## Key Patterns

### Adding Dependencies
Always use the version catalog in `gradle/libs.versions.toml`:
```toml
[versions]
commonmark = "0.22.0"

[libraries]
commonmark = { group = "org.commonmark", name = "commonmark", version.ref = "commonmark" }
```
Then reference in `app/build.gradle.kts`: `implementation(libs.commonmark)`

### Composables
- Wrap all screens in `VerbaTheme { }` (see `ui/theme/Theme.kt`)
- Use `Scaffold` with `innerPadding` for edge-to-edge display
- State hoisting: screens receive state, emit events to ViewModel

### TTS Controller Pattern
- Block-by-block playback with `onBlockStarted(blockId)` callback for UI highlighting
- Support start/end block selection (long-press context menu)
- Lifecycle tied to ViewModel scope

### File Access (SAF)
```kotlin
// Use ActivityResultContracts.OpenDocument for file picking
val launcher = rememberLauncherForActivityResult(OpenDocument()) { uri -> ... }
launcher.launch(arrayOf("text/markdown", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
```

## Build & Run
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Install on connected device
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests
```

## Format-Specific Rules
| Format | Editable | Parser Output |
|--------|----------|---------------|
| Markdown | ✅ Yes | Headings, paragraphs, lists via CommonMark AST |
| PDF | ❌ No | Text extraction with paragraph boundaries |
| DOCX | ✅ Yes | Paragraph-based via Apache POI XWPF |

## Do NOT
- Use experimental/deprecated Android APIs
- Invent non-existent Android features
- Assume file paths (always use SAF URIs)
- Use cloud TTS unless explicitly requested
- Skip imports in code generation
- Create monolithic files (split by responsibility)
- **Run Gradle commands or build the project** — Only provide instructions; the user will build/run in Android Studio
- Execute `./gradlew`, `adb`, or any terminal build commands — If testing is needed, describe what to test and the user will run it manually
