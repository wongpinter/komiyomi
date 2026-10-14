package exh.debug

import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.injectLazy
import java.util.Locale

enum class DebugToggles(val default: Boolean) {
    // KMK -->
    HIDE_COVER_IMAGE_ONLY_SHOW_COLOR(false),
    // KMK <--
    ;

    private val prefKey = "eh_debug_toggle_${name.lowercase(Locale.US)}"

    var enabled: Boolean
        get() = preferenceStore.getBoolean(prefKey, default).get()
        set(value) {
            preferenceStore.getBoolean(prefKey).set(value)
        }

    companion object {
        private val preferenceStore: PreferenceStore by injectLazy()
    }
}
