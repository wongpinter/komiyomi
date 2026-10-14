package mihon.core.archive

import android.content.Context
import android.os.ParcelFileDescriptor
import com.hippo.unifile.UniFile
import java.io.File
import java.security.MessageDigest

internal fun UniFile.openFileDescriptor(context: Context, mode: String): ParcelFileDescriptor =
    context.contentResolver.openFileDescriptor(uri, mode) ?: error("Failed to open file descriptor: ${filePath ?: uri}")

// KMK -->
fun UniFile.archiveReader(context: Context): ArchiveReader {
    val indexKey = MessageDigest.getInstance("SHA-256")
        .digest("$uri:${lastModified()}".toByteArray())
        .joinToString("") { "%02x".format(it) }
    return ArchiveReader.withZipIndexCache(File(File(context.cacheDir, "zip-directory-index"), indexKey)) {
        openFileDescriptor(context, "r")
    }
}
// KMK <--

fun UniFile.epubReader(context: Context) = EpubReader(archiveReader(context))
