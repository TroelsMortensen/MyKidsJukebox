package pastimegames.mykidsjukebox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import pastimegames.mykidsjukebox.data.library.LibraryItemKind
import pastimegames.mykidsjukebox.features.playerview.PlayerRoute
import pastimegames.mykidsjukebox.features.playerview.PlayerQueueItem
import pastimegames.mykidsjukebox.features.playerview.PlayerScreen
import pastimegames.mykidsjukebox.features.libraryoverview.LibraryOverviewScreen
import pastimegames.mykidsjukebox.ui.theme.MyKidsJukeboxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyKidsJukeboxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var route by remember { mutableStateOf<PlayerRoute>(PlayerRoute.Library) }
                    var playerSessionId by remember { mutableStateOf(0L) }
                    val libraryFolderStackUris = remember { mutableStateListOf<String>() }

                    when (val currentRoute = route) {
                        PlayerRoute.Library -> {
                            LibraryOverviewScreen(
                                folderStackUris = libraryFolderStackUris,
                                onOpenPlayer = { audioItems, startIndex ->
                                    playerSessionId += 1
                                    route = PlayerRoute.Player(
                                        folderAudioItems = audioItems
                                            .filter { it.kind == LibraryItemKind.Audio }
                                            .map { item ->
                                                PlayerQueueItem(
                                                    title = item.name,
                                                    audioUri = item.targetUri,
                                                    artworkUri = item.artworkUri
                                                )
                                            },
                                        startIndex = startIndex,
                                        initialWindowSize = 5,
                                        sessionId = playerSessionId
                                    )
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        is PlayerRoute.Player -> {
                            PlayerScreen(
                                route = currentRoute,
                                onBack = { route = PlayerRoute.Library },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}