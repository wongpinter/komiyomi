package tachiyomi.source.local.io

import com.hippo.unifile.UniFile
import tachiyomi.domain.storage.service.StorageManager

actual class LocalSourceFileSystem(
    private val storageManager: StorageManager,
) {

    actual fun getBaseDirectory(): UniFile? {
        return storageManager.getLocalSourceDirectory()
    }

    actual fun getFilesInBaseDirectory(): List<UniFile> {
        return getBaseDirectory()?.listFiles().orEmpty().toList()
    }

    // KMK -->
    actual fun resolveFile(relativePath: String): UniFile? {
        val parts = relativePath.split('/')
        if (parts.any { it.isEmpty() || it == "." || it == ".." }) return null
        return parts.fold(getBaseDirectory()) { directory, name -> directory?.findFile(name) }
    }
    // KMK <--

    actual fun getMangaDirectory(name: String): UniFile? {
        // KMK -->
        return resolveFile(name)
            // KMK <--
            ?.takeIf { it.isDirectory }
    }

    actual fun getFilesInMangaDirectory(name: String): List<UniFile> {
        // KMK -->
        return resolveFile(name)
            // KMK <--
            ?.takeIf { it.isDirectory }
            ?.listFiles().orEmpty().toList()
    }
}
