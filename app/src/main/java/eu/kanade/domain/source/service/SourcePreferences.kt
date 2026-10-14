package eu.kanade.domain.source.service

import tachiyomi.core.common.preference.PreferenceStore

class SourcePreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun pinnedSources() = preferenceStore.getStringSet(
        // KMK -->
        PINNED_SOURCES_PREF_KEY,
        // KMK <--
        emptySet(),
    )

    // KMK -->
    fun allowLocalSourceHiddenFolders() = preferenceStore.getBoolean("allow_local_source_hidden_folders", false)
    // KMK <--

    companion object {
        const val PINNED_SOURCES_PREF_KEY = "pinned_catalogues"
    }
}
