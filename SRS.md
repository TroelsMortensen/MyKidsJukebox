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




## Task 3: The Minimalist Player
*Goal: Audio playback with zero distractions.*

- **Sub-task 3.3:** Background playback support (so the screen can be off).

## Task 4: UI

- The right side time-left needs more space. 10:04 made a line break.
- player screen, vertical, the main artwork grows too large.
- player screen, horizontal, layout just messed up.

## Task 5: Persistence & Logic
*Goal: Make it a reliable tool.*

- **Sub-task 4.1:** Save current playback position (Timestamp) for every file in a local Room database or DataStore.
- **Sub-task 4.2:** Auto-resume: When a file is clicked, start from the saved timestamp.
- **Sub-task 4.3:** Hidden Settings: Implement a 3-second long-press on the screen corner to allow re-selecting the root folder.
- **Sub-task 4.4:** Parental Settings: Add a protected "Change Library Folder" option in the parental settings screen instead of the main browsing view.

## Task 6: Parental Settings

To be figured out.

- Optional “simple mode” toggle in parental settings (e.g., hide times/progress).

## Task: Spotify

Det kunne være smart hvis jeg kunne linke til spotify.

## Task: app feedback

- Tiny audio cue on successful taps (optional, careful not distracting).