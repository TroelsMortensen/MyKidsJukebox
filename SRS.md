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
- Persistence: DataStore (root folder URI); Room (per-audiobook playback progress)

---

## UI layout plan
- Initially two views:
  - The select specific book to play
  - Showing the currently playing book, with progress bar, play/pause, and a restart control (seek to start + pause)

Later a settings screen will be added to allow the user to configure various aspects of the app. This will be parental locked, similar to YouTube Kids.



## Task 4: UI

- I stedet for "this folder is empty", lave noget loading graphic....

## Task

Queue loop around. 

## Task 5: Persistence & Logic
*Goal: Make it a reliable tool.*

### Implemented
- **Sub-task 4.1 (done):** Save playback position in Room (`PlaybackProgressRepository`), keyed by audio URI. `PlaybackService` upserts about every 10s while playing, and on pause / destroy. Keeps the last **20** books; prunes on book start (not app start). Near-start / near-end positions are cleared.
- **Sub-task 4.2 (done):** Auto-resume: when a file is opened (queue apply or track change), start from the saved position.
- Player **restart** control: seek to 0 and pause (clears meaningful saved progress via pause-save rules). Closing the player pauses without seeking to 0 so progress is preserved.

### Still open
- **Sub-task 4.3:** Hidden Settings: Implement a 3-second long-press on the screen corner to allow re-selecting the root folder.
- **Sub-task 4.4:** Parental Settings: Add a protected "Change Library Folder" option in the parental settings screen instead of the main browsing view.

## Task 6: Parental Settings
* Title på front page, det skal lige have en pill shape, og hvis man holder inde i 5 sekunder, får man menu page. Her kan man skifte root dir.

To be figured out.


## Task: Spotify

Det kunne være smart hvis jeg kunne linke til spotify.

## Task: app feedback

- Tiny audio cue on successful taps (optional, careful not distracting).