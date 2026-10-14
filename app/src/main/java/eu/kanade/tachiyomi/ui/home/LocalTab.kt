// KMK -->
package eu.kanade.tachiyomi.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.main.MainActivity
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object LocalLibraryTab : Tab {

    @Suppress("unused")
    private fun readResolve(): Any = LocalLibraryTab

    override val options: TabOptions
        @Composable
        get() {
            return TabOptions(
                index = 0u,
                title = stringResource(MR.strings.label_library),
                icon = rememberVectorPainter(Icons.Outlined.FolderOpen),
            )
        }

    @Composable
    override fun Content() {
        LocalArchiveContent(
            keyPrefix = "LocalLibrary",
            recursive = true,
        )
    }
}

// KMK -->
data object OfflineBrowseTab : Tab {

    @Suppress("unused")
    private fun readResolve(): Any = OfflineBrowseTab

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 1u,
            title = stringResource(MR.strings.browse),
            icon = rememberVectorPainter(Icons.Outlined.Folder),
        )

    @Composable
    override fun Content() {
        LocalArchiveContent(keyPrefix = "OfflineBrowse", recursive = false)
    }
}

@Composable
private fun LocalArchiveContent(
    keyPrefix: String,
    recursive: Boolean,
) {
    val context = LocalContext.current
    val storagePreferences = remember { Injekt.get<StoragePreferences>() }
    val baseStorageDirectory by storagePreferences.baseStorageDirectory().collectAsState()
    val localSourceDirectory by storagePreferences.localSourceDirectory().collectAsState()
    val navigatorKey = remember(keyPrefix, baseStorageDirectory, localSourceDirectory) {
        "$keyPrefix-${baseStorageDirectory.hashCode()}-${localSourceDirectory.hashCode()}"
    }

    Navigator(
        screen = LocalArchiveScreen(recursive),
        key = navigatorKey,
    ) {
        CurrentScreen()
    }

    LaunchedEffect(Unit) {
        (context as? MainActivity)?.ready = true
    }
}
// KMK <--
// KMK <--
