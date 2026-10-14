package eu.kanade.presentation.more

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.openInBrowser
import tachiyomi.core.common.Constants
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MoreScreen(
    // KMK -->
    // Only offline manager actions are exposed by this screen.
    // KMK <--
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    // SY -->
    // KMK -->
    // Updates and history are not navigation destinations in this app.
    // KMK <--
    // SY <--
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    // SY -->
    // KMK -->
    // Local manager actions do not depend on online source preferences.
    // KMK <--
    // SY <--

    Scaffold { contentPadding ->
        ScrollbarLazyColumn(
            // KMK: use contentPadding as preferable padding for ScrollbarLazyColumn when not using stickyHeader
            contentPadding = contentPadding,
        ) {
            item {
                LogoHeader()
            }
            // KMK -->
            // Every item in this app is already a local archive.
            // KMK <--
            item {
                SwitchPreferenceWidget(
                    title = stringResource(MR.strings.pref_incognito_mode),
                    subtitle = stringResource(MR.strings.pref_incognito_mode_summary),
                    // KMK -->
                    icon = rememberAnimatedVectorPainter(
                        AnimatedImageVector.animatedVectorResource(R.drawable.anim_incognito),
                        incognitoMode,
                    ),
                    // KMK <--
                    checked = incognitoMode,
                    onCheckedChanged = onIncognitoModeChange,
                )
            }

            item { HorizontalDivider() }

            // SY -->
            // KMK -->
            // Offline navigation has no updates or history links.
            // KMK <--
            // SY <--

            // KMK -->
            // Download queues are not used by the offline reader.
            // KMK <--
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.categories),
                    icon = Icons.AutoMirrored.Outlined.Label,
                    onPreferenceClick = onClickCategories,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_stats),
                    icon = Icons.Outlined.QueryStats,
                    onPreferenceClick = onClickStats,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_data_storage),
                    icon = Icons.Outlined.Storage,
                    onPreferenceClick = onClickDataAndStorage,
                )
            }
            // SY -->
            // KMK -->
            // Online batch imports are not used by the local manager.
            // KMK <--
            // SY <--

            item { HorizontalDivider() }

            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_settings),
                    icon = Icons.Outlined.Settings,
                    onPreferenceClick = onClickSettings,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.pref_category_about),
                    icon = Icons.Outlined.Info,
                    onPreferenceClick = onClickAbout,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_help),
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    onPreferenceClick = { uriHandler.openUri(Constants.URL_HELP) },
                )
            }
            // KMK -->
            // Komiyomi credits and licenses are available in About.
            // KMK <--
        }
    }
}

// KMK -->
@Composable
fun Sponsor() {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MaterialTheme.padding.medium),
        horizontalArrangement = Arrangement.Center,
    ) {
        TextButton(
            onClick = { context.openInBrowser(Constants.SPONSOR) },
            modifier = Modifier
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small,
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(KMR.strings.sponsor_me),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(KMR.strings.sponsor_me),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SponsorPreview() {
    TachiyomiPreviewTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp,
        ) {
            Sponsor()
        }
    }
}
// KMK <--
