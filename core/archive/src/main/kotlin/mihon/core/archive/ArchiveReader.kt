package mihon.core.archive

import android.os.ParcelFileDescriptor
import me.zhanghai.android.libarchive.ArchiveException
import java.io.Closeable
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream

// KMK -->
class ArchiveReader private constructor(
    private val openDescriptor: () -> ParcelFileDescriptor,
    private val zipIndexCacheFile: File?,
) : Closeable {
// KMK <--
    // KMK -->
    constructor(openDescriptor: () -> ParcelFileDescriptor) : this(openDescriptor, null)

    companion object {
        fun withZipIndexCache(
            cacheFile: File,
            openDescriptor: () -> ParcelFileDescriptor,
        ) = ArchiveReader(openDescriptor, cacheFile)
    }
    // KMK <--

    // KMK -->
    private val lock = Any()
    private var isClosed = false
    private var fallbackArchive: ArchiveInputStream? = null
    private val fallbackVisitedEntries = mutableSetOf<String>()
    val size = openDescriptor().use { it.statSize }

    // Use the descriptor granted by SAF directly, with Long offsets for ZIP64.
    private val zipArchive = openDescriptor().let { descriptor ->
        try {
            descriptor to ZipArchiveReader(
                ParcelFileDescriptor.AutoCloseInputStream(descriptor).channel,
                // KMK -->
                zipIndexCacheFile,
                // KMK <--
            )
        } catch (_: Exception) {
            descriptor.close()
            null // RAR, encrypted ZIP, or unsupported provider: use libarchive.
        }
    }

    private fun openArchive(usePassword: Boolean): ArchiveInputStream = synchronized(lock) {
        check(!isClosed) { "Archive reader is closed" }
        ArchiveInputStream(openDescriptor(), usePassword)
    }
    // KMK <--

    // SY -->
    var encrypted: Boolean = false
        private set
    var wrongPassword: Boolean? = null
        private set

    init {
        checkEncryptionStatus()
    }
    // SY <--

    // KMK -->
    fun <T> useEntries(block: (Sequence<ArchiveEntry>) -> T): T {
        synchronized(lock) {
            check(!isClosed) { "Archive reader is closed" }
            zipArchive?.let { return it.second.useEntries(block) }
        }
        return openArchive(encrypted).use { block(generateSequence { it.getNextEntry() }) }
    }
    // KMK <--

    // KMK -->
    fun getInputStream(entryName: String): InputStream? {
        synchronized(lock) {
            check(!isClosed) { "Archive reader is closed" }
            zipArchive?.let { return it.second.getInputStream(entryName) }
            return getFallbackInputStream(entryName)
        }
    }

    /**
     * Libarchive exposes RAR entries as a sequential stream. Keep one cursor open and spool only
     * the requested entry so normal forward reading does not reopen and replay the archive per page.
     */
    private fun getFallbackInputStream(entryName: String): InputStream? {
        if (entryName in fallbackVisitedEntries) resetFallbackArchive()
        val archive = fallbackArchive ?: openArchive(encrypted).also { fallbackArchive = it }
        while (true) {
            val entry = archive.getNextEntry() ?: return null
            fallbackVisitedEntries.add(entry.name)
            if (entry.name != entryName) continue

            val temporaryDirectory = zipIndexCacheFile?.parentFile?.parentFile?.let { File(it, "archive-entry-temp") }
            check(temporaryDirectory == null || temporaryDirectory.isDirectory || temporaryDirectory.mkdirs()) {
                "Cannot create archive entry cache"
            }
            val temporaryFile = File.createTempFile("archive-entry-", ".tmp", temporaryDirectory)
            try {
                temporaryFile.outputStream().buffered().use { archive.copyTo(it, 64 * 1024) }
                return object : FilterInputStream(temporaryFile.inputStream()) {
                    override fun close() {
                        try {
                            super.close()
                        } finally {
                            temporaryFile.delete()
                        }
                    }
                }
            } catch (error: Throwable) {
                temporaryFile.delete()
                resetFallbackArchive()
                throw error
            }
        }
    }

    private fun resetFallbackArchive() {
        fallbackArchive?.close()
        fallbackArchive = null
        fallbackVisitedEntries.clear()
    }
    // KMK <--

    // SY -->
    private fun checkEncryptionStatus() {
        // KMK -->
        if (zipArchive != null) return
        val archive = openArchive(false)
        // KMK <--
        try {
            // KMK -->
            if (archive.hasEncryptedEntries() == 0) {
                archive.close()
                return
            }
            // KMK <--
            while (true) {
                val entry = archive.getNextEntry() ?: break
                if (entry.isEncrypted) {
                    encrypted = true
                    isPasswordIncorrect(entry.name)
                    break
                }
            }
        } catch (e: ArchiveException) {
            archive.close()
            throw e
        }
        archive.close()
    }

    private fun isPasswordIncorrect(entryName: String) {
        try {
            getInputStream(entryName).use { stream ->
                stream!!.read()
            }
        } catch (e: ArchiveException) {
            if (e.message == "Incorrect passphrase") {
                wrongPassword = true
                return
            }
            throw e
        }
        wrongPassword = false
    }
    // SY <--

    // KMK -->
    override fun close() {
        synchronized(lock) {
            if (isClosed) return
            isClosed = true
            fallbackArchive?.close()
            fallbackArchive = null
            zipArchive?.let { (descriptor, reader) ->
                try {
                    reader.close()
                } finally {
                    descriptor.close()
                }
            }
        }
    }
    // KMK <--
}
