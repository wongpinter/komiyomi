package tachiyomi.domain.source.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount

interface SourceRepository {

    fun getSourcesWithNonLibraryManga(): Flow<List<SourceWithCount>>
}
