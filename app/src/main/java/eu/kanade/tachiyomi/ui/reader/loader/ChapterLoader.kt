package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import mihon.core.archive.ArchiveSection
import mihon.core.archive.archiveReader
import mihon.core.archive.epubReader
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.io.Format

/**
 * Loads pages from a chapter in the local library.
 */
class ChapterLoader(
    private val context: Context,
    private val source: Source,
    private val readerPrefs: ReaderPreferences,
) {

    /**
     * Assigns the chapter's page loader and loads its pages. Returns if the chapter is ready.
     */
    suspend fun loadChapter(chapter: ReaderChapter, page: Int? = null) {
        if (chapterIsReady(chapter)) return

        chapter.state = ReaderChapter.State.Loading
        withIOContext {
            logcat { "Loading pages for ${chapter.chapter.name}" }
            try {
                val loader = getPageLoader(chapter)
                chapter.pageLoader = loader

                val pages = loader.getPages().onEach { it.chapter = chapter }
                if (pages.isEmpty()) {
                    throw Exception(context.stringResource(MR.strings.page_list_empty_error))
                }

                if (!chapter.chapter.read || readerPrefs.preserveReadingPosition().get() || page != null || source is LocalSource) {
                    chapter.requestedPage = page ?: chapter.chapter.last_page_read
                }

                // KMK -->
                val archivePaths = pages.mapNotNull { it.archivePath }
                val sections = if (archivePaths.size == pages.size) ArchiveSection.fromPagePaths(archivePaths) else emptyList()
                chapter.state = ReaderChapter.State.Loaded(pages, sections)
                // KMK <--
            } catch (e: Throwable) {
                chapter.state = ReaderChapter.State.Error(e)
                throw e
            }
        }
    }

    private fun chapterIsReady(chapter: ReaderChapter): Boolean =
        chapter.state is ReaderChapter.State.Loaded && chapter.pageLoader != null

    private fun getPageLoader(chapter: ReaderChapter): PageLoader =
        when (source) {
            is LocalSource -> when (val format = source.getFormat(chapter.chapter)) {
                is Format.Directory -> DirectoryPageLoader(format.file)
                is Format.Archive -> ArchivePageLoader(
                    format.file.archiveReader(context),
                    // KMK -->
                    format.file,
                    cacheOfflinePages = true,
                    // KMK <--
                )
                is Format.Epub -> EpubPageLoader(format.file.epubReader(context))
            }
            is StubSource -> error(context.stringResource(MR.strings.source_not_installed, source.toString()))
            else -> error(context.stringResource(MR.strings.loader_not_implemented_error))
        }
}
