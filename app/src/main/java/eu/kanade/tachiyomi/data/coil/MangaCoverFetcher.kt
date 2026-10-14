package eu.kanade.tachiyomi.data.coil

import androidx.core.net.toUri
import coil3.Extras
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.getOrDefault
import coil3.request.Options
import com.hippo.unifile.UniFile
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File

/** Loads local cover files and content URIs for manga in the offline library. */
class MangaCoverFetcher(
    private val mangaCover: MangaCover,
    private val url: String? = mangaCover.url,
    private val options: Options,
    private val customCoverFileLazy: Lazy<File>,
    private val diskCacheKey: String,
) : Fetcher {

    private val scope by lazy { CoroutineScope(Dispatchers.IO) }
    private val uiPreferences = Injekt.get<UiPreferences>()
    private val themeCoverBased = uiPreferences.themeCoverBased().get()
    private val preloadLibraryColor = uiPreferences.preloadLibraryColor().get()

    override suspend fun fetch(): FetchResult {
        if (options.extras.getOrDefault(USE_CUSTOM_COVER_KEY)) {
            val customCoverFile = customCoverFileLazy.value
            if (customCoverFile.exists()) return fileLoader(customCoverFile)
        }

        val coverUrl = url ?: error("No cover specified")
        return when {
            coverUrl.startsWith("file://") -> fileLoader(File(coverUrl.removePrefix("file://")))
            coverUrl.startsWith('/') -> fileLoader(File(coverUrl))
            coverUrl.startsWith("content:") -> fileUriLoader(coverUrl)
            else -> error("Only local cover images are supported")
        }
    }

    private fun fileLoader(file: File): FetchResult {
        setRatioAndColorsInScope(mangaCover, ogFile = file)
        return SourceFetchResult(
            source = ImageSource(
                file = file.toOkioPath(),
                fileSystem = FileSystem.SYSTEM,
                diskCacheKey = diskCacheKey,
            ),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    private fun fileUriLoader(uri: String): FetchResult {
        setRatioAndColorsInScope(mangaCover)
        val source = UniFile.fromUri(options.context, uri.toUri())!!
            .openInputStream()
            .source()
            .buffer()
        return SourceFetchResult(
            source = ImageSource(source = source, fileSystem = FileSystem.SYSTEM),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    private fun setRatioAndColorsInScope(
        mangaCover: MangaCover,
        ogFile: File? = null,
        onlyDominantColor: Boolean = !themeCoverBased,
    ) {
        if (!preloadLibraryColor) return
        scope.launch {
            MangaCoverMetadata.setRatioAndColors(mangaCover, ogFile = ogFile, onlyDominantColor = onlyDominantColor)
        }
    }

    class MangaFactory : Fetcher.Factory<Manga> {
        private val coverCache: CoverCache by injectLazy()

        override fun create(data: Manga, options: Options, imageLoader: ImageLoader): Fetcher = MangaCoverFetcher(
            mangaCover = data.asMangaCover(),
            options = options,
            customCoverFileLazy = lazy { coverCache.getCustomCoverFile(data.id) },
            diskCacheKey = imageLoader.components.key(data, options)!!,
        )
    }

    class MangaCoverFactory : Fetcher.Factory<MangaCover> {
        private val coverCache: CoverCache by injectLazy()

        override fun create(data: MangaCover, options: Options, imageLoader: ImageLoader): Fetcher = MangaCoverFetcher(
            mangaCover = data,
            options = options,
            customCoverFileLazy = lazy { coverCache.getCustomCoverFile(data.mangaId) },
            diskCacheKey = imageLoader.components.key(data, options)!!,
        )
    }

    companion object {
        val USE_CUSTOM_COVER_KEY = Extras.Key(true)
    }
}
