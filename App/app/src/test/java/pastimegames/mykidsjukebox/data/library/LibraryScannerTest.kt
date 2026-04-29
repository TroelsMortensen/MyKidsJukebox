package pastimegames.mykidsjukebox.data.library

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryScannerTest {
    @Test
    fun audio_shell_items_are_enriched_with_matching_jpg() {
        val shells = listOf(
            FolderGridItem(
                name = "StoryA",
                targetUri = Uri.parse("content://audio/storyA"),
                artworkUri = null,
                artworkIsLoading = true,
                kind = LibraryItemKind.Audio
            ),
            FolderGridItem(
                name = "StoryB",
                targetUri = Uri.parse("content://audio/storyB"),
                artworkUri = null,
                artworkIsLoading = true,
                kind = LibraryItemKind.Audio
            )
        )

        val updates = buildAudioArtworkUpdates(
            audioShellItems = shells,
            siblingJpgByLowerName = mapOf(
                "storya.jpg" to Uri.parse("content://art/storyA")
            )
        )

        assertEquals(Uri.parse("content://art/storyA"), updates[0].artworkUri)
        assertFalse(updates[0].artworkIsLoading)
        assertNull(updates[1].artworkUri)
        assertFalse(updates[1].artworkIsLoading)
    }

    @Test
    fun has_jpg_extension_is_case_insensitive() {
        assertTrue("cover.jpg".hasJpgExtension())
        assertTrue("cover.JPG".hasJpgExtension())
        assertFalse("track.mp3".hasJpgExtension())
    }
}
