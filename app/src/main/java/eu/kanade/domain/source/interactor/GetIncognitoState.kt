package eu.kanade.domain.source.interactor

import eu.kanade.domain.base.BasePreferences
import kotlinx.coroutines.flow.Flow

class GetIncognitoState(
    private val basePreferences: BasePreferences,
) {
    fun await(sourceId: Long?): Boolean = basePreferences.incognitoMode().get()

    fun subscribe(sourceId: Long?): Flow<Boolean> = basePreferences.incognitoMode().changes()
}
