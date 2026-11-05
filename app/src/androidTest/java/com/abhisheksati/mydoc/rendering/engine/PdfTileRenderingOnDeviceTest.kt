package com.abhisheksati.mydoc.rendering.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhisheksati.mydoc.domain.model.ReadingMode
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * H-5 asks whether a zoom tile is genuinely rendered as a region or whether the engine rasterizes a
 * whole high-resolution page and crops it afterwards. Each quadrant of the fixture page is filled
 * with a different grey, so a tile's average colour identifies which part of the page it came from.
 */
@RunWith(AndroidJUnit4::class)
class PdfTileRenderingOnDeviceTest {

    private lateinit var context: Context
    private lateinit var file: File

    // 255 = white, 160, 80 and 20 darken clockwise from the top-left.
    private val topLeft = 160
    private val topRight = 20
    private val bottomLeft = 80
    private val bottomRight = 200

    private fun grey(v: Int) = Color.rgb(v, v, v)

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.getExternalFilesDir(null), "tile-probe.pdf").apply { delete() }

        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(200, 200, 1).create())
        val canvas = page.canvas
        val half = 100f
        val paint = Paint()
        paint.color = grey(topLeft); canvas.drawRect(0f, 0f, half, half, paint)
        paint.color = grey(topRight); canvas.drawRect(half, 0f, 200f, half, paint)
        paint.color = grey(bottomLeft); canvas.drawRect(0f, half, half, 200f, paint)
        paint.color = grey(bottomRight); canvas.drawRect(half, half, 200f, 200f, paint)
        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    private fun renderer(): MuPdfRenderer {
        val renderer = MuPdfRenderer(filePath = file.absolutePath)
        assertTrue("probe document did not open", renderer.pageCount >= 1)
        return renderer
    }

    private fun meanOf(bitmap: Bitmap): Int {
        val px = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var sum = 0L
        for (p in px) sum += (p shr 16) and 0xFF
        return (sum / px.size).toInt()
    }

    @Test
    fun aTileIsTheRequestedSize() {
        val renderer = renderer()
        val tile = renderer.renderRegion(
            pageIndex = 0,
            region = RectF(0f, 0f, 0.5f, 0.5f),
            pageWidthPx = 400f,
            outWidth = 200,
            outHeight = 200,
            mode = ReadingMode.NORMAL
        )
        requireNotNull(tile)
        assertEquals(200, tile.width)
        assertEquals(200, tile.height)
        tile.recycle()
        renderer.close()
    }

    @Test
    fun theTopLeftTileShowsTheTopLeftOfThePage() {
        val renderer = renderer()
        val tile = requireNotNull(
            renderer.renderRegion(
                pageIndex = 0,
                region = RectF(0f, 0f, 0.5f, 0.5f),
                pageWidthPx = 400f,
                outWidth = 200,
                outHeight = 200,
                mode = ReadingMode.NORMAL
            )
        )
        val mean = meanOf(tile)
        tile.recycle()
        renderer.close()

        val wholePageMean = (topLeft + topRight + bottomLeft + bottomRight) / 4
        assertTrue(
            "top-left tile averaged $mean; the top-left quadrant is $topLeft, the whole page " +
                "would average $wholePageMean, and the top-right is $topRight",
            kotlin.math.abs(mean - topLeft) <= 8
        )
    }

    @Test
    fun theBottomRightTileShowsTheBottomRightOfThePage() {
        val renderer = renderer()
        val tile = requireNotNull(
            renderer.renderRegion(
                pageIndex = 0,
                region = RectF(0.5f, 0.5f, 1f, 1f),
                pageWidthPx = 400f,
                outWidth = 200,
                outHeight = 200,
                mode = ReadingMode.NORMAL
            )
        )
        val mean = meanOf(tile)
        tile.recycle()
        renderer.close()

        assertTrue(
            "bottom-right tile averaged $mean; that quadrant is $bottomRight",
            kotlin.math.abs(mean - bottomRight) <= 8
        )
    }
}
