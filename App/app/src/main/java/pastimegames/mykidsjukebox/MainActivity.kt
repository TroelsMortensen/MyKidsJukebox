package pastimegames.mykidsjukebox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import pastimegames.mykidsjukebox.ui.theme.MyKidsJukeboxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyKidsJukeboxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LibraryBrowserScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun LibraryBrowserScreen(modifier: Modifier = Modifier) {
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
    val folderItems by produceState(initialValue = emptyList<FolderGridItem>(), currentFolder) {
        value = currentFolder?.let { scanner.listFolderItems(it) } ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (currentFolder == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select your library folder to begin.",
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { folderPicker.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        Text("Select Library Folder")
                    }
                    errorMessage?.let { Text(text = it, textAlign = TextAlign.Center) }
                }
            }
            return@Column
        }

        if (folderStack.size > 1) {
            Button(
                onClick = { folderStack.removeAt(folderStack.lastIndex) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                Text(
                    text = "Back",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Button(
            onClick = {
                val startUri = rootUriString?.let(Uri::parse)
                folderPicker.launch(startUri)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Text("Change Library Folder")
        }

        Text(
            text = currentFolder.name ?: "Library",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        if (folderItems.isEmpty()) {
            val hasPlayableContent = scanner.hasAnyBrowsableContent(currentFolder)
            Text(
                text = if (hasPlayableContent) {
                    "No subfolders here. Go back or choose another folder."
                } else {
                    "This folder is empty."
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            FolderGrid(
                items = folderItems,
                onFolderClick = { clickedFolder ->
                    toDocumentFolder(context, clickedFolder.folderUri)?.let { folderStack.add(it) }
                }
            )
        }
    }
}

@Composable
private fun FolderGrid(
    items: List<FolderGridItem>,
    onFolderClick: (FolderGridItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { onFolderClick(item) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FolderArtwork(artworkUri = item.artworkUri)
                    Text(
                        text = item.name,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Text(
                        text = "${item.childFolderCount} folders | ${item.audioFileCount} audio",
                        textAlign = TextAlign.Center,
                        color = Color(0xFFBFDBFE)
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderArtwork(artworkUri: Uri?) {
    if (artworkUri != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUri)
                .crossfade(true)
                .build(),
            contentDescription = "Folder artwork",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(color = Color(0xFFFBBF24), shape = RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = "Folder",
            tint = Color(0xFF1D4ED8),
            modifier = Modifier.size(56.dp)
        )
    }
}

private fun toDocumentFolder(context: android.content.Context, uri: Uri): DocumentFile? {
    return DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)
}