## Project Context: MyKidsJukebox (Current Architecture)

### Goal
A toddler-friendly Android audiobook browser/player with very large touch targets and minimal text/distractions.

### Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Image loading:** Coil (`AsyncImage`)
- **Storage access:** Storage Access Framework (SAF) via `OpenDocumentTree` + `DocumentFile`
- **Persistence:** DataStore Preferences (stores selected root folder URI)
- **Media playback:** Media3 ExoPlayer with a foreground `PlaybackService` (see `features/playerview/`)

---

## Current High-Level Architecture

### 1) Entry + Screen Composition
- **File:** `App/app/src/main/java/pastimegames/mykidsjukebox/MainActivity.kt`
- `MainActivity` sets Compose content, a short splash, then switches between **`LibraryOverviewScreen`** (library) and **`PlayerScreen`** (playback) via `PlayerRoute`.
- **`LibraryOverviewScreen`** (`features/libraryoverview/LibraryOverviewScreen.kt`) handles:
  - Folder picker launch (`ActivityResultContracts.OpenDocumentTree`)
  - Persisted SAF permission acquisition (`takePersistableUriPermission`)
  - Root URI restore from DataStore
  - In-memory folder navigation stack (list of folder URI strings)
  - Delegates grid rendering to `FolderGrid` for the current folder level

### 2) Root Folder Persistence
- **File:** `App/app/src/main/java/pastimegames/mykidsjukebox/data/settings/RootFolderStore.kt`
- Stores root folder URI string in DataStore.
- Exposes `rootUriFlow` for startup restoration.
- Supports save/clear operations.

### 3) Folder Discovery / Scan Layer
- **Files:** `App/app/src/main/java/pastimegames/mykidsjukebox/data/library/LibraryScanner.kt`, `FolderGridItem.kt`
- Defines `FolderGridItem` model (name, target URI, artwork URI, kind, counts for folders).
- `scanFolderItemsIncremental(...)` streams `ScanEvent` batches for the current folder:
  - Child **directories** become folder cards; metadata pass resolves **`cover.jpg`** (case-insensitive filename) per folder and subfolder/audio counts.
  - **Audio:** **`.mp3` only** (other extensions are not indexed as playable).
  - **Per-track art:** sibling **`.jpg`** whose basename matches the MP3 basename (e.g. `Track.mp3` + `Track.jpg`).
  - On completion, exposes sorted MP3 list (`sortedBy` on name, case-insensitive) for the player queue.
- Empty / loading messaging is driven by scan progress and `hasBrowsableContent` from the incremental scan.

### 4) UI Card Definition
- **Files:** `features/libraryoverview/components/FolderGrid.kt`, `FolderGridCard.kt`, `FolderArtwork.kt`
- `FolderGrid` renders `LazyVerticalGrid` with **2 columns** in portrait and **3** in landscape.
- Each card shows:
  - Artwork (or placeholder icon)
  - Name
  - For folders: resolved counts as metadata (`X folders | Y audio` when available)
  - For audio: large **Play** button on the card
- **Navigation:** large back control via `NavigationButtons` when not at library root (nested folders).

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
5. UI renders cards; tapping a **folder** card pushes that folder URI onto the stack; **Play** on an MP3 card opens the player with the current folder’s sorted queue  

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
- `App/app/src/main/java/pastimegames/mykidsjukebox/data/library/LibraryScanner.kt`
- `App/app/src/main/java/pastimegames/mykidsjukebox/data/settings/RootFolderStore.kt`
- `App/app/src/main/AndroidManifest.xml`
- `SRS.md`
- `.cursorrules`

---

## Useful Prompt Context Snippet (copy/paste)
- “This is an Android Kotlin + Compose app for toddler audiobook browsing.”
- “Library access uses SAF (`OpenDocumentTree`, `DocumentFile`) with persisted URI in DataStore.”
- “Folder grid rendering lives in `features/libraryoverview/components/` (`FolderGrid`, `FolderGridCard`, `FolderArtwork`).”
- “Folder scanning/model lives in `data/library/LibraryScanner.kt` (`FolderGridItem`, incremental scan, `cover.jpg`, sibling `.jpg` for MP3s).”
- “Current UX priorities: giant buttons, minimal text, simple navigation, parental settings for sensitive actions.”