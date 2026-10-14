package mihon.core.migration.migrations

import mihon.core.migration.Migration

val migrations: List<Migration>
    get() = listOf(
        SetupBackupCreateMigration(),
        // KMK -->
        CancelRemovedOnlineWorkMigration(),
        EHentaiMigration(),
        MergedMangaDedupeModeMigration(),
        // KMK <--
        CategoryPreferencesCleanupMigration(),
        // SY -->
        RemoveDuplicateReaderPreferenceMigration(),
        // SY <--
        InstallationIdMigration(),
    )
