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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.collect
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
import pastimegames.mykidsjukebox.features.shared.components.rememberUiFeedback
import pastimegames.mykidsjukebox.R
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
    val uiFeedback = rememberUiFeedback()
    val rootUriString by folderStore.rootUriFlow.collectAsState(initial = null)
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
            uiFeedback.performSuccessFeedback()
        } catch (_: SecurityException) {
            errorMessage = context.getString(R.string.select_folder_permission_error)
            uiFeedback.performErrorFeedback()
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
    var quickScanResult by remember(currentFolder?.uri?.toString()) { mutableStateOf<LibraryScanner.QuickScanResult?>(null) }
    var hasBrowsableContent by remember(currentFolder?.uri?.toString()) { mutableStateOf(false) }
    var isScanInProgress by remember(currentFolder?.uri?.toString()) { mutableStateOf(false) }
    var playableAudioItems by remember(currentFolder?.uri?.toString()) { mutableStateOf(emptyList<FolderGridItem>()) }
    LaunchedEffect(currentFolder?.uri?.toString()) {
        val folder = currentFolder
        if (folder == null) {
            gridItems = emptyList()
            quickScanResult = null
            hasBrowsableContent = false
            isScanInProgress = false
            playableAudioItems = emptyList()
            return@LaunchedEffect
        }

        gridItems = emptyList()
        quickScanResult = null
        hasBrowsableContent = false
        isScanInProgress = true
        playableAudioItems = emptyList()
        scanner.scanFolderItemsIncremental(context, folder).collect { event ->
            when (event) {
                is LibraryScanner.ScanEvent.Batch -> {
                    gridItems = appendUniqueItems(gridItems, event.items)
                    quickScanResult = event.quickScanResult
                }

                is LibraryScanner.ScanEvent.ItemsUpdated -> {
                    gridItems = applyItemUpdates(gridItems, event.items)
                    quickScanResult = event.quickScanResult
                }

                is LibraryScanner.ScanEvent.Complete -> {
                    quickScanResult = event.quickScanResult
                    hasBrowsableContent = event.hasBrowsableContent
                    playableAudioItems = event.sortedAudioItems
                    isScanInProgress = false
                }
            }
        }
    }

    val uiState = LibraryOverviewState(
        isRootSelected = currentFolder != null,
        showBackButton = folderStackUris.size > 1,
        currentFolderName = currentFolder?.name ?: stringResource(R.string.library_title),
        gridItems = gridItems,
        hasBrowsableContent = hasBrowsableContent,
        isScanInProgress = isScanInProgress,
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
            if (isScanInProgress) {
                return@onPlayClick
            }
            if (clickedItem.kind != LibraryItemKind.Audio) {
                return@onPlayClick
            }
            val selectedIndex = playableAudioItems.indexOfFirst { it.targetUri == clickedItem.targetUri }
            if (selectedIndex >= 0) {
                onOpenPlayer(playableAudioItems, selectedIndex)
            }
        }
    )

    LibraryOverviewContent(
        state = uiState,
        actions = actions,
        context = context,
        scanner = scanner,
        quickScanResult = quickScanResult,
        modifier = modifier
    )
}

@Composable
private fun LibraryOverviewContent(
    state: LibraryOverviewState,
    actions: LibraryOverviewActions,
    context: android.content.Context,
    scanner: LibraryScanner,
    quickScanResult: LibraryScanner.QuickScanResult?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!state.isRootSelected) {
            SelectFolderState(
                onSelectFolderClick = actions.onSelectFolderClick,
                errorMessage = state.errorMessage,
                message = stringResource(R.string.select_folder_message),
                buttonLabel = stringResource(R.string.select_folder_button)
            )
            return@Column
        }

        NavigationButtons(
            showBackButton = state.showBackButton,
            onBackClick = actions.onBackClick
        )
        LibraryHeader(title = state.currentFolderName)

        if (state.gridItems.isEmpty()) {
            if (state.isScanInProgress) {
                EmptyFolderState(
                    message = stringResource(R.string.library_scanning_message)
                )
                return@Column
            }
            EmptyFolderState(
                message = if (state.hasBrowsableContent) {
                    stringResource(R.string.empty_folder_has_content_elsewhere)
                } else {
                    stringResource(R.string.empty_folder_message)
                }
            )
        } else {
            FolderGrid(
                items = state.gridItems,
                context = context,
                scanner = scanner,
                quickScanResult = quickScanResult,
                playEnabled = !state.isScanInProgress,
                onItemClick = actions.onItemClick,
                onPlayClick = actions.onPlayClick
            )
        }
    }
}

private fun appendUniqueItems(
    existingItems: List<FolderGridItem>,
    incomingItems: List<FolderGridItem>
): List<FolderGridItem> {
    if (incomingItems.isEmpty()) {
        return existingItems
    }
    val existingUris = existingItems.map { it.targetUri }.toHashSet()
    val newItems = incomingItems.filter { !existingUris.contains(it.targetUri) }
    return if (newItems.isEmpty()) existingItems else existingItems + newItems
}

private fun applyItemUpdates(
    existingItems: List<FolderGridItem>,
    updatedItems: List<FolderGridItem>
): List<FolderGridItem> {
    if (updatedItems.isEmpty()) {
        return existingItems
    }
    val updatedByUri = updatedItems.associateBy { it.targetUri }
    return existingItems.map { item -> updatedByUri[item.targetUri] ?: item }
}
