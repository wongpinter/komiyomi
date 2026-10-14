package eu.kanade.domain.manga.interactor

import eu.kanade.domain.manga.model.WESTERN_COMIC_READER_FLAG
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository

class SetMangaViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun awaitSetReadingMode(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        // KMK -->
        val viewerFlags = manga.viewerFlags.setFlag(flag, ReadingMode.MASK.toLong())
            .and(WESTERN_COMIC_READER_FLAG.inv())
        // KMK <--
        mangaRepository.update(
            MangaUpdate(
                id = id,
                // KMK -->
                viewerFlags = viewerFlags,
                // KMK <--
            ),
        )
    }

    // KMK -->
    suspend fun awaitSetWesternComicMode(id: Long, enabled: Boolean) {
        val manga = mangaRepository.getMangaById(id)
        val viewerFlags = if (enabled) {
            manga.viewerFlags or WESTERN_COMIC_READER_FLAG
        } else {
            manga.viewerFlags and WESTERN_COMIC_READER_FLAG.inv()
        }
        mangaRepository.update(MangaUpdate(id = id, viewerFlags = viewerFlags))
    }
    // KMK <--

    suspend fun awaitSetOrientation(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.update(
            MangaUpdate(
                id = id,
                viewerFlags = manga.viewerFlags.setFlag(flag, ReaderOrientation.MASK.toLong()),
            ),
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
