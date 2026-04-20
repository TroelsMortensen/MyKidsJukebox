package pastimegames.mykidsjukebox.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

fun toDocumentFolder(context: Context, uri: Uri): DocumentFile? {
    return DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)
}
