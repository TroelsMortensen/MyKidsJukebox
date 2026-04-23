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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
fun LibraryOverviewScreen(
    folderStackUris: SnapshotStateList<String>,
    onOpenPlayer: (List<FolderGridItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scanner = remember { LibraryScanner() }
    val folderStore = remember { RootFolderStore(context) }
    val scope = rememberCoroutineScope()
    val rootUriString by folderStore.rootUriFlow.collectAsState(initial = null)
    val artworkCacheByFolder = remember { mutableStateMapOf<String, MutableMap<String, Uri?>>() }
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
            folderStackUris.clear()
            folderStackUris.add(uri.toString())
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
            folderStackUris.clear()
            return@LaunchedEffect
        }

        if (folderStackUris.isEmpty()) {
            folderStackUris.add(savedUri)
        }
    }

    val currentFolder = folderStackUris
        .lastOrNull()
        ?.let { toDocumentFolder(context, Uri.parse(it)) }
    LaunchedEffect(currentFolder, folderStackUris.size) {
        if (folderStackUris.isNotEmpty() && currentFolder == null) {
            folderStackUris.removeAt(folderStackUris.lastIndex)
        }
    }
    var gridItems by remember(currentFolder?.uri?.toString()) { mutableStateOf(emptyList<FolderGridItem>()) }
    var hasBrowsableContent by remember(currentFolder?.uri?.toString()) { mutableStateOf(false) }
    LaunchedEffect(currentFolder?.uri?.toString()) {
        val folder = currentFolder
        if (folder == null) {
            gridItems = emptyList()
            hasBrowsableContent = false
            return@LaunchedEffect
        }

        val quickScan = withContext(Dispatchers.IO) { scanner.listFolderItemsQuick(folder) }
        gridItems = quickScan.items
        hasBrowsableContent = quickScan.items.isNotEmpty() ||
            withContext(Dispatchers.IO) { scanner.hasAnyBrowsableContent(folder) }

        val folderKey = folder.uri.toString()
        val folderArtworkCache = artworkCacheByFolder.getOrPut(folderKey) { mutableMapOf() }
        coroutineScope {
            quickScan.items.forEach { baseItem ->
                launch(Dispatchers.IO) {
                    val itemKey = baseItem.targetUri.toString()
                    val cachedArtworkUri = folderArtworkCache[itemKey]
                    val resolvedArtworkUri = cachedArtworkUri ?: scanner
                        .resolveArtworkForItem(baseItem, quickScan)
                        .also { resolved -> folderArtworkCache[itemKey] = resolved }
                    val folderCounts = scanner.resolveFolderCounts(baseItem, quickScan)

                    withContext(Dispatchers.Main) {
                        gridItems = gridItems.map { item ->
                            if (item.targetUri == baseItem.targetUri) {
                                item.copy(
                                    artworkUri = resolvedArtworkUri ?: item.artworkUri,
                                    artworkIsLoading = false,
                                    childFolderCount = folderCounts?.childFolderCount ?: item.childFolderCount,
                                    audioFileCount = folderCounts?.audioFileCount ?: item.audioFileCount
                                )
                            } else {
                                item
                            }
                        }
                    }
                }
            }
        }
    }

    val uiState = LibraryOverviewState(
        isRootSelected = currentFolder != null,
        showBackButton = folderStackUris.size > 1,
        currentFolderName = currentFolder?.name ?: "Library",
        gridItems = gridItems,
        hasBrowsableContent = hasBrowsableContent,
        errorMessage = errorMessage
    )
    val actions = LibraryOverviewActions(
        onSelectFolderClick = { folderPicker.launch(null) },
        onBackClick = {
            if (folderStackUris.isNotEmpty()) {
                folderStackUris.removeAt(folderStackUris.lastIndex)
            }
        },
        onItemClick = onItemClick@{ clickedItem ->
            if (clickedItem.kind != LibraryItemKind.Folder) {
                return@onItemClick
            }
            toDocumentFolder(context, clickedItem.targetUri)?.let {
                folderStackUris.add(clickedItem.targetUri.toString())
            }
        },
        onPlayClick = onPlayClick@{ clickedItem ->
            if (clickedItem.kind != LibraryItemKind.Audio) {
                return@onPlayClick
            }
            val audioItems = gridItems.filter { it.kind == LibraryItemKind.Audio }
            val selectedIndex = audioItems.indexOfFirst { it.targetUri == clickedItem.targetUri }
            if (selectedIndex >= 0) {
                onOpenPlayer(audioItems, selectedIndex)
            }
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
            onBackClick = actions.onBackClick
        )
        LibraryHeader(title = state.currentFolderName)

        if (state.gridItems.isEmpty()) {
            EmptyFolderState(hasBrowsableContent = state.hasBrowsableContent)
        } else {
            FolderGrid(
                items = state.gridItems,
                onItemClick = actions.onItemClick,
                onPlayClick = actions.onPlayClick
            )
        }
    }
}
