package pastimegames.mykidsjukebox.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jukebox_prefs")

class RootFolderStore(private val context: Context) {
    private val rootUriKey: Preferences.Key<String> = stringPreferencesKey("root_folder_uri")

    val rootUriFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[rootUriKey]
    }

    suspend fun saveRootUri(uri: String) {
        context.dataStore.edit { prefs ->
            prefs[rootUriKey] = uri
        }
    }

    suspend fun clearRootUri() {
        context.dataStore.edit { prefs ->
            prefs.remove(rootUriKey)
        }
    }
}
