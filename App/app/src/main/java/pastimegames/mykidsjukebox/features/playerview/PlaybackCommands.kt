package pastimegames.mykidsjukebox.features.playerview

import android.net.Uri
import android.os.Bundle

private const val KEY_TITLES = "titles"
private const val KEY_AUDIO_URIS = "audio_uris"
private const val KEY_ARTWORK_URIS = "artwork_uris"
private const val KEY_START_INDEX = "start_index"
private const val KEY_WINDOW_SIZE = "window_size"

const val COMMAND_SET_QUEUE = "set_queue"

data class QueueCommandPayload(
    val items: List<PlayerQueueItem>,
    val startIndex: Int,
    val queueWindowSize: Int
)

fun QueueCommandPayload.toBundle(): Bundle {
    return Bundle().apply {
        putStringArrayList(KEY_TITLES, ArrayList(items.map { it.title }))
        putStringArrayList(KEY_AUDIO_URIS, ArrayList(items.map { it.audioUri.toString() }))
        putStringArrayList(KEY_ARTWORK_URIS, ArrayList(items.map { it.artworkUri?.toString().orEmpty() }))
        putInt(KEY_START_INDEX, startIndex)
        putInt(KEY_WINDOW_SIZE, queueWindowSize)
    }
}

fun Bundle.toQueueCommandPayloadOrNull(): QueueCommandPayload? {
    val titles = getStringArrayList(KEY_TITLES) ?: return null
    val audioUris = getStringArrayList(KEY_AUDIO_URIS) ?: return null
    val artworkUris = getStringArrayList(KEY_ARTWORK_URIS) ?: return null
    if (titles.size != audioUris.size || audioUris.size != artworkUris.size) {
        return null
    }

    val items = titles.indices.mapNotNull { index ->
        val uriString = audioUris[index]
        if (uriString.isBlank()) {
            return@mapNotNull null
        }
        val artworkUri = artworkUris[index].takeIf { it.isNotBlank() }?.let(Uri::parse)
        PlayerQueueItem(
            title = titles[index],
            audioUri = Uri.parse(uriString),
            artworkUri = artworkUri
        )
    }
    if (items.isEmpty()) {
        return null
    }

    return QueueCommandPayload(
        items = items,
        startIndex = getInt(KEY_START_INDEX, 0),
        queueWindowSize = getInt(KEY_WINDOW_SIZE, 5)
    )
}
