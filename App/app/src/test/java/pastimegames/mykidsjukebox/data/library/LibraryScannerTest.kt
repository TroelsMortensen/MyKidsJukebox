package pastimegames.mykidsjukebox.data.library

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryScannerTest {
    @Test
    fun directories_only_are_returned_as_folder_cards() {
        val entries = listOf(
            directoryEntry("Animals", "content://folders/animals", childFolders = 3, audioFiles = 0),
            directoryEntry("Stories", "content://folders/stories", childFolders = 1, audioFiles = 4)
        )

        val result = buildItemsFromEntries(entries)

        assertEquals(2, result.size)
        assertEquals(listOf(LibraryItemKind.Folder, LibraryItemKind.Folder), result.map { it.kind })
        assertEquals(listOf("Animals", "Stories"), result.map { it.name })
    }

    @Test
    fun mp3_files_are_returned_as_audio_cards_with_matching_jpg() {
        val entries = listOf(
            audioEntry("StoryA.mp3", "content://audio/storyA"),
            jpgEntry("StoryA.jpg", "content://art/storyA"),
            audioEntry("StoryB.mp3", "content://audio/storyB")
        )

        val result = buildItemsFromEntries(entries)

        assertEquals(2, result.size)
        assertEquals(listOf("StoryA", "StoryB"), result.map { it.name })
        assertEquals(listOf(LibraryItemKind.Audio, LibraryItemKind.Audio), result.map { it.kind })
        assertEquals(Uri.parse("content://art/storyA"), result[0].artworkUri)
        assertNull(result[1].artworkUri)
    }

    @Test
    fun mixed_entries_keep_both_folder_and_audio_items() {
        val entries = listOf(
            directoryEntry("FolderOne", "content://folders/one", childFolders = 0, audioFiles = 2),
            audioEntry("TrackOne.mp3", "content://audio/one"),
            jpgEntry("TrackOne.jpg", "content://art/one")
        )

        val result = buildItemsFromEntries(entries)

        assertEquals(2, result.size)
        assertEquals(setOf(LibraryItemKind.Folder, LibraryItemKind.Audio), result.map { it.kind }.toSet())
    }

    private fun directoryEntry(
        name: String,
        uri: String,
        childFolders: Int,
        audioFiles: Int
    ) = ScanEntry(
        name = name,
        uri = Uri.parse(uri),
        isDirectory = true,
        isMp3Audio = false,
        isJpg = false,
        folderDetails = FolderDetails(
            coverArtworkUri = null,
            childFolderCount = childFolders,
            audioFileCount = audioFiles
        )
    )

    private fun audioEntry(name: String, uri: String) = ScanEntry(
        name = name,
        uri = Uri.parse(uri),
        isDirectory = false,
        isMp3Audio = true,
        isJpg = false
    )

    private fun jpgEntry(name: String, uri: String) = ScanEntry(
        name = name,
        uri = Uri.parse(uri),
        isDirectory = false,
        isMp3Audio = false,
        isJpg = true
    )
}
