package eu.kanade.presentation.more.settings.screen.about

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.LogoHeader
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.ui.more.ComingUpdatesScreen
import eu.kanade.tachiyomi.ui.more.NewUpdateScreen
import eu.kanade.tachiyomi.ui.more.WhatsNewScreen
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.lang.toDateTimestampString
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.updaterEnabled
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.components.LinkIcon
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.icons.CustomIcons
import tachiyomi.presentation.core.icons.Discord
import tachiyomi.presentation.core.icons.Github
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AboutScreen : Screen() {

    // KMK -->
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val handleBack = LocalBackPress.current
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_category_about),
                    navigateUp = if (handleBack != null) handleBack::invoke else null,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            ScrollbarLazyColumn(contentPadding = contentPadding) {
                item { LogoHeader() }
                item {
                    TextPreferenceWidget(
                        title = stringResource(KMR.strings.komiyomi_app_name),
                        subtitle = stringResource(KMR.strings.komiyomi_description),
                    )
                }
                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.version),
                        subtitle = getVersionName(withBuildDate = true),
                        onPreferenceClick = {
                            context.copyToClipboard("Debug information", CrashLogUtil(context).getDebugInfo())
                        },
                    )
                }
                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.licenses),
                        subtitle = stringResource(KMR.strings.komiyomi_credits),
                        onPreferenceClick = { navigator.push(OpenSourceLicensesScreen()) },
                    )
                }
            }
        }
    }
    // KMK <--

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    private suspend fun checkVersion(
        context: Context,
        onAvailableUpdate: (GetApplicationRelease.Result.NewUpdate) -> Unit,
        onFinish: () -> Unit,
        // KMK -->
        peekIntoPreview: Boolean = false,
        // KMK <--
    ) {
        val updateChecker = AppUpdateChecker(
            // KMK -->
            peekIntoPreview = peekIntoPreview,
            // KMK <--
        )
        withUIContext {
            try {
                when (val result = withIOContext { updateChecker.checkForUpdate(context, forceCheck = true) }) {
                    is GetApplicationRelease.Result.NewUpdate -> {
                        onAvailableUpdate(result)
                    }
                    is GetApplicationRelease.Result.NoNewUpdate -> {
                        context.toast(MR.strings.update_check_no_new_updates)
                    }
                    is GetApplicationRelease.Result.OsTooOld -> {
                        context.toast(MR.strings.update_check_eol)
                    }
                }
            } catch (e: Exception) {
                context.toast(e.message)
                logcat(LogPriority.ERROR, e)
            } finally {
                onFinish()
            }
        }
    }

    companion object {
        // KMK -->
        suspend fun getReleaseNotes(
            context: Context,
            onAvailableUpdate: (GetApplicationRelease.Result.NewUpdate) -> Unit,
            onFinish: () -> Unit,
        ) {
            val updateChecker = AppUpdateChecker()
            withUIContext {
                try {
                    when (val result = withIOContext { updateChecker.getReleaseNotes() }) {
                        is GetApplicationRelease.Result.NewUpdate -> {
                            onAvailableUpdate(result)
                        }

                        else -> {}
                    }
                } catch (e: Exception) {
                    context.toast(e.message)
                    logcat(LogPriority.ERROR, e)
                } finally {
                    onFinish()
                }
            }
        }
        // KMK <--

        fun getVersionName(withBuildDate: Boolean): String {
            return when {
                isDebugBuildType -> {
                    "Debug ${BuildConfig.COMMIT_SHA}".let {
                        if (withBuildDate) {
                            "$it (${getFormattedBuildTime()})"
                        } else {
                            it
                        }
                    }
                }

                isPreviewBuildType -> {
                    "Beta r${BuildConfig.COMMIT_COUNT}".let {
                        if (withBuildDate) {
                            "$it (${BuildConfig.COMMIT_SHA}, ${getFormattedBuildTime()})"
                        } else {
                            "$it (${BuildConfig.COMMIT_SHA})"
                        }
                    }
                }

                else -> {
                    "Stable ${BuildConfig.VERSION_NAME}".let {
                        if (withBuildDate) {
                            "$it (${getFormattedBuildTime()})"
                        } else {
                            it
                        }
                    }
                }
            }
        }

        internal fun getFormattedBuildTime(): String {
            return try {
                LocalDateTime.ofInstant(
                    Instant.parse(BuildConfig.BUILD_TIME),
                    ZoneId.systemDefault(),
                )
                    .toDateTimestampString(
                        UiPreferences.dateFormat(
                            Injekt.get<UiPreferences>().dateFormat().get(),
                        ),
                    )
            } catch (_: Exception) {
                BuildConfig.BUILD_TIME
            }
        }
    }
}
