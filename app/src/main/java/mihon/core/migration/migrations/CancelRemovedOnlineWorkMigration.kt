package mihon.core.migration.migrations

import android.app.Application
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.guava.await
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class CancelRemovedOnlineWorkMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val application = migrationContext.get<Application>() ?: return false
        val workManager = application.workManager

        workManager.cancelAllWorkByTag(TAG_LIBRARY_UPDATE).result.await()
        workManager.cancelAllWorkByTag(TAG_SYNC_DATA).result.await()
        workManager.cancelAllWorkByTag(TAG_EHENTAI_UPDATE).result.await()

        return true
    }

    private companion object {
        const val TAG_LIBRARY_UPDATE = "LibraryUpdate"
        const val TAG_SYNC_DATA = "SyncDataJob"
        const val TAG_EHENTAI_UPDATE = "EHBackgroundUpdater"
    }
}
