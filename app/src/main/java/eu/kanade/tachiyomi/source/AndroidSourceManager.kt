@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.source

import android.content.Context
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class AndroidSourceManager(
    private val context: Context,
    private val sourceRepository: StubSourceRepository,
) : SourceManager {

    // KMK -->
    private val localSource: LocalSource by lazy {
        val sourcePreferences: SourcePreferences = Injekt.get()
        LocalSource(
            context,
            Injekt.get(),
            Injekt.get(),
            sourcePreferences.allowLocalSourceHiddenFolders()::get,
        )
    }

    private val _isInitialized = MutableStateFlow(true)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    override val sources: Flow<List<Source>> = flowOf(listOf(localSource))
    private val stubSources = ConcurrentHashMap<Long, StubSource>()
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    init {
        scope.launch {
            sourceRepository.subscribeAll().collectLatest { sources ->
                stubSources.clear()
                sources.forEach { stubSources[it.id] = it }
            }
        }
    }

    override fun get(sourceKey: Long): Source? = localSource.takeIf { sourceKey == LocalSource.ID }

    override fun getOrStub(sourceKey: Long): Source = get(sourceKey) ?: stubSources.getOrPut(sourceKey) {
        runBlocking { sourceRepository.getStubSource(sourceKey) } ?: StubSource(sourceKey, "", "")
    }

    override fun getAll(): List<Source> = listOf(localSource)

    override fun getStubSources(): List<StubSource> = stubSources.values.toList()
    // KMK <--
}
