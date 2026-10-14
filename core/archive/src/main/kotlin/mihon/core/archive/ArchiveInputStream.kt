package mihon.core.archive
import android.os.ParcelFileDescriptor
import me.zhanghai.android.libarchive.Archive
import me.zhanghai.android.libarchive.ArchiveEntry
import me.zhanghai.android.libarchive.ArchiveException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.concurrent.Volatile
import mihon.core.archive.ArchiveEntry as MihonArchiveEntry

class ArchiveInputStream(
    // KMK -->
    private val pfd: ParcelFileDescriptor,
    // KMK <--
    // SY -->
    encrypted: Boolean,
    // SY <--
) : InputStream() {
    private val lock = Any()

    @Volatile
    private var isClosed = false

    private val archive = Archive.readNew()

    init {
        try {
            // SY -->
            if (encrypted) {
                Archive.readAddPassphrase(archive, CbzCrypto.getDecryptedPasswordCbz())
            }
            // SY <--
            Archive.setCharset(archive, Charsets.UTF_8.name().toByteArray())
            Archive.readSupportFilterAll(archive)
            Archive.readSupportFormatAll(archive)
            // KMK -->
            Archive.readOpenFd(archive, pfd.fd, 64 * 1024L)
            // KMK <--
        } catch (e: ArchiveException) {
            close()
            throw e
        }
    }

    // KMK -->
    fun hasEncryptedEntries(): Int = Archive.readHasEncryptedEntries(archive)

    private val oneByteBuffer = ByteBuffer.allocateDirect(1)

    override fun read(): Int {
        oneByteBuffer.clear()
        Archive.readData(archive, oneByteBuffer)
        oneByteBuffer.flip()
        return if (oneByteBuffer.hasRemaining()) oneByteBuffer.get().toUByte().toInt() else -1
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || len > b.size - off) throw IndexOutOfBoundsException()
        if (len == 0) return 0

        val buffer = ByteBuffer.wrap(b, off, len)
        Archive.readData(archive, buffer)
        return (buffer.position() - off).takeIf { it > 0 } ?: -1
    }
    // KMK <--

    override fun close() {
        synchronized(lock) {
            if (isClosed) return
            isClosed = true
        }

        // KMK -->
        try {
            Archive.readFree(archive)
        } finally {
            pfd.close()
        }
        // KMK <--
    }

    fun getNextEntry(): MihonArchiveEntry? {
        return Archive.readNextHeader(archive).takeUnless { it == 0L }?.let { entry ->
            val name = ArchiveEntry.pathnameUtf8(entry) ?: ArchiveEntry.pathname(entry)?.decodeToString() ?: return null
            val isFile = ArchiveEntry.filetype(entry) == ArchiveEntry.AE_IFREG
            // SY -->
            val isEncrypted = ArchiveEntry.isEncrypted(entry)
            // SY <--
            MihonArchiveEntry(
                name,
                isFile,
                // SY -->
                isEncrypted,
                // SY <--
            )
        }
    }
}
