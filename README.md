<div align="center">
  <img src="./branding/komiyomi-logo.svg" width="420" alt="Komiyomi" />

  <p><strong>An offline CBZ and CBR reader for Android</strong></p>

  [![Android build](https://github.com/wongpinter/komiyomi/actions/workflows/build_pull_request.yml/badge.svg)](https://github.com/wongpinter/komiyomi/actions/workflows/build_pull_request.yml)
  [![License: Apache-2.0](https://img.shields.io/github/license/wongpinter/komiyomi)](./LICENSE)
</div>

Komiyomi is a separate Android app forked from [Komikku](https://github.com/komikku-app/komikku). It refocuses Komikku's open-source foundation on comics stored on your device: browse folders, build a library of CBZ and CBR files, and read offline. Komiyomi does not provide or host comic content; you choose and manage your own files.

Requires Android 8.0 or later.

## Features

- **Local library:** scan a selected folder and its subfolders to find CBZ and CBR comics.
- **Folder browsing:** navigate your collection and open files in place.
- **Covers and metadata:** extract archive covers and read optional `ComicInfo.xml` metadata.
- **Reading progress and bookmarks:** save your place locally and resume later.
- **Reader modes:** page-by-page and continuous reading, with controls for manga and Western comics.
- **Large archive support:** load pages on demand, with multi-gigabyte archives as a core performance focus instead of unpacking a whole book into memory.
- **Chapter navigation:** move through chapter directories included inside an archive.

Archive speed depends on the archive format, compression method, storage, and device. CBZ supports direct page access; some CBR archives need sequential decoding when seeking to uncached pages.

## Open your comics

1. Keep your `.cbz` and `.cbr` files in a folder accessible to Android.
2. In Komiyomi, open **Browse**, choose that folder, and grant Android access when prompted.
3. Scan the folder. Comics in its subfolders will appear in **Library**; use Browse to navigate folders directly.
4. Scan again after adding or moving files.

Komiyomi reads files in place; you do not need to extract archives or copy them into a special app folder. The core reading workflow works offline once your files are available on the device.

## Project lineage

Komiyomi is a separate app forked from [Komikku](https://github.com/komikku-app/komikku). Komikku is based on [Mihon](https://github.com/mihonapp/mihon) and includes work from TachiyomiSY and other open-source projects. Komiyomi continues that lineage with a focused purpose: managing and reading local CBZ and CBR collections offline. Thank you to the upstream maintainers and contributors.

Original copyright notices, licenses, and upstream attributions are retained. See [LICENSE](./LICENSE) and notices in the source tree.

## Build

Use Android Studio with Android SDK 36 and JDK 21. Build a debug APK with:

```sh
./gradlew assembleDebug
```

The debug application ID is `app.komiyomi.dev`; release builds use `app.komiyomi`.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md). Bug reports and feature requests can be opened in [GitHub Issues](https://github.com/wongpinter/komiyomi/issues).

## License

Komiyomi is distributed under the Apache License 2.0. See [LICENSE](./LICENSE).
