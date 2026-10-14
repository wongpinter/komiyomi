package eu.kanade.tachiyomi.ui.more

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.core.preference.asState
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.MoreScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object MoreTab : Tab {
    @Suppress("unused")
    private fun readResolve(): Any = MoreTab

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_more_enter)
            return TabOptions(
                // KMK -->
                index = 2u,
                // KMK <--
                title = stringResource(MR.strings.label_more),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(SettingsScreen())
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MoreScreenModel() }
        // KMK -->
        // The local manager has no download queue to observe.
        // KMK <--
        MoreScreen(
            incognitoMode = screenModel.incognitoMode,
            onIncognitoModeChange = { screenModel.incognitoMode = it },
            // SY -->
            // KMK -->
            // Offline navigation has no updates link.
            // KMK <--
            // KMK -->
            // Offline navigation has no history link.
            // KMK <--
            // SY <--
            onClickCategories = { navigator.push(CategoryScreen()) },
            onClickStats = { navigator.push(StatsScreen()) },
            onClickDataAndStorage = { navigator.push(SettingsScreen(SettingsScreen.Destination.DataAndStorage)) },
            onClickSettings = { navigator.push(SettingsScreen()) },
            onClickAbout = { navigator.push(SettingsScreen(SettingsScreen.Destination.About)) },
            // SY -->
            // KMK -->
            // Online imports and source navigation are omitted.
            // KMK <--
            // SY <--
        )

        // AM (DISCORD) -->
        // KMK -->
        // The offline manager does not publish a Discord presence.
        // KMK <--
        // <-- AM (DISCORD)
    }
}

// KMK -->
private class MoreScreenModel(
    preferences: BasePreferences = Injekt.get(),
    // SY -->
    // Offline navigation does not use source tab preferences.
    // SY <--
) : ScreenModel {

    var incognitoMode by preferences.incognitoMode().asState(screenModelScope)

    // SY -->
    // Offline navigation does not use source tab preferences.
    // SY <--
}
// KMK <--
