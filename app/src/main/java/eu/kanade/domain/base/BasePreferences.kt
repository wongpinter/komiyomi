package eu.kanade.domain.base

import eu.kanade.tachiyomi.util.system.GLUtil
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class BasePreferences(private val preferenceStore: PreferenceStore) {

    fun incognitoMode() = preferenceStore.getBoolean(Preference.appStateKey("incognito_mode"), false)

    fun shownOnboardingFlow() = preferenceStore.getBoolean(Preference.appStateKey("onboarding_complete"), false)

    fun displayProfile() = preferenceStore.getString("pref_display_profile_key", "")

    fun hardwareBitmapThreshold() = preferenceStore.getInt("pref_hardware_bitmap_threshold", GLUtil.SAFE_TEXTURE_LIMIT)

    fun alwaysDecodeLongStripWithSSIV() = preferenceStore.getBoolean("pref_always_decode_long_strip_with_ssiv", false)

    fun installationId() = preferenceStore.getString(Preference.appStateKey("installation_id"), "")
}
