<div align="center">
  <img src="./branding/komiyomi-logo.svg" width="420" alt="Komiyomi" />

  <p><strong>An offline comic reader for Android</strong></p>

  [![Android build](https://github.com/wongpinter/komiyomi/actions/workflows/build_pull_request.yml/badge.svg)](https://github.com/wongpinter/komiyomi/actions/workflows/build_pull_request.yml)
  [![License: Apache-2.0](https://img.shields.io/github/license/wongpinter/komiyomi)](./LICENSE)
</div>

Komiyomi is a focused reader and library manager for comics you already have on your device. Browse folders, keep a library of CBZ and CBR files, and continue reading offline.

## Features

- Library catalog for CBZ and CBR files in your selected folder, including nested folders.
- Folder browsing to organize and open your existing collection in place.
- Cover extraction and optional `ComicInfo.xml` metadata.
- Reading progress and page bookmarks saved locally.
- Page-by-page and continuous reading, with manga and Western comic controls.
- On-demand archive page loading designed for very large, multi-gigabyte files without unpacking a whole book into memory.
- Chapter navigation for archives that contain chapter directories.

Archive speed depends on the archive format, compression method, storage, and device. CBZ supports direct page access; some CBR archives need sequential decoding when seeking to uncached pages.

## Open your comics

1. Install Komiyomi and grant access to the folder that contains your comics.
2. Keep your existing `.cbz` and `.cbr` files in that folder or its subfolders.
3. Open **Library** for the collection or **Browse** to navigate folders directly.
4. Use the library refresh control after adding or moving files.

Komiyomi reads the selected files in place; it does not require you to extract the archives first.

## Build

- Android Studio with Android SDK 36
- JDK 21

Build a debug APK with:

```sh
./gradlew assembleDebug
```

The debug application ID is `app.komiyomi.dev`; release builds use `app.komiyomi`.

## Project lineage

Komiyomi is a separate app derived from the Komikku codebase, which is based on Mihon and TachiyomiSY and includes work from other open-source projects. Original copyright notices, licenses, and upstream attributions are retained. See [LICENSE](./LICENSE) and the notices in the source tree.

Komiyomi is not affiliated with comic publishers or content providers and does not distribute comic files. You provide and manage your own collection.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md). Bug reports and feature requests can be opened in [GitHub Issues](https://github.com/wongpinter/komiyomi/issues).

## License

Komiyomi is distributed under the Apache License 2.0. See [LICENSE](./LICENSE).
