package pastimegames.mykidsjukebox.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

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

fun queryChildDocuments(
    context: Context,
    folderUri: Uri
): List<SafChildDocument> {
    val result = mutableListOf<SafChildDocument>()
    forEachChildDocument(context, folderUri) { result += it }
    return result
}

fun forEachChildDocument(
    context: Context,
    folderUri: Uri,
    onDocument: (SafChildDocument) -> Unit
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

    return try {
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val flagsIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)
            if (idIndex < 0 || mimeIndex < 0) {
                return false
            }

            while (cursor.moveToNext()) {
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
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }
}
