# Komiyomi – AI Agent Guide

Komiyomi is an Android offline comic reader focused on local CBZ/CBR libraries. It is a separate application derived from Komikku and, through it, Mihon and TachiyomiSY. Preserve upstream licenses, copyright notices, and attribution.

## Project identity

- Display name: Komiyomi (`こみよみ`)
- Application ID: `app.komiyomi` (debug: `app.komiyomi.dev`)
- Main use: scan local folders, manage a library, and read CBZ/CBR archives offline.
- Android min SDK 26; target SDK 36; JVM target 17; build with JDK 21.

## Changes and upstream markers

Wrap Komiyomi-specific changes to existing source with `// KMK -->` and `// KMK <--`. Keep Mihon `// SY -->` / `// SY <--` and legacy `// EXH` blocks intact; nest Komiyomi changes inside them when needed. Do not add new `// SY` or `// EXH` blocks.

For XML, use `<!-- KMK -->` and `<!-- /KMK -->`; XML comments cannot contain the `--` sequence used in the Kotlin closing marker.

All new Komiyomi strings belong in `i18n-kmk/src/commonMain/moko-resources/base/strings.xml` and use `KMR`. Do not edit translated locale files; Weblate owns them.

Keep changes focused on offline library management, archive access, image loading, metadata, reading progress, and reader controls. Do not introduce a dependency on Komikku's private services or secrets. Configure an updater only for an explicitly configured Komiyomi repository.

## Git

- Create a feature branch for each task.
- Do not commit directly to `main` or `master` unless the user explicitly asks.
- Push only a feature branch unless the user explicitly asks otherwise.

## Architecture

- UI: Jetpack Compose, Material 3, Voyager.
- DI: Injekt; register app-specific services in the existing app/domain modules.
- Database: SQLDelight; schema changes require a migration, query and mapper updates, and `:data:generateSqlDelightInterface`.
- Concurrency: Kotlin coroutines and Flow; use lifecycle-owned scopes in screens and models.
- Images: Coil 3.
- Source compatibility: avoid breaking the extension ABI in `source-api`.

## Build and formatting

After Kotlin/XML changes, run these commands in order:

```bash
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew assembleDebug
```

Use JDK 21. Builds without `-Pinclude-telemetry` do not require Firebase secrets. Do not add or run tests unless the user asks for verification; preserve the existing CI test workflow.
