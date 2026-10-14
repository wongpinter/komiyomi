package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.text.format.Formatter
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.BufferedOutputStream
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Class used to create chapter cache
 * For each image in a chapter a file is created
 * For each chapter a Json list is created and converted to a file.
 * The files are in format *md5key*.0
 *
 * @param context the application context.
 */
class ChapterCache(
    private val context: Context,
    // SY -->
    readerPreferences: ReaderPreferences,
    // SY <--
) {

    // --> EH
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    /** Cache class used for cache management.  */
    private var diskCache = setupDiskCache(readerPreferences.cacheSize().get().toLong())

    // KMK -->
    /** Local archive pages have their own bounded LRU so reading offline comics cannot evict web pages. */
    private val archivePageCache = setupDiskCache(LOCAL_ARCHIVE_CACHE_SIZE_MB, "local_archive_page_cache")
    // KMK <--

    init {
        readerPreferences.cacheSize().changes()
            .drop(1)
            .onEach {
                // Save old cache for destruction later
                val oldCache = diskCache
                diskCache = setupDiskCache(it.toLong())
                oldCache.close()
            }
            .launchIn(scope)
    }
    // <-- EH

    /**
     * Returns directory of cache.
     */
    private val cacheDir: File = diskCache.directory

    /**
     * Returns real size of directory.
     */
    private val realSize: Long
        get() = DiskUtil.getDirectorySize(cacheDir) +
            // KMK -->
            DiskUtil.getDirectorySize(archivePageCache.directory) +
            DiskUtil.getDirectorySize(File(context.cacheDir, "zip-directory-index")) +
            DiskUtil.getDirectorySize(File(context.cacheDir, "archive-entry-temp"))
    // KMK <--

    /**
     * Returns real size of directory in human readable format.
     */
    val readableSize: String
        get() = Formatter.formatFileSize(context, realSize)

    // --> EH
    // Cache size is in MB
    // KMK -->
    private fun setupDiskCache(cacheSize: Long): DiskLruCache = setupDiskCache(cacheSize, "chapter_disk_cache")
    // KMK <--

    // KMK -->
    private fun setupDiskCache(cacheSize: Long, directoryName: String): DiskLruCache {
        return DiskLruCache.open(
            File(context.cacheDir, directoryName),
            PARAMETER_APP_VERSION,
            PARAMETER_VALUE_COUNT,
            cacheSize * 1024 * 1024,
        )
    }
    // KMK <--
    // <-- EH

    // KMK -->
    fun getOrPutImageStream(imageUrl: String, source: () -> InputStream): InputStream {
        return getOrPutImageStream(diskCache, imageUrl, source)
    }

    fun isArchiveImageInCache(imageUrl: String): Boolean = runCatching {
        archivePageCache.get(DiskUtil.hashKeyForDisk(imageUrl)).use { it != null }
    }.getOrDefault(false)

    fun getOrPutArchiveImageStream(imageUrl: String, source: () -> InputStream): InputStream =
        getOrPutImageStream(archivePageCache, imageUrl, source)

    private fun getOrPutImageStream(cache: DiskLruCache, imageUrl: String, source: () -> InputStream): InputStream {
        val key = DiskUtil.hashKeyForDisk(imageUrl)
        val cachedStream = runCatching {
            cache.get(key)?.use {
                File(cache.directory, "$key.0").inputStream()
            }
        }.getOrNull()
        if (cachedStream != null) return cachedStream

        val editor = try {
            cache.edit(key)
        } catch (_: Exception) {
            null
        } ?: return source()

        val output = try {
            BufferedOutputStream(editor.newOutputStream(0))
        } catch (_: Exception) {
            runCatching { editor.abortUnlessCommitted() }
            return source()
        }

        val input = try {
            source()
        } catch (e: Exception) {
            runCatching { output.close() }
            runCatching { editor.abortUnlessCommitted() }
            throw e
        }

        return CacheWritingInputStream(
            input = input,
            output = output,
            onComplete = {
                try {
                    cache.flush()
                    editor.commit()
                } catch (_: Exception) {
                    runCatching { editor.abortUnlessCommitted() }
                }
            },
            onAbort = { runCatching { editor.abortUnlessCommitted() } },
        )
    }
    // KMK <--

    fun clear(): Int {
        var deletedFiles = 0
        cacheDir.listFiles()?.forEach {
            if (removeFileFromCache(it.name)) {
                deletedFiles++
            }
        }
        // KMK -->
        deletedFiles += clearCacheDirectory(archivePageCache)
        val zipIndexDirectory = File(context.cacheDir, "zip-directory-index")
        deletedFiles += zipIndexDirectory.listFiles().orEmpty().count { it.delete() }
        val archiveEntryTempDirectory = File(context.cacheDir, "archive-entry-temp")
        deletedFiles += archiveEntryTempDirectory.listFiles().orEmpty().count { it.delete() }
        // KMK <--
        return deletedFiles
    }

    // KMK -->
    private fun clearCacheDirectory(cache: DiskLruCache): Int {
        var deletedFiles = 0
        cache.directory.listFiles()?.forEach { file ->
            if (file.name != "journal" && !file.name.startsWith("journal.")) {
                runCatching { cache.remove(file.name.substringBeforeLast('.')) }
                    .onSuccess { if (it) deletedFiles++ }
            }
        }
        return deletedFiles
    }
    // KMK <--

    /**
     * Remove file from cache.
     *
     * @param file name of file "md5.0".
     * @return status of deletion for the file.
     */
    private fun removeFileFromCache(file: String): Boolean {
        // Make sure we don't delete the journal file (keeps track of cache)
        if (file == "journal" || file.startsWith("journal.")) {
            return false
        }

        return try {
            // Remove the extension from the file to get the key of the cache
            val key = file.substringBeforeLast(".")
            // Remove file from cache
            diskCache.remove(key)
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Failed to remove file from cache" }
            false
        }
    }
}

// KMK -->
private class CacheWritingInputStream(
    input: InputStream,
    private val output: OutputStream,
    private val onComplete: () -> Unit,
    private val onAbort: () -> Unit,
) : FilterInputStream(input) {
    private var isCaching = true

    override fun read(): Int {
        return try {
            super.read().also { value ->
                if (value == -1) {
                    finishCaching()
                } else {
                    writeToCache(value)
                }
            }
        } catch (e: IOException) {
            abortCaching()
            throw e
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return try {
            super.read(buffer, offset, length).also { count ->
                when {
                    count == -1 -> finishCaching()
                    count > 0 -> writeToCache(buffer, offset, count)
                }
            }
        } catch (e: IOException) {
            abortCaching()
            throw e
        }
    }

    override fun skip(byteCount: Long): Long {
        if (byteCount <= 0) return 0
        val buffer = ByteArray(8192)
        var skipped = 0L
        while (skipped < byteCount) {
            val count = read(buffer, 0, minOf(buffer.size.toLong(), byteCount - skipped).toInt())
            if (count < 0) break
            skipped += count
        }
        return skipped
    }

    override fun mark(readlimit: Int) {
        abortCaching()
        super.mark(readlimit)
    }

    override fun reset() {
        abortCaching()
        super.reset()
    }

    override fun close() {
        try {
            super.close()
        } finally {
            abortCaching()
        }
    }

    private fun writeToCache(value: Int) {
        if (!isCaching) return
        try {
            output.write(value)
        } catch (_: Exception) {
            abortCaching()
        }
    }

    private fun writeToCache(buffer: ByteArray, offset: Int, length: Int) {
        if (!isCaching) return
        try {
            output.write(buffer, offset, length)
        } catch (_: Exception) {
            abortCaching()
        }
    }

    private fun finishCaching() {
        if (!isCaching) return
        isCaching = false
        try {
            output.close()
            onComplete()
        } catch (_: Exception) {
            onAbort()
        }
    }

    private fun abortCaching() {
        if (!isCaching) return
        isCaching = false
        runCatching { output.close() }
        onAbort()
    }
}
// KMK <--

/** Application cache version.  */
private const val PARAMETER_APP_VERSION = 1

/** The number of values per cache entry. Must be positive.  */
private const val PARAMETER_VALUE_COUNT = 1

/** The maximum number of bytes this cache should use to store.  */
private const val PARAMETER_CACHE_SIZE = 100L * 1024 * 1024

// KMK -->
private const val LOCAL_ARCHIVE_CACHE_SIZE_MB = 256L
// KMK <--
