package com.abhisheksati.mydoc.rendering.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxConverter
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxRenderer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * A SmartArt graphic keeps its words in a diagram data part of its own, and the shapes come from a
 * gallery the package does not carry. These tests render a deck that holds a diagram, then read the
 * text layer of the page back out, so what is checked is the words a reader can actually reach.
 *
 * The diagram XML follows ECMA-376 Part 1's `dgm:dataModel`; no Office-authored SmartArt exists on
 * the machine these fixtures were made on (the SmartArt gallery is not installed), so this is the
 * shape the standard describes rather than a file PowerPoint wrote.
 */
@RunWith(AndroidJUnit4::class)
class SmartArtSlideOnDeviceTest {

    private lateinit var context: Context
    private lateinit var dir: File

    private val a = "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""
    private val r = "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""
    private val p = "xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\""
    private val dgm = "xmlns:dgm=\"http://schemas.openxmlformats.org/drawingml/2006/diagram\""

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "smartart-slides").apply {
            if (exists()) walkBottomUp().forEach { it.delete() }
            mkdirs()
        }
    }

    @Test
    fun theWordsASmartArtGraphicHoldsReachThePage() {
        val text = pageText(deck(dataModel()))
        assertTrue("the diagram's words never reached the page: $text", text.contains("Gather"))
        assertTrue("a process node's text was dropped: $text", text.contains("Analyse"))
        assertTrue("the last node's text was dropped: $text", text.contains("Report"))
        assertTrue(
            "a node with only the adjacency variant was not read: $text",
            text.contains("Reconcile")
        )
    }

    @Test
    fun theNodesAreReadInTheOrderTheModelListsThem() {
        val text = pageText(deck(dataModel())).replace('\n', ' ')
        val first = text.indexOf("Gather")
        val second = text.indexOf("Analyse")
        val third = text.indexOf("Report")
        assertTrue("the nodes came out of order: $text", first in 0 until second && second < third)
    }

    @Test
    fun aDiagramWithoutItsDataPartContributesNothing() {
        val text = pageText(deck(diagramPart = null))
        listOf("Gather", "Analyse", "Report", "Reconcile").forEach { word ->
            assertFalse("the blank deck somehow says $word", text.contains(word))
        }
    }

    @Test
    fun theWordsReachThePdfTheViewerActuallyOpens() {
        // The viewer does not call the renderer: it opens the PDF the converter cached. Converting
        // through that entry point is what a reader sees, so that is the path checked here.
        val pptx = File(dir, "smartart.pptx")
        pptx.writeBytes(deck(dataModel()))
        val converted = PptxConverter.convertPptxToPdf(context, Uri.fromFile(pptx), pptx.name)
        val pdf = (converted as OfficeConversion.Success).pdf
        val renderer = MuPdfRenderer(filePath = pdf.absolutePath)
        val extracted = try { renderer.getDocumentText() } finally { renderer.close() }
        val text = extracted.orEmpty()
        listOf("Gather", "Analyse", "Reconcile", "Report")
            .forEach { line ->
                assertTrue("the converted page is missing [" + line + "]: " + text, text.contains(line))
            }
    }

    @Test
    fun theRecoveredWordsAreInkOnThePageAndNotOnlyATextLayer() {
        // Extraction can read a text layer a reader never sees, and the viewer opens the PDF the
        // converter wrote rather than the renderer's document. So the file the reader would be shown
        // is rasterised and its ink counted: the words have to be drawn, not merely present.
        val pptx = File(dir, "smartart-ink.pptx")
        pptx.writeBytes(deck(dataModel()))
        val converted = PptxConverter.convertPptxToPdf(context, Uri.fromFile(pptx), pptx.name)
        val pdf = (converted as OfficeConversion.Success).pdf
        val ink = darkPixels(pdf)
        assertTrue("the page the viewer opens carries no ink for the recovered words ($ink dark pixels)", ink > 400)
    }

    private fun darkPixels(pdf: File): Int {
        val descriptor = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        return try {
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, Rect(0, 0, bitmap.width, bitmap.height), null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            bitmap.recycle()
            pixels.count { Color.alpha(it) > 200 && (Color.red(it) + Color.green(it) + Color.blue(it)) / 3 < 180 }
        } finally {
            renderer.close()
            descriptor.close()
        }
    }

    private fun pageText(bytes: ByteArray): String {
        val doc: PdfDocument = PptxRenderer.render(bytes) ?: error("the deck did not render")
        val pdf = File(dir, "smartart.pdf")
        FileOutputStream(pdf).use { doc.writeTo(it) }
        doc.close()
        val renderer = MuPdfRenderer(filePath = pdf.absolutePath)
        val extracted = try { renderer.getDocumentText() } finally { renderer.close() }
        return extracted.orEmpty()
    }

    private fun dataModel(): String = """
        <?xml version="1.0"?><dgm:dataModel $dgm $a>
          <dgm:ptLst>
            <dgm:pt modelId="dgmDoc" type="doc"><dgm:prSet/><dgm:extLst><a:ext/></dgm:extLst></dgm:pt>
            <dgm:pt modelId="n0"><dgm:prSet phldr="0"/>
              <dgm:textVariants>
                <dgm:txVar var="def"><a:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:t>Gather</a:t></a:r></a:p></a:txBody></dgm:txVar>
                <dgm:txVar var="adj"><a:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:t>Gather (short)</a:t></a:r></a:p></a:txBody></dgm:txVar>
              </dgm:textVariants>
            </dgm:pt>
            <dgm:pt modelId="n1" type="proc"><dgm:prSet/>
              <dgm:textVariants><dgm:txVar var="def"><a:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:t>Analyse</a:t></a:r></a:p></a:txBody></dgm:txVar></dgm:textVariants>
            </dgm:pt>
            <dgm:pt modelId="n2"><dgm:prSet/>
              <dgm:textVariants><dgm:txVar var="def"><a:txBody><a:bodyPr/><a:lstStyle/><a:p><a:endParaRPr/></a:p></a:txBody></dgm:txVar></dgm:textVariants>
            </dgm:pt>
            <dgm:pt modelId="n3"><dgm:prSet/>
              <dgm:textVariants><dgm:txVar var="adj"><a:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:t>Reconcile</a:t></a:r></a:p></a:txBody></dgm:txVar></dgm:textVariants>
            </dgm:pt>
            <dgm:pt modelId="n4"><dgm:prSet/>
              <dgm:textVariants><dgm:txVar var="def"><a:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:t>Report</a:t></a:r></a:p></a:txBody></dgm:txVar></dgm:textVariants>
            </dgm:pt>
          </dgm:ptLst>
          <dgm:cxnLst>
            <dgm:cxn modelId="c0" type="parTrans" srcId="dgmDoc" destId="n0" parSeg="0" chSeg="0"/>
          </dgm:cxnLst>
          <dgm:bg/><dgm:whole/>
        </dgm:dataModel>
    """.trimIndent()

    private fun deck(diagramPart: String?): ByteArray {
        val frame = """
            <p:graphicFrame>
              <p:nvGraphicFramePr><p:cNvPr id="6" name="SmartArt"/><p:cNvGraphicFramePr/><p:nvPr/></p:nvGraphicFramePr>
              <p:xfrm><a:off x="457200" y="457200"/><a:ext cx="6858000" cy="4572000"/></p:xfrm>
              <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/diagram">
                <dgm:relIds $dgm r:dm="rId3" r:lo="rId4" r:qs="rId5" r:cs="rId6"/>
              </a:graphicData>
            </p:graphicFrame>
        """.trimIndent()
        val rels = if (diagramPart == null) "" else
            "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/diagramData\" Target=\"../diagrams/data1.xml\"/>"
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put(
                "ppt/presentation.xml",
                "<?xml version=\"1.0\"?><p:presentation $a $r $p>" +
                    "<p:sldIdLst><p:sldId id=\"256\" r:id=\"rId1\"/></p:sldIdLst>" +
                    "<p:sldSz cx=\"9144000\" cy=\"6858000\"/></p:presentation>"
            )
            put(
                "ppt/_rels/presentation.xml.rels",
                "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide\" " +
                    "Target=\"slides/slide1.xml\"/></Relationships>"
            )
            put(
                "ppt/slides/slide1.xml",
                "<?xml version=\"1.0\"?><p:sld $a $r $p><p:cSld><p:spTree>" +
                    "<p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>" +
                    "<p:grpSpPr/>$frame</p:spTree></p:cSld></p:sld>"
            )
            put(
                "ppt/slides/_rels/slide1.xml.rels",
                "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">$rels</Relationships>"
            )
            diagramPart?.let { put("ppt/diagrams/data1.xml", it) }
        }
        return out.toByteArray()
    }
}
