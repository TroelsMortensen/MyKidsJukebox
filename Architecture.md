## Project Context: MyKidsJukebox (Current Architecture)

### Goal
A toddler-friendly Android audiobook browser/player with very large touch targets and minimal text/distractions.

### Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Image loading:** Coil (`AsyncImage`)
- **Storage access:** Storage Access Framework (SAF) via `OpenDocumentTree` + `DocumentFile`
- **Persistence:** DataStore Preferences (stores selected root folder URI)
- **Media playback:** Planned via Media3 ExoPlayer (not yet primary focus in current code path)

---

## Current High-Level Architecture

### 1) Entry + Screen Composition
- **File:** `App/app/src/main/java/pastimegames/mykidsjukebox/MainActivity.kt`
- `MainActivity` sets Compose content and renders `LibraryBrowserScreen(...)`.
- `LibraryBrowserScreen` handles:
  - Folder picker launch (`ActivityResultContracts.OpenDocumentTree`)
  - Persisted SAF permission acquisition (`takePersistableUriPermission`)
  - Root URI restore from DataStore
  - In-memory folder navigation stack (`mutableStateListOf<DocumentFile>()`)
  - Grid rendering for current folder level

### 2) Root Folder Persistence
- **File:** `App/app/src/main/java/pastimegames/mykidsjukebox/RootFolderStore.kt`
- Stores root folder URI string in DataStore.
- Exposes `rootUriFlow` for startup restoration.
- Supports save/clear operations.

### 3) Folder Discovery / Scan Layer
- **File:** `App/app/src/main/java/pastimegames/mykidsjukebox/LibraryScanner.kt`
- Defines `FolderGridItem` model (name, folder URI, artwork URI, subfolder count, audio file count).
- `listFolderItems(currentFolder: DocumentFile)`:
  - Lists child directories of current folder
  - Counts subdirectories and audio files (`.mp3`, `.m4a`, `.wav`)
  - Finds artwork (`cover.jpg` then `artwork.jpg`)
- `hasAnyBrowsableContent(...)` used for empty-state messaging.

### 4) UI Card Definition
- **File:** `App/app/src/main/java/pastimegames/mykidsjukebox/MainActivity.kt`
- `FolderGrid(...)` renders `LazyVerticalGrid(columns = 2)`.
- Each card shows:
  - Artwork (or placeholder icon)
  - Folder name
  - Metadata line: `X folders | Y audio`
- Includes large Back button for nested navigation.
- Includes placeholder **Parental Settings** button (currently no-op).

---

## Storage / Permission Model (Current)
- Uses **SAF tree URI** instead of direct `File` access for reliability on scoped storage Android versions.
- Permission is persisted per selected root folder URI.
- App can restore access after restart/reboot (if URI remains valid).

---

## Data Flow Summary

1. App starts -> reads saved root URI from DataStore  
2. If missing/invalid -> shows “Select Library Folder” UI  
3. User picks folder via SAF -> app persists permission + URI  
4. Scanner builds `FolderGridItem` list for current folder  
5. UI renders cards; tapping a card pushes next folder to stack  

---

## Current UX Behavior
- If no root selected: big select-folder CTA
- If root selected: folder grid is shown
- Card metadata currently includes subfolder/audio counts
- Missing artwork uses a colorful placeholder folder icon
- “Change library folder” is intended to move to parental settings (main view no longer expected to expose it directly)

---

## Key Files to Mention in Future Prompts
- `App/app/src/main/java/pastimegames/mykidsjukebox/MainActivity.kt`
- `App/app/src/main/java/pastimegames/mykidsjukebox/LibraryScanner.kt`
- `App/app/src/main/java/pastimegames/mykidsjukebox/RootFolderStore.kt`
- `App/app/src/main/AndroidManifest.xml`
- `SRS.md`
- `.cursorrules`

---

## Useful Prompt Context Snippet (copy/paste)
- “This is an Android Kotlin + Compose app for toddler audiobook browsing.”
- “Library access uses SAF (`OpenDocumentTree`, `DocumentFile`) with persisted URI in DataStore.”
- “Folder grid rendering lives in `MainActivity.kt` (`FolderGrid`/`FolderArtwork`).”
- “Folder scanning/model lives in `LibraryScanner.kt` (`FolderGridItem`, child counts, artwork lookup).”
- “Current UX priorities: giant buttons, minimal text, simple navigation, parental settings for sensitive actions.”