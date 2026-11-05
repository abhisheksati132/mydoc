package com.abhisheksati.mydoc.rendering.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxRenderer
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
 * A chart on a slide is a part of its own, reached through the slide's relationships. Whether the
 * renderer reads that part and whether the painter puts ink on the page is only visible once the
 * package has been rendered and rasterised - so that is what these tests do, and the same deck
 * without its chart part is the control that says where the ink came from.
 */
@RunWith(AndroidJUnit4::class)
class ChartSlideOnDeviceTest {

    private lateinit var context: Context
    private lateinit var dir: File

    private val a = "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""
    private val r = "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""
    private val p = "xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\""
    private val c = "xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\""

    // The painter's own sequence: first series blue, second orange.
    private val first = Triple(0x4E, 0x79, 0xC7)
    private val second = Triple(0xED, 0x7D, 0x31)

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "chart-slides").apply {
            if (exists()) walkBottomUp().forEach { it.delete() }
            mkdirs()
        }
    }

    @Test
    fun aBarChartReachableFromTheSlideIsPaintedWithItsSeriesColours() {
        val pixels = raster(PptxRenderer.render(deck(barChart()))!!)
        val blue = count(pixels, first)
        val orange = count(pixels, second)
        assertTrue(
            "the chart part was not drawn (blue=$blue orange=$orange): the slide's chart reference " +
                "or the chart part itself never reached the painter",
            blue > 400 && orange > 400
        )
    }

    @Test
    fun theSameSlideWithoutItsChartPartStaysEmpty() {
        // Control for the test above: nothing else on this deck uses those two colours, so any ink
        // of theirs can only have come from the chart part.
        val pixels = raster(PptxRenderer.render(deck(chartPart = null))!!)
        assertTrue(
            "the blank deck already contains the series colours (blue=${count(pixels, first)} " +
                "orange=${count(pixels, second)})",
            count(pixels, first) < 5 && count(pixels, second) < 5
        )
    }

    @Test
    fun aPieChartPaintsOneSlicePerCategory() {
        val pixels = raster(PptxRenderer.render(deck(pieChart()))!!)
        val blue = count(pixels, first)
        val orange = count(pixels, second)
        assertTrue(
            "the pie was not drawn from its cached values (blue=$blue orange=$orange)",
            blue > 400 && orange > 400
        )
    }

    private fun count(pixels: IntArray, rgb: Triple<Int, Int, Int>): Int {
        var hits = 0
        for (pixel in pixels) {
            if (Color.alpha(pixel) < 200) continue
            if (abs(Color.red(pixel) - rgb.first) <= 12 &&
                abs(Color.green(pixel) - rgb.second) <= 12 &&
                abs(Color.blue(pixel) - rgb.third) <= 12
            ) hits++
        }
        return hits
    }

    private fun abs(v: Int): Int = if (v < 0) -v else v

    private fun raster(doc: PdfDocument): IntArray {
        val pdf = File(dir, "page.pdf")
        FileOutputStream(pdf).use { doc.writeTo(it) }
        doc.close()
        val descriptor = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        return try {
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, Rect(0, 0, bitmap.width, bitmap.height), null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            IntArray(bitmap.width * bitmap.height).also {
                bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            }.also { bitmap.recycle() }
        } finally {
            renderer.close()
            descriptor.close()
        }
    }

    private fun deck(chartPart: String?): ByteArray {
        val frame = """
            <p:graphicFrame>
              <p:nvGraphicFramePr><p:cNvPr id="4" name="Chart"/><p:cNvGraphicFramePr/><p:nvPr/></p:nvGraphicFramePr>
              <p:xfrm><a:off x="609600" y="609600"/><a:ext cx="7924800" cy="5029200"/></p:xfrm>
              <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/chart">
                <c:chart $c r:id="rId2"/>
              </a:graphicData>
            </p:graphicFrame>
        """.trimIndent()
        val rels = if (chartPart == null) "" else
            """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart" Target="../charts/chart1.xml"/>"""
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
                    "<p:sldSz cx=\"12192000\" cy=\"6858000\"/></p:presentation>"
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
            chartPart?.let { put("ppt/charts/chart1.xml", it) }
        }
        return out.toByteArray()
    }

    private fun barChart() = """
        <?xml version="1.0"?><c:chartSpace $c $a $r><c:chart><c:title><a:tx><a:rich><a:p><a:r><a:t>Units</a:t></a:r></a:p></a:rich></a:tx></c:title>
        <c:plotArea><c:barChart><c:barDir val="col"/>
          <c:ser><c:idx val="0"/><c:tx><c:strRef><c:strCache><c:ptCount val="1"/><c:pt idx="0"><c:v>North</c:v></c:pt></c:strCache></c:strRef></c:tx>
            <c:cat><c:strRef><c:strCache><c:ptCount val="3"/><c:pt idx="0"><c:v>Q1</c:v></c:pt><c:pt idx="1"><c:v>Q2</c:v></c:pt><c:pt idx="2"><c:v>Q3</c:v></c:pt></c:strCache></c:strRef></c:cat>
            <c:val><c:numRef><c:numCache><c:ptCount val="3"/><c:pt idx="0"><c:v>120</c:v></c:pt><c:pt idx="1"><c:v>150</c:v></c:pt><c:pt idx="2"><c:v>180</c:v></c:pt></c:numCache></c:numRef></c:val></c:ser>
          <c:ser><c:idx val="1"/><c:tx><c:strRef><c:strCache><c:ptCount val="1"/><c:pt idx="0"><c:v>South</c:v></c:pt></c:strCache></c:strRef></c:tx>
            <c:val><c:numRef><c:numCache><c:ptCount val="3"/><c:pt idx="0"><c:v>90</c:v></c:pt><c:pt idx="1"><c:v>110</c:v></c:pt><c:pt idx="2"><c:v>140</c:v></c:pt></c:numCache></c:numRef></c:val></c:ser>
        </c:barChart></c:plotArea></c:chart></c:chartSpace>
    """.trimIndent()

    private fun pieChart() = """
        <?xml version="1.0"?><c:chartSpace $c $a $r><c:chart><c:plotArea><c:pieChart><c:firstSliceAng val="0"/>
          <c:ser><c:idx val="0"/>
            <c:cat><c:strRef><c:strCache><c:ptCount val="2"/><c:pt idx="0"><c:v>Desktop</c:v></c:pt><c:pt idx="1"><c:v>Mobile</c:v></c:pt></c:strCache></c:strRef></c:cat>
            <c:val><c:numRef><c:numCache><c:ptCount val="2"/><c:pt idx="0"><c:v>45</c:v></c:pt><c:pt idx="1"><c:v>40</c:v></c:pt></c:numCache></c:numRef></c:val></c:ser>
        </c:pieChart></c:plotArea></c:chart></c:chartSpace>
    """.trimIndent()
}
