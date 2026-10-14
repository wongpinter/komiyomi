package eu.kanade.tachiyomi.ui.reader.loader

import android.os.SystemClock
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.core.archive.ArchiveReader
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Loader used to load a chapter from an archive file.
 */
internal class ArchivePageLoader(
    private val reader: ArchiveReader,
    // KMK -->
    private val archiveFile: UniFile,
    private val cacheOfflinePages: Boolean = false,
    // KMK <--
) : PageLoader() {
    // SY -->
    private val mutex = Mutex()
    private val readerPreferences: ReaderPreferences by injectLazy()
    // KMK -->
    private val chapterCache: ChapterCache by injectLazy()
    private val firstPageCacheStatusLogged = AtomicBoolean()
    private val archiveCacheKeyPrefix by lazy {
        "local-archive-v2:${archiveFile.uri}:${reader.size}:${archiveFile.lastModified()}:"
    }
    // KMK <--
    // SY -->
    // KMK -->
    // Page caching is lazy and bounded, so don't extract the entire archive first.
    private val archiveReaderMode = readerPreferences.archiveReaderMode().get().let { mode ->
        if (
            reader.size <= 0 ||
            reader.size > 64L * 1024 * 1024 ||
            mode == ReaderPreferences.ArchiveReaderMode.CACHE_TO_DISK
        ) {
            ReaderPreferences.ArchiveReaderMode.LOAD_FROM_FILE
        } else {
            mode
        }
    }
    // KMK <--
    // SY <--

    init {
        reader.wrongPassword?.let { wrongPassword ->
            if (wrongPassword) {
                error("Incorrect archive password")
            }
        }
    }
    // SY <--

    override var isLocal: Boolean = true

    // SY -->
    // KMK -->
    override suspend fun getPages(): List<ReaderPage> {
        val pageListStartedAt = SystemClock.elapsedRealtime()
        val pages = reader.useEntries { entries ->
            entries
                .filter { it.isFile && ImageUtil.isImage(it.name) { reader.getInputStream(it.name)!! } }
                .sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
                .mapIndexed { i, entry ->
                    // SY -->
                    val imageBytesDeferred: Deferred<ByteArray>? =
                        // KMK -->
                        when (archiveReaderMode) {
                            ReaderPreferences.ArchiveReaderMode.LOAD_INTO_MEMORY -> {
                                CoroutineScope(Dispatchers.IO).async {
                                    mutex.withLock {
                                        reader.getInputStream(entry.name)!!.buffered().use { stream ->
                                            stream.readBytes()
                                        }
                                    }
                                }
                            }

                            else -> null
                        }
                    // KMK <--
                    val imageBytes by lazy { runBlocking { imageBytesDeferred?.await() } }
                    // SY <--
                    ReaderPage(i).apply {
                        // KMK -->
                        archivePath = entry.name
                        stream = {
                            val cacheKey = archiveCacheKeyPrefix + entry.name
                            if (cacheOfflinePages) {
                                if (i == 0 && firstPageCacheStatusLogged.compareAndSet(false, true)) {
                                    logcat { "Local archive first-page cache hit=${chapterCache.isArchiveImageInCache(cacheKey)}" }
                                }
                                chapterCache.getOrPutArchiveImageStream(cacheKey) {
                                    imageBytes?.copyOf()?.inputStream() ?: reader.getInputStream(entry.name)!!
                                }
                            } else {
                                chapterCache.getOrPutImageStream(cacheKey) {
                                    imageBytes?.copyOf()?.inputStream() ?: reader.getInputStream(entry.name)!!
                                }
                            }
                        }
                        // KMK <--
                        status = Page.State.Ready
                    }
                }
                .toList()
        }
        if (cacheOfflinePages) {
            logcat {
                "Local archive page list ready: pages=${pages.size} elapsedMs=${SystemClock.elapsedRealtime() - pageListStartedAt}"
            }
        }
        return pages
    }
    // KMK <--
    // SY <--

    override suspend fun loadPage(page: ReaderPage) {
        check(!isRecycled)
    }

    override fun recycle() {
        super.recycle()
        reader.close()
        // SY -->
        // KMK -->
        // Persistent page caching is managed by ChapterCache.
        // KMK <--
        // SY <--
    }
}
