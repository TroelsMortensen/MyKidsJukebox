# SRS: Toddler Audio Jukebox (Non-Reader Edition)

## Project Goal
A low-text, visual-first Android audiobook player. The app allows a non-reading child to navigate a library of MP3s (or other audio formats) by clicking on large book-like covers. It is designed to be "Pinned" via Android App Pinning to act as a single-purpose device.
It is possible to organize the audio files into folders, each folder can have a cover image.

## Tech Stack
- Language: Kotlin
- UI: Jetpack Compose
- Media: Media3 ExoPlayer
- Image Loading: Coil
- Storage: Storage Access Framework (SAF)

---

## UI layout plan
- Initially two views:
  - The select specific book to play
  - Showing the currently playing book, with progress bar and play/pause button

Later a settings screen will be added to allow the user to configure various aspects of the app. This will be parental locked, similar to YouTube Kids.

## Task 1: File System Proof of Concept & Media Discovery

### Goal
Establish persistent access to the Android file system and recursively map the library into a hierarchical data model (Universe > Series > Episode).


### 1. Technical Requirements
* **Sub-task 1.1: Root Folder Picker**
    * Implement `ActivityResultContracts.OpenDocumentTree` (Storage Access Framework) to allow the user to select the **Audiobooks (Root)** folder.
* **Sub-task 1.2: Permission Persistence**
    * Request and persist 'Take Persistable Uri Permission' using `contentResolver.takePersistableUriPermission`.
    * The app must "remember" this URI (using SharedPreferences or DataStore) so the folder remains accessible after a device reboot or app restart.
* **Sub-task 1.3: Recursive Media Scan**
    * Scan the selected root and all sub-folders for files with extensions: `.mp3`, `.m4a`, and `.wav`.
* **Sub-task 1.4: Initial Discovery UI**
    * If no root is selected, display a message/button: "Select Audiobooks Root Folder".
    * Once selected, display a simple **Grid of Cards** for the current level.
    * Each card must show a default 'Folder' icon (or artwork) and the raw filename for development purposes.

---

### 2. Folder Hierarchy & Discovery Logic
The app treats folders as either containers or playable content based on their contents:

* **Universe (Top-level):** Any folder directly inside the Root.
* **Series (Sub-series):** Any folder inside a Universe.
* **Episode (Track):** Any individual audio file.

#### Artwork Priority Rules:
1.  **For Universes/Series:** Look for `cover.jpg` (or `artwork.jpg`) inside the folder.
2.  **For Episodes:** Look for a `.jpg` with the **exact same name** as the audio file (e.g., `Book 1.mp3` -> `Book 1.jpg`).
3.  **Fallback:** Use a Material Design icon placeholder if no image is found.

---

### 3. Data Model
Implement a `sealed class` structure in Kotlin to represent the tree:

```kotlin
sealed class MediaNode {
    abstract val title: String
    abstract val artworkUri: Uri?

    data class Universe(
        override val title: String,
        override val artworkUri: Uri?,
        val series: List<Series>
    ) : MediaNode()

    data class Series(
        override val title: String,
        override val artworkUri: Uri?,
        val episodes: List<Episode>
    ) : MediaNode()

    data class Episode(
        override val title: String,
        override val artworkUri: Uri?,
        val audioUri: Uri,
        val duration: Long = 0 
    ) : MediaNode()
}

## Task 2: Visual Library (The Grid)
*Goal: Replace text with the 'Netflix-style' grid.*

- **Sub-task 2.1:** For every folder found, look for an image file named `cover.jpg` or `folder.jpg`.
- **Sub-task 2.2:** Create a `LazyVerticalGrid` (2 columns) displaying these images as large buttons.
- **Sub-task 2.3:** Implement a "Series View": clicking a folder icon opens a sub-grid of the audio files inside that specific folder.

Automatically resizes/samples the images to the size of the UI container to save memory.

Make the "Back" button massive. In a nested world, the most common thing a toddler does is tap into the wrong folder and need an easy way out!



## Task 3: The Minimalist Player
*Goal: Audio playback with zero distractions.*

- **Sub-task 3.1:** Integrate Media3 ExoPlayer.
- **Sub-task 3.2:** Create a Player UI with ONLY:
    - Giant Play/Pause button.
    - Large 'Back' button (top left).
    - Large 'Rewind 30s' button.
- **Sub-task 3.3:** Background playback support (so the screen can be off).

## Task 4: Persistence & Logic
*Goal: Make it a reliable tool.*

- **Sub-task 4.1:** Save current playback position (Timestamp) for every file in a local Room database or DataStore.
- **Sub-task 4.2:** Auto-resume: When a file is clicked, start from the saved timestamp.
- **Sub-task 4.3:** Hidden Settings: Implement a 3-second long-press on the screen corner to allow re-selecting the root folder.