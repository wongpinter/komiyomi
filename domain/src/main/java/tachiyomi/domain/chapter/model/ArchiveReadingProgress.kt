// KMK -->
package tachiyomi.domain.chapter.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

const val ARCHIVE_PAGE_COUNT = "komiyomi_page_count"
const val ARCHIVE_PAGE_BOOKMARKS = "komiyomi_page_bookmarks"

fun JsonObject.archivePageCount(): Int = (get(ARCHIVE_PAGE_COUNT) as? JsonPrimitive)?.intOrNull?.coerceAtLeast(0) ?: 0

fun JsonObject.archivePageBookmarks(): List<Int> = (get(ARCHIVE_PAGE_BOOKMARKS) as? JsonArray)
    ?.mapNotNull { (it as? JsonPrimitive)?.intOrNull?.takeIf { page -> page >= 0 } }
    ?.distinct()?.sorted().orEmpty()

data class ArchiveReadingProgress(
    val chapterId: Long,
    val mangaId: Long,
    val path: String,
    val completed: Boolean,
    val lastPage: Long,
    val pageCount: Int,
    val lastRead: Long,
) {
    val started: Boolean get() = completed || lastPage > 0 || lastRead > 0
    val fraction: Float get() = if (completed) {
        1f
    } else if (pageCount > 0) {
        ((lastPage + 1).toFloat() / pageCount).coerceIn(0f, 1f)
    } else {
        0f
    }
}
// KMK <--
