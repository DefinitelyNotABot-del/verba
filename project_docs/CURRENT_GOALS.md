# Verba — Current Goals

> **Single-phase execution.** Only one phase is active at a time. Complete all items before advancing.

---

## Operating Rules

1. **No Gradle execution** — Provide instructions only; user builds in Android Studio
2. **SAF-only file access** — Never assume file paths; use URIs from Storage Access Framework
3. **Offline TTS only** — No cloud speech services unless explicitly requested
4. **Version catalog** — All dependencies go through `gradle/libs.versions.toml`

---

## Phase 4 — UI Polish & GitHub Integration (ACTIVE)

### Objective

Refine the user interface, remove redundant controls, fix theme toggle, add read/edit mode, and publish to GitHub.

### Status

| Task | Status | Notes |
|------|--------|-------|
| Remove stop button from TTS bar | ✅ Complete | Keep only play/pause |
| Remove bottom Voice/Speed buttons | ✅ Complete | Settings handles these |
| Fix voice name display | ✅ Complete | Show engine name, not "Local" |
| Fix theme toggle | ✅ Complete | Light/Dark only |
| Fix table rendering | ✅ Complete | Proper bordered grid |
| Add Read/Edit mode toggle | ✅ Complete | VS Code/Obsidian style |
| Add auto-update from GitHub | ✅ Complete | Check releases for updates |
| Push to GitHub | ✅ Complete | DefinitelyNotABot-del/verba |
| Create project_docs | ✅ Complete | SESSION_HISTORY + CURRENT_GOALS |
| Context-aware TTS | ✅ Complete | ML=machine learning in tech docs |

---

## Locked Architecture

> These decisions are final and must not be changed without explicit approval.

| Component | Decision | Rationale |
|-----------|----------|-----------|
| Document Model | `DocBlock` + `Document` | Unified representation for all formats |
| Markdown Parser | CommonMark + GFM Tables | Standard spec compliance |
| TTS Engine | Android TextToSpeech | Offline-first, system voices |
| File Access | SAF OpenDocument | Android 13+ scoped storage |
| State Management | ViewModel + StateFlow | Compose-compatible, lifecycle-aware |
| Theme System | Material 3 Dynamic Color | Modern Android theming |

---

## Phase Backlog

### Phase 5 — PDF Support
- [ ] Implement PdfParser using Android PdfRenderer
- [ ] Handle page-based content extraction
- [ ] Mark PDF blocks as non-editable

### Phase 6 — DOCX Support
- [ ] Add Apache POI XWPF dependency
- [ ] Implement DocxParser for paragraph extraction
- [ ] Enable editing for DOCX format

### Phase 7 — Editor Mode
- [ ] Create EditorScreen with text editing
- [ ] Implement save functionality via SAF
- [ ] Add Markdown preview toggle

---

## Release Checklist

- [ ] All Phase 4 tasks complete
- [ ] Debug APK builds successfully
- [ ] Core TTS flow tested manually
- [ ] GitHub repository created and pushed
- [ ] README.md with installation instructions
- [ ] Release APK signed and tagged

---

## Change Log

| Date | Change | Phase |
|------|--------|-------|
| 2025 | Initial architecture established | 1 |
| 2025 | Markdown parser + TTS controller | 1 |
| 2025 | Reader UI + Settings screen | 2 |
| 2025 | Table support + code blocks | 3 |
| 2025 | Project documentation created | 4 |
