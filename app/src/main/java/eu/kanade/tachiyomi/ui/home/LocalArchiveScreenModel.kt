// KMK -->
package eu.kanade.tachiyomi.ui.home

import android.app.Application
import android.content.Context
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.chapter.interactor.SyncChaptersWithSource
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import exh.log.xLogE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.SetMangaDefaultChapterFlags
import tachiyomi.domain.chapter.model.ArchiveReadingProgress
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalArchiveCatalog
import tachiyomi.source.local.io.LocalArchiveEntry
import tachiyomi.source.local.io.LocalArchiveScan
import tachiyomi.source.local.io.LocalSourceFileSystem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class LocalArchiveScreenModel(
    private val recursive: Boolean,
    private val path: String,
    fileSystem: LocalSourceFileSystem = Injekt.get(),
    private val coverManager: LocalCoverManager = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val getChapters: GetChaptersByMangaId = Injekt.get(),
    private val syncChapters: SyncChaptersWithSource = Injekt.get(),
    private val setChapterFlags: SetMangaDefaultChapterFlags = Injekt.get(),
    context: Context = Injekt.get<Application>(),
    storagePreferences: StoragePreferences = Injekt.get(),
) : StateScreenModel<LocalArchiveScreenModel.State>(State()) {
    private val catalog = LocalArchiveCatalog(fileSystem)
    private val catalogCache = LocalArchiveCatalogCache(
        context = context,
        root = storagePreferences.localSourceDirectory().get().ifBlank {
            storagePreferences.baseStorageDirectory().get().trimEnd('/') + "/local"
        },
        path = path,
        recursive = recursive,
    )
    private val coverSlots = Semaphore(2)
    private val coverLocks = ConcurrentHashMap<String, Mutex>()
    private var scanJob: Job? = null
    private var progressJob: Job? = null

    init {
        loadCatalog(useCache = true)
    }

    fun startObservingProgress() {
        if (progressJob != null) return
        progressJob = screenModelScope.launch {
            Injekt.get<ChapterRepository>().getArchiveReadingProgress(LocalSource.ID)
                .catch { xLogE("Unable to load local reading progress", it) }
                .collect { records ->
                    val progress = withIOContext {
                        records.groupBy { it.path }.mapValues { (_, matches) ->
                            matches.maxWith(compareBy<ArchiveReadingProgress> { it.lastRead }.thenBy { it.lastPage }.thenBy { it.chapterId })
                        }
                    }
                    mutableState.update { it.copy(progress = progress) }
                }
        }
    }

    fun stopObservingProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    fun refresh() {
        loadCatalog(useCache = false)
    }

    private fun loadCatalog(useCache: Boolean) {
        scanJob?.cancel()
        scanJob = screenModelScope.launch {
            val previous = state.value
            mutableState.update { it.copy(scanning = true, scanFailed = false, unreadableFolders = 0, foldersScanned = 0, revision = it.revision + 1) }
            try {
                if (useCache) {
                    val saved = try {
                        catalogCache.load()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        xLogE("Unable to load the saved local catalog", e)
                        null
                    }
                    if (saved != null) {
                        showScan(saved)
                        return@launch
                    }
                }
                catalog.scan(path, recursive).collect { scan ->
                    if (scan.complete && scan.foldersScanned == 0 && scan.unreadableFolders > 0) {
                        throw IOException("Local folder could not be listed")
                    }
                    if (scan.complete) {
                        try {
                            catalogCache.save(scan)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            xLogE("Unable to save the local catalog", e)
                        }
                    }
                    showScan(scan)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                xLogE("Unable to scan the local comic folder", e)
                mutableState.update {
                    it.copy(
                        scanning = false,
                        scanFailed = true,
                        entries = previous.entries,
                        foldersScanned = previous.foldersScanned,
                        unreadableFolders = previous.unreadableFolders,
                    )
                }
            }
        }
    }

    private suspend fun showScan(scan: LocalArchiveScan) {
        val sorted = withIOContext {
            scan.entries.sortedWith(
                Comparator<LocalArchiveEntry> { a, b ->
                    when {
                        a.isDirectory != b.isDirectory -> if (a.isDirectory) -1 else 1
                        else -> a.name.compareToCaseInsensitiveNaturalOrder(b.name)
                            .takeIf { it != 0 } ?: a.path.compareToCaseInsensitiveNaturalOrder(b.path)
                    }
                },
            )
        }
        mutableState.update {
            it.copy(
                entries = sorted,
                scanning = !scan.complete,
                foldersScanned = scan.foldersScanned,
                unreadableFolders = scan.unreadableFolders,
            )
        }
    }

    suspend fun cover(entry: LocalArchiveEntry): String? = coverSlots.withPermit {
        coverLocks.computeIfAbsent(entry.identity) { Mutex() }.withLock {
            try {
                coverManager.getArchiveCover(entry.file, entry.size, entry.modified)?.uri?.toString()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                xLogE("Unable to extract cover for ${entry.path}", e)
                null
            }
        }
    }

    fun open(entry: LocalArchiveEntry, onOpen: (Long, Long) -> Unit) {
        if (state.value.opening != null) return
        mutableState.update { it.copy(opening = entry.path, openFailed = false) }
        screenModelScope.launch {
            try {
                val target = withIOContext {
                    state.value.progress[entry.path]?.let { return@withIOContext it.mangaId to it.chapterId }
                    // Reuse older series records too, so moving to the flat library keeps reading progress.
                    var candidate = entry.path
                    while (candidate.isNotEmpty()) {
                        val existing = getManga.await(candidate, LocalSource.ID)
                        if (existing != null) {
                            val chapter = getChapters.await(existing.id).firstOrNull { it.url == entry.path }
                            if (chapter != null) return@withIOContext existing.id to chapter.id
                        }
                        candidate = candidate.substringBeforeLast('/', "")
                    }
                    val source = Injekt.get<SourceManager>().getOrStub(LocalSource.ID)
                    // KMK -->
                    val rawManga = SManga.create().apply {
                        url = entry.path
                        title = entry.title
                        initialized = true
                    }
                    val detailedManga = source.getMangaUpdate(
                        manga = rawManga,
                        chapters = emptyList(),
                        fetchDetails = true,
                        fetchChapters = false,
                    ).manga
                    val manga = networkToLocalManga(detailedManga.toDomainManga(LocalSource.ID))
                    // KMK <--
                    val chapter = SChapter.create().apply {
                        url = entry.path
                        name = entry.title
                        chapter_number = 1f
                        date_upload = entry.modified
                    }
                    setChapterFlags.await(manga)
                    // Register one archive without rescanning all its images or extracting metadata on this path.
                    syncChapters.await(rawSourceChapters = listOf(chapter), manga = manga, source = source)
                    val savedChapter = getChapters.await(manga.id).first { it.url == entry.path }
                    manga.id to savedChapter.id
                }
                onOpen(target.first, target.second)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                xLogE("Unable to open ${entry.path}", e)
                mutableState.update { it.copy(openFailed = true) }
            } finally {
                mutableState.update { it.copy(opening = null) }
            }
        }
    }

    data class State(
        val entries: List<LocalArchiveEntry> = emptyList(),
        val scanning: Boolean = true,
        val foldersScanned: Int = 0,
        val unreadableFolders: Int = 0,
        val scanFailed: Boolean = false,
        val opening: String? = null,
        val openFailed: Boolean = false,
        val revision: Int = 0,
        val progress: Map<String, ArchiveReadingProgress> = emptyMap(),
    )
}
// KMK <--
