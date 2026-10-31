# Document Engine Architecture & Improvement Plan

> **Project Target:** `mydoc` (`com.abhisheksati.mydoc`)  
> **Inspiration & Attribution:** Architecture and design paradigms adapted with credit to `ClearPDF-5.1` (`com.chethan616.clearpdf`).  
> **Focus:** High-fidelity document loading, visual precision, and memory-safe rendering for PDF, PPTX, and DOCX files.

---

## 1. Executive Summary & Core Architectural Insight

High-fidelity mobile document reading requires a disciplined, memory-safe architecture:

### 🌟 The "Universal PDF Pipeline" Architecture
Rather than creating fragile, independent layout engines for every proprietary document format in Compose:
- **PDF:** Rendered via MuPDF Fitz with sub-pixel vector tiling.
- **DOCX:** Rendered via offline sandboxed `docx-preview` inside an offscreen WebView converted to PDF pages.
- **PPTX:** Parsed via OOXML shape tree, slide master/layout inheritance, and theme color palettes directly into vector PDF pages.
- **XLSX:** Banded column layout rendered directly into PDF pages.

```mermaid
graph TD
    subgraph Input Formats
        PDF[PDF File]
        DOCX[Word .docx / .doc]
        PPTX[PowerPoint .pptx / .ppt]
        XLSX[Excel .xlsx / .csv]
    end

    subgraph Converters
        DOCX_CONV["DocxWebRenderer<br/>(docx-preview in sandboxed WebView + PDF export)"]
        PPTX_CONV["PptxRenderer<br/>(OOXML Shape Tree + Layout/Master Inheritance to PdfDocument)"]
        XLSX_CONV["XlsxPdfRenderer<br/>(Banded Column Layout to PdfDocument)"]
    end

    subgraph Universal Document Engine
        PDF_ENGINE["Unified PDF Engine<br/>• Viewport Tiling (renderRegion)<br/>• Thread-Safe ReentrantLock<br/>• OOM Protection (Throwable Catch & Recycle)<br/>• Continuous Vertical & Horizontal Pager"]
    end

    PDF --> PDF_ENGINE
    DOCX --> DOCX_CONV --> PDF_ENGINE
    PPTX --> PPTX_CONV --> PDF_ENGINE
    XLSX --> XLSX_CONV --> PDF_ENGINE
```

By adopting this architecture, every improvement made to PDF viewing (smooth zoom, search, annotations, page jump, continuous vertical scrolling) automatically benefits DOCX and PPTX files.

---

## 2. Core Rendering Principles

| Dimension | Specification | Architectural Benefit |
| :--- | :--- | :--- |
| **Word (.docx)** | **`DocxWebRenderer`** with offline `docx-preview.js` asset in hardened WebView; slices to PDF pages. | Authentic margins, typography, page breaks, and complex tables rendered accurately without bulky external binaries. |
| **PowerPoint (.pptx)** | **`PptxRenderer`** reads presentation XML, theme palettes (`a:clrScheme`), slide layout & master inheritance, painting directly to `PdfDocument`. | Authentic aspect ratios (`p:sldSz`), proper slide ordering via `<p:sldIdLst>`, and accurate color contrast. |
| **PDF Zoom & Resolution** | **`renderRegion`**: Renders only the active on-screen viewport at native screen DPI while zoomed in. | Zero pixelation at 5x–10x zoom without allocating giant full-page bitmaps. |
| **Thread Safety & OOM** | Native rendering operations protected by `ReentrantLock` with explicit `Throwable` handling and bitmap recycling. | Eliminates native concurrency crashes during fast scrolling or low-memory conditions. |
| **Slide Ordering** | Resolves authoritative order through `<p:sldIdLst>` in `ppt/presentation.xml`. | Guarantees authentic presentation sequence. |

---

## 3. High-DPI PDF Zooming with Viewport Tiling (`renderRegion`)

When zooming in on high-resolution documents, full-page bitmap allocations at high zoom levels quickly cause `OutOfMemoryError`. Instead, the viewport tiling architecture calculates and renders only the visible slice:

```kotlin
override fun renderRegion(
    document: PdfDocument,
    pageIndex: Int,
    region: RectF,         // Normalized 0f..1f bounds of visible viewport
    pageWidthPx: Float,    // Target virtual width of the page
    outWidth: Int,         // Physical screen width in pixels
    outHeight: Int         // Physical screen height in pixels
): Bitmap? = lock.withLock {
    val renderer = renderers[document.uri] ?: return null
    val page = renderer.openPage(pageIndex)
    var bitmap: Bitmap? = null
    try {
        val srcW = page.width.toFloat().coerceAtLeast(1f)
        val s = pageWidthPx / srcW
        bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        val matrix = Matrix().apply {
            postScale(s, s)
            postTranslate(-region.left * pageWidthPx, -region.top * (page.height * s))
        }
        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    } catch (t: Throwable) {
        bitmap?.recycle()
        null
    } finally {
        runCatching { page.close() }
    }
}
```

---

## 4. Architectural Roadmap

| Phase | Milestone | Key Deliverables |
| :--- | :--- | :--- |
| **Phase 1** | **PDF Core Hardening** | `ReentrantLock` concurrency, `Throwable` OOM handling, and viewport `renderRegion`. |
| **Phase 2** | **PPTX Engine Upgrade** | OOXML parsing, master/layout placeholder inheritance, theme color mapping, and vector PDF conversion. |
| **Phase 3** | **DOCX Web Rendering** | Offline sandboxed `docx-preview` asset, PDF export, and twips reflow fallback. |
| **Phase 4** | **Unified Universal Viewer** | Route DOCX & PPTX to Master Viewer with pinch-to-zoom, search, and reading continuity. |

### Component Paths
- PDF Engine: `app/src/main/java/com/abhisheksati/mydoc/rendering/engine/`
- Viewer UI: `app/src/main/java/com/abhisheksati/mydoc/ui/viewer/pdf/PdfViewerScreen.kt`
- PPTX Parser: `app/src/main/java/com/abhisheksati/mydoc/rendering/engine/presentation/`
- DOCX Web Renderer: `app/src/main/java/com/abhisheksati/mydoc/rendering/engine/word/DocxWebRenderer.kt`
- ViewModel Layer: `app/src/main/java/com/abhisheksati/mydoc/viewmodel/ViewerViewModel.kt`

---

## 5. Verification & Quality Guardrails

1. **Zero Telemetry & Offline Processing:** All document operations run strictly on-device; no cloud dependencies.
2. **Pure AMOLED Aesthetics:** Maintain `#000000` AMOLED Black and clean high-contrast themes.
3. **Mandatory Build Checks:**
   - `./gradlew testDebugUnitTest` (100% green)
   - `./gradlew lintDebug` (0 errors)
   - `./gradlew assembleDebug` (Clean compilation)
