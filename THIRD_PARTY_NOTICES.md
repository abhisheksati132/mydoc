# Third-Party Software Notices and Licenses

MyDoc bundles or links the components below. Each entry states what is actually packaged in the
released APK, not what was once considered: the dependency set is the one Gradle resolves for
`app:dependencies --configuration releaseRuntimeClasspath` (AGP 9.5.0, Kotlin 2.3.21), and the native
and asset payload was read out of a built APK.

Licence texts that a licence requires to travel with the work do travel with it: the AGPL-3.0 covering
this repository is in [`LICENSE`](LICENSE), and each bundled font's SIL Open Font License 1.1 text is
in [`app/src/main/assets/licences/`](app/src/main/assets/licences), so the copy inside the APK carries
it as well.

---

## Components linked into the app

### LibreOfficeKit Word engine
- **Version**: 26.2.6.3; copyright LibreOffice contributors and The Document Foundation.
- **Use**: Full offline Word-family page conversion in a dedicated process, with bundled fonts and native dependencies (including NSS/NSPR and SQLite).
- **Licenses/source/build provenance**: [Complete engine notices](third_party/libreoffice/README.md), including exact upstream source and F-Droid source/build links. The MPL/GPL/LGPL texts, upstream NOTICE and complete third-party/font license collection are packaged under `assets/licences/libreoffice` as well as retained in this repository.
- Native libraries/runtime assets are unmodified; the three modified Java adapters are published under their existing MPL 2.0 headers. This is an independent integration, not a LibreOffice-endorsed application.

### 1. Artifex MuPDF
- **Project**: [MuPDF](https://mupdf.com/) — `com.artifex.mupdf:fitz:1.28.5`
- **Copyright**: © Artifex Software, Inc.
- **License**: GNU Affero General Public License v3.0 (AGPLv3) — the full text is in [`LICENSE`](LICENSE)
- **Use**: Native PDF, XPS and EPUB rendering (Fitz JNI), PDF encryption, and the page display lists
  behind zoom tiles.
- **Corresponding source**: MyDoc's complete corresponding source for the AGPL-covered work is the
  public repository this file ships from, at the tag matching the release you downloaded. MuPDF's own
  source is available from Artifex at the version above.

### 2. PDFBox-Android
- **Project**: [PDFBox-Android](https://github.com/TomRoush/PdfBox-Android) — `com.tom-roush:pdfbox-android:2.0.27.0`
- **Copyright**: © The Apache Software Foundation; © Tom Roush
- **License**: Apache License, Version 2.0 (declared in the artifact's POM)
- **Use**: Streaming Office parsers (DOCX/XLSX/PPTX to PDF), PDF metadata extraction, text stripping,
  image recompression during PDF optimisation.
- **Its own dependencies**: Apache PDFBox, plus `org.json:json` (Public Domain, per its POM) and
  `net.sf.kxml:kxml2` (BSD-style or Public Domain, dual, per its POM).

### 3. Google ML Kit Text Recognition
- **Project**: [ML Kit](https://developers.google.com/ml-kit) — `com.google.mlkit:text-recognition:16.0.1`
- **Copyright**: © Google LLC
- **License**: Apache License, Version 2.0 for the client library; the bundled model files are
  distributed under Google's ML Kit terms of service.
- **Use**: On-device OCR of images and PDF pages. This is the model-bundled artifact: the OCR models
  and `lib/<abi>/libmlkit_google_ocr_pipeline.so` are packaged inside the APK — read from a built
  artifact as `assets/mlkit-google-ocr-models/**` — and no model is fetched at runtime.
- **Transitive components**: ML Kit pulls in `com.google.android.datatransport`, `com.google.firebase`
  and `com.google.android.gms` artifacts (Apache License, Version 2.0). Their transport components are
  packaged and registered in the merged manifest: `TransportBackendDiscovery` with a
  `cct.CctBackendFactory` backend, `JobInfoSchedulerService`, and
  `AlarmManagerSchedulerBroadcastReceiver`. MyDoc does not call them and no traffic from them was
  observed; [SECURITY.md](SECURITY.md) §4 states exactly what is and is not claimed about them.

### 4. AndroidX Camera
- **Project**: [AndroidX Camera](https://developer.android.com/media/camera/camera-architecture) —
  `androidx.camera:camera-*:1.6.2`
- **Copyright**: © The Android Open Source Project
- **License**: Apache License, Version 2.0
- **Use**: Camera preview and image capture for document scanning.

### 5. AndroidX Media3 / ExoPlayer
- **Project**: [AndroidX Media3](https://github.com/androidx/media) — `androidx.media3:*:1.11.1`
- **Copyright**: © The Android Open Source Project
- **License**: Apache License, Version 2.0
- **Use**: Local audio and video playback.

### 6. Jetpack Compose and AndroidX
- **Projects**: [AndroidX](https://developer.android.com/jetpack/androidx/startups/dependencies) —
  `androidx.compose:compose-bom:2026.09.00`, `androidx.compose.material3:material3:1.5.0-rc01`,
  `androidx.activity:activity-compose:1.13.0`, `androidx.core:core-ktx:1.19.1`,
  `androidx.core:core-splashscreen:1.2.0`, `androidx.lifecycle:*:2.11.0`,
  `androidx.exifinterface:exifinterface:1.4.2`,
  `androidx.compose.material:material-icons-extended:1.7.8`
- **Copyright**: © The Android Open Source Project
- **License**: Apache License, Version 2.0
- **Use**: UI layer, insets and lifecycle handling, EXIF orientation reads, splash screen.

### 7. AndroidX Room
- **Project**: [Room](https://developer.android.com/training/data-storage/room) — `androidx.room:*:2.8.5`
- **Copyright**: © The Android Open Source Project
- **License**: Apache License, Version 2.0
- **Use**: The local database: recent documents, favourites, offline-copy records, trash, playback positions.

### 8. Coil
- **Project**: [Coil](https://github.com/coil-kt/Coil) — `io.coil-kt:coil-{compose,gif,video}:2.7.0`
- **Copyright**: © Coil contributors
- **License**: Apache License, Version 2.0
- **Use**: Thumbnail, GIF and video-frame loading in Compose.

### 9. Haze
- **Project**: [Haze](https://github.com/chrisbanes/haze) — `dev.chrisbanes.haze:haze:2.1.0`, `dev.chrisbanes.haze:haze-glass:2.1.0`, `dev.chrisbanes.haze:haze-blur:2.1.0`
- **Copyright**: © Chris Banes (named in the artifact's POM)
- **License**: Apache License, Version 2.0 (declared in the artifact's POM)
- **Use**: The backdrop blur that gives the glass surfaces their optics.

### 10. Kotlin
- **Projects**: [Kotlin](https://kotlinlang.org/) — `org.jetbrains.kotlin:kotlin-stdlib:2.3.21`,
  `org.jetbrains.kotlinx:kotlinx-coroutines-*:1.11.0`, `org.jetbrains.kotlinx:kotlinx-serialization-*:1.8.1`
- **Copyright**: © JetBrains s.r.o. and Kotlin contributors
- **License**: Apache License, Version 2.0, with the licensing exceptions described in the artifacts'
  own `LICENSE` files
- **Use**: Language runtime, coroutines, serialization of persisted state.

---

## Fonts

Each font below is bundled as a resource in `app/src/main/res/font` and is licensed under the SIL Open
Font License, Version 1.1. The licence requires the copyright notice and the licence text to accompany
every copy of the font, so the upstream texts are bundled at `app/src/main/assets/licences/` and ship
inside the APK.

| Font | Resource | Copyright | Licence text |
|---|---|---|---|
| Fraunces | `fraunces.ttf` | © 2018 The Fraunces Project Authors | `licences/fraunces.OFL.txt` |
| Inter | `inter.ttf` | © 2020 The Inter Project Authors | `licences/inter.OFL.txt` |
| JetBrains Mono | `jetbrains_mono.ttf` | © 2020 The JetBrains Mono Project Authors | `licences/jetbrains-mono.OFL.txt` |

The copyright lines above are the ones published with each font's licence file. "JetBrains Mono" is a
trademark of JetBrains s.r.o.; the trademark is not licensed with the font software.

Fonts the device itself provides (`sans-serif`, `serif`, `monospace`) are not redistributed by MyDoc.

---

## Removed from this file

Credits that nothing in the released APK or the resolved dependency graph supports, listed so the
removal is on the record rather than silent:

- **ZXing** — MyDoc scans no barcodes; no `zxing` artifact is declared or resolved.
- **docx-preview** and **JSZip** — DOCX is converted by the native streaming parser. There is no
  WebView document renderer and no JavaScript payload; the module has no `assets/` directory of its own.
- A design-attribution credit to an unrelated PDF application — no code, asset or resource from it is
  present in this repository.

---

## Standard licence texts

### Apache License, Version 2.0

```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

### SIL Open Font License, Version 1.1

The authoritative text is bundled with the fonts, in the files named in the table above, and is
published by SIL International at <https://openfontlicense.org/>. It is deliberately not transcribed
here: the copy that ships inside the APK is the one that counts, and a second hand-copied version in
this file would only add a way to be wrong.

### GNU Affero General Public License, Version 3.0

MuPDF is AGPL-licensed and MyDoc is distributed under the same terms; the whole text is in
[`LICENSE`](LICENSE).
