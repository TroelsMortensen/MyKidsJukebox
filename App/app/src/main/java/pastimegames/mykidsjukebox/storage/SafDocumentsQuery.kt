package pastimegames.mykidsjukebox.storage

import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.provider.DocumentsContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job

data class SafChildDocument(
    val documentId: String,
    val displayName: String?,
    val mimeType: String?,
    val flags: Int,
    val documentUri: Uri
) {
    val isDirectory: Boolean
        get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
}

suspend fun queryChildDocuments(
    context: Context,
    folderUri: Uri
): List<SafChildDocument> {
    val result = mutableListOf<SafChildDocument>()
    forEachChildDocument(context, folderUri) { result += it }
    return result
}

suspend fun forEachChildDocument(
    context: Context,
    folderUri: Uri,
    onDocument: suspend (SafChildDocument) -> Unit
): Boolean {
    val resolver = context.contentResolver
    val folderDocumentId = try {
        DocumentsContract.getDocumentId(folderUri)
    } catch (_: IllegalArgumentException) {
        return false
    }

    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, folderDocumentId)
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_FLAGS
    )
    val cancellationSignal = CancellationSignal()
    val cancelHandle = currentCoroutineContext().job.invokeOnCompletion {
        cancellationSignal.cancel()
    }

    return try {
        resolver.query(
            childrenUri,
            projection,
            null,
            null,
            null,
            cancellationSignal
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val flagsIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)
            if (idIndex < 0 || mimeIndex < 0) {
                return false
            }

            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val documentId = cursor.getString(idIndex) ?: continue
                val displayName = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                val mimeType = if (mimeIndex >= 0) cursor.getString(mimeIndex) else null
                val flags = if (flagsIndex >= 0) cursor.getInt(flagsIndex) else 0
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, documentId)
                onDocument(
                    SafChildDocument(
                        documentId = documentId,
                        displayName = displayName,
                        mimeType = mimeType,
                        flags = flags,
                        documentUri = documentUri
                    )
                )
            }
        } ?: return false
        true
    } catch (e: CancellationException) {
        throw e
    } catch (_: OperationCanceledException) {
        currentCoroutineContext().ensureActive()
        false
    } catch (_: Exception) {
        false
    } finally {
        cancelHandle.dispose()
    }
}
