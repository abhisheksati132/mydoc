# LibreOffice Office engine

MyDoc integrates LibreOfficeKit **26.2.6.3**, copyright LibreOffice contributors and The Document Foundation. MyDoc is an independent application; no affiliation or endorsement is implied.

## Provenance and source

- Unmodified native libraries and runtime assets come from F-Droid's LibreOffice Viewer packages 129 (x86_64), 130 (armeabi-v7a), and 131 (arm64-v8a).
- [Package and source downloads](https://f-droid.org/en/packages/org.documentfoundation.libreoffice/), including the source archive beside each binary package.
- [Exact upstream source tag](https://github.com/LibreOffice/core/tree/libreoffice-26.2.6.3).
- [F-Droid build recipes and patches](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/org.documentfoundation.libreoffice.yml).
- `engine.lock.json` pins each binary's SHA-256 and the F-Droid signing certificate. `scripts/prepare_word_engine.py` verifies both before extracting runtime files. The upstream app's UI, classes, resources and identity are not included.

The three Java adapters under `office-engine/src/main/java/org/libreoffice/kit` derive from upstream `android/Bootstrap/src/org/libreoffice/kit` at this tag. MyDoc changes: initialize using a service Context/private profile, disable stdout redirection, and make callback references volatile. Their MPL headers remain intact; the complete modified source is in this repository.

## Licenses

`COPYING.MPL`, `COPYING.LGPL`, `COPYING.GPL`, `NOTICE`, and `license.xml` are verbatim upstream files. The last contains the full upstream third-party copyright and license collection, including optional components; its presence does not mean MyDoc includes every listed component. `license_plain_text.xsl` is upstream's accompanying transform. Copies of the license texts, NOTICE and complete collection (as `LICENSE.html`) are also packaged under `assets/licences/libreoffice` for offline access. The collection includes the bundled font notices and license terms.

Preserve these notices and corresponding-source links when redistributing the engine. Native-library modifications must include matching source. MyDoc's own integration and worker guard are covered by this repository's license.
