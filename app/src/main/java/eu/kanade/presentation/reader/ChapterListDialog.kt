package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.manga.components.MangaChapterListItem
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import eu.kanade.tachiyomi.util.lang.toRelativeString
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ChapterListDialog(
    onDismissRequest: () -> Unit,
    chapters: ImmutableList<ReaderChapterItem>,
    onClickChapter: (Chapter) -> Unit,
    onBookmark: (Chapter) -> Unit,
    dateRelativeTime: Boolean,
) {
    val context = LocalContext.current
    val state = rememberLazyListState(chapters.indexOfFirst { it.isCurrent }.coerceAtLeast(0))

    AdaptiveSheet(
        onDismissRequest = onDismissRequest,
    ) {
        LazyColumn(
            state = state,
            modifier = Modifier.heightIn(min = 200.dp, max = 500.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(
                items = chapters,
                key = { "chapter-list-${it.chapter.id}" },
            ) { chapterItem ->
                MangaChapterListItem(
                    title = chapterItem.chapter.name,
                    date = chapterItem.chapter.dateUpload
                        .takeIf { it > 0L }
                        ?.let {
                            LocalDate.ofInstant(
                                Instant.ofEpochMilli(it),
                                ZoneId.systemDefault(),
                            ).toRelativeString(context, dateRelativeTime, chapterItem.dateFormat)
                        },
                    readProgress = null,
                    scanlator = chapterItem.chapter.scanlator,
                    sourceName = null,
                    read = chapterItem.chapter.read,
                    bookmark = chapterItem.chapter.bookmark,
                    selected = false,
                    chapterSwipeStartAction = LibraryPreferences.ChapterSwipeAction.ToggleBookmark,
                    chapterSwipeEndAction = LibraryPreferences.ChapterSwipeAction.ToggleBookmark,
                    onLongClick = { /*TODO*/ },
                    onClick = { onClickChapter(chapterItem.chapter) },
                    onChapterSwipe = {
                        onBookmark(chapterItem.chapter)
                    },
                )
            }
        }
    }
}
