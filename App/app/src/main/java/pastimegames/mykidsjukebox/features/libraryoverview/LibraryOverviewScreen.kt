package pastimegames.mykidsjukebox.features.libraryoverview

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.launch
import pastimegames.mykidsjukebox.data.library.FolderGridItem
import pastimegames.mykidsjukebox.data.library.LibraryItemKind
import pastimegames.mykidsjukebox.data.library.LibraryScanner
import pastimegames.mykidsjukebox.data.settings.RootFolderStore
import pastimegames.mykidsjukebox.features.libraryoverview.components.EmptyFolderState
import pastimegames.mykidsjukebox.features.libraryoverview.components.FolderGrid
import pastimegames.mykidsjukebox.features.libraryoverview.components.LibraryHeader
import pastimegames.mykidsjukebox.features.libraryoverview.components.NavigationButtons
import pastimegames.mykidsjukebox.features.libraryoverview.components.SelectFolderState
import pastimegames.mykidsjukebox.storage.toDocumentFolder

@Composable
fun LibraryOverviewScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scanner = remember { LibraryScanner() }
    val folderStore = remember { RootFolderStore(context) }
    val scope = rememberCoroutineScope()
    val rootUriString by folderStore.rootUriFlow.collectAsState(initial = null)
    val folderStack = remember { mutableStateListOf<DocumentFile>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            scope.launch {
                folderStore.saveRootUri(uri.toString())
            }
            folderStack.clear()
            toDocumentFolder(context, uri)?.let { folderStack.add(it) }
            errorMessage = null
        } catch (_: SecurityException) {
            errorMessage = "Could not keep access to this folder. Please select it again."
        }
    }

    LaunchedEffect(rootUriString) {
        val savedUri = rootUriString ?: return@LaunchedEffect
        val doc = toDocumentFolder(context, Uri.parse(savedUri))
        if (doc == null || !doc.exists()) {
            scope.launch {
                folderStore.clearRootUri()
            }
            folderStack.clear()
            return@LaunchedEffect
        }

        if (folderStack.isEmpty()) {
            folderStack.add(doc)
        }
    }

    val currentFolder = folderStack.lastOrNull()
    val gridItems by produceState(initialValue = emptyList<FolderGridItem>(), currentFolder) {
        value = currentFolder?.let { scanner.listFolderItems(it) } ?: emptyList()
    }
    val hasBrowsableContent = currentFolder?.let { scanner.hasAnyBrowsableContent(it) } ?: false

    val uiState = LibraryOverviewState(
        isRootSelected = currentFolder != null,
        showBackButton = folderStack.size > 1,
        currentFolderName = currentFolder?.name ?: "Library",
        gridItems = gridItems,
        hasBrowsableContent = hasBrowsableContent,
        errorMessage = errorMessage
    )
    val actions = LibraryOverviewActions(
        onSelectFolderClick = { folderPicker.launch(null) },
        onBackClick = {
            if (folderStack.isNotEmpty()) {
                folderStack.removeAt(folderStack.lastIndex)
            }
        },
        onParentalSettingsClick = {
            // Placeholder for future parental settings screen.
        },
        onItemClick = onItemClick@{ clickedItem ->
            if (clickedItem.kind != LibraryItemKind.Folder) {
                return@onItemClick
            }
            toDocumentFolder(context, clickedItem.targetUri)?.let { folderStack.add(it) }
        }
    )

    LibraryOverviewContent(
        state = uiState,
        actions = actions,
        modifier = modifier
    )
}

@Composable
private fun LibraryOverviewContent(
    state: LibraryOverviewState,
    actions: LibraryOverviewActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!state.isRootSelected) {
            SelectFolderState(
                onSelectFolderClick = actions.onSelectFolderClick,
                errorMessage = state.errorMessage
            )
            return@Column
        }

        NavigationButtons(
            showBackButton = state.showBackButton,
            onBackClick = actions.onBackClick,
            onParentalSettingsClick = actions.onParentalSettingsClick
        )
        LibraryHeader(title = state.currentFolderName)

        if (state.gridItems.isEmpty()) {
            EmptyFolderState(hasBrowsableContent = state.hasBrowsableContent)
        } else {
            FolderGrid(
                items = state.gridItems,
                onItemClick = actions.onItemClick
            )
        }
    }
}
