package tachiyomi.source.local.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import mihon.core.archive.ZipWriter
import mihon.core.archive.archiveReader
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.local.io.Archive
import tachiyomi.source.local.io.LocalSourceFileSystem
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

private const val DEFAULT_COVER_NAME = "cover.jpg"
private const val COVER_ARCHIVE_NAME = "cover.cbi"

actual class LocalCoverManager(
    private val context: Context,
    private val fileSystem: LocalSourceFileSystem,
) {

    // KMK -->
    actual fun find(mangaUrl: String): UniFile? {
        val mangaDirectory = fileSystem.getMangaDirectory(mangaUrl)
        val coverFiles = if (mangaDirectory != null) {
            fileSystem.getFilesInMangaDirectory(mangaUrl)
        } else {
            val archive = fileSystem.resolveFile(mangaUrl)
                ?.takeIf { it.isFile && Archive.isSupported(it) }
                ?: return null
            archiveCoverDirectory(archive).listFiles().orEmpty().mapNotNull { UniFile.fromFile(it) }
        }

        return coverFiles
            .filter { it.isFile && it.nameWithoutExtension.equals("cover", ignoreCase = true) }
            .firstOrNull {
                ImageUtil.isImage(it.name) { it.openInputStream() } || it.name == COVER_ARCHIVE_NAME
            }
    }

    private fun archiveCoverDirectory(
        archive: UniFile,
        size: Long = archive.length(),
        modified: Long = archive.lastModified(),
    ): File =
        File(
            context.cacheDir,
            "local-archive-covers/" + MessageDigest.getInstance("SHA-256")
                .digest("${archive.uri}:$modified:$size".toByteArray())
                .joinToString("") { "%02x".format(it) },
        )

    /** Extract just the first page and cache a small thumbnail, never the whole archive. */
    suspend fun getArchiveCover(
        archive: UniFile,
        size: Long = archive.length(),
        modified: Long = archive.lastModified(),
    ): UniFile? = withIOContext {
        val directory = archiveCoverDirectory(archive, size, modified)
        val cover = File(directory, DEFAULT_COVER_NAME)
        if (cover.isFile && cover.length() > 0) return@withIOContext UniFile.fromFile(cover)
        val encryptedCover = File(directory, COVER_ARCHIVE_NAME)
        if (encryptedCover.isFile && encryptedCover.length() > 0) {
            return@withIOContext UniFile.fromFile(encryptedCover)
        }
        currentCoroutineContext().ensureActive()
        val coroutineContext = currentCoroutineContext()
        archive.archiveReader(context).use { reader ->
            val firstPage = reader.useEntries { entries ->
                entries.filter {
                    coroutineContext.ensureActive()
                    it.isFile && !it.name.startsWith("__MACOSX/") &&
                        !it.name.substringAfterLast('/').startsWith("._") &&
                        ImageUtil.isImage(it.name) { reader.getInputStream(it.name)!! }
                }
                    .minWithOrNull { a, b -> a.name.compareToCaseInsensitiveNaturalOrder(b.name) }
            } ?: return@withIOContext null
            check(directory.isDirectory || directory.mkdirs()) { "Cannot create cover cache" }
            val pageFile = File.createTempFile("page-", ".tmp", directory)
            try {
                val stream = reader.getInputStream(firstPage.name) ?: return@withIOContext null
                stream.use { input ->
                    pageFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var copied = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            copied += count
                            check(copied <= 128L * 1024 * 1024) { "Cover page is too large" }
                            output.write(buffer, 0, count)
                        }
                    }
                }
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(pageFile.path, options)
                if (options.outWidth <= 0 || options.outHeight <= 0) return@withIOContext null
                options.inJustDecodeBounds = false
                options.inSampleSize = 1
                while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 768) {
                    options.inSampleSize *= 2
                }
                val thumbnailFile = File.createTempFile("cover-", ".tmp", directory)
                try {
                    val bitmap = BitmapFactory.decodeFile(pageFile.path, options) ?: return@withIOContext null
                    try {
                        thumbnailFile.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
                    } finally {
                        bitmap.recycle()
                    }
                    currentCoroutineContext().ensureActive()
                    if (reader.encrypted) {
                        val encryptedTemp = File.createTempFile("encrypted-cover-", ".tmp", directory)
                        try {
                            ZipWriter(context, UniFile.fromFile(encryptedTemp)!!, encrypt = true).use { writer ->
                                writer.write(thumbnailFile.readBytes(), DEFAULT_COVER_NAME)
                            }
                            check(encryptedTemp.renameTo(encryptedCover)) { "Cannot save encrypted cover" }
                            UniFile.fromFile(encryptedCover)
                        } finally {
                            encryptedTemp.delete()
                        }
                    } else {
                        check(thumbnailFile.renameTo(cover)) { "Cannot save cover thumbnail" }
                        UniFile.fromFile(cover)
                    }
                } finally {
                    thumbnailFile.delete()
                }
            } finally {
                pageFile.delete()
            }
        }
    }
    // KMK <--

    actual fun update(
        manga: SManga,
        inputStream: InputStream,
        // SY -->
        encrypted: Boolean,
        // SY <--
    ): UniFile? {
        // KMK -->
        val mangaDirectory = fileSystem.getMangaDirectory(manga.url)
        val standaloneArchive = if (mangaDirectory == null) {
            fileSystem.resolveFile(manga.url)
                ?.takeIf { it.isFile && Archive.isSupported(it) }
        } else {
            null
        }
        val directory = mangaDirectory ?: standaloneArchive?.let {
            archiveCoverDirectory(it).apply { mkdirs() }.let { cacheDirectory -> UniFile.fromFile(cacheDirectory) }
        }
        // KMK <--
        if (directory == null) {
            inputStream.close()
            return null
        }

        // KMK -->
        val targetName = if (encrypted) COVER_ARCHIVE_NAME else DEFAULT_COVER_NAME
        val targetFile = directory.listFiles().orEmpty().firstOrNull { it.name.equals(targetName, ignoreCase = true) }
            ?: directory.createFile(targetName)
            ?: run {
                inputStream.close()
                return null
            }
        // KMK <--

        inputStream.use { input ->
            // SY -->
            if (encrypted) {
                ZipWriter(context, targetFile, encrypt = true).use { writer ->
                    // KMK -->
                    writer.write(input.readBytes(), DEFAULT_COVER_NAME)
                    // KMK <--
                }
            } else {
                // SY <--
                targetFile.openOutputStream().use { output ->
                    input.copyTo(output)
                }
                // SY -->
            }
            // SY <--
        }

        // KMK -->
        directory.listFiles().orEmpty()
            .filter {
                it.uri != targetFile.uri &&
                    (
                        it.name.equals(DEFAULT_COVER_NAME, ignoreCase = true) ||
                            it.name.equals(COVER_ARCHIVE_NAME, ignoreCase = true)
                        )
            }
            .forEach { it.delete() }
        // KMK <--
        DiskUtil.createNoMediaFile(directory, context)
        manga.thumbnail_url = targetFile.uri.toString()
        return targetFile
    }
}
