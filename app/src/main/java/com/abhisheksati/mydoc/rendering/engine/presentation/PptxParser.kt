package com.abhisheksati.mydoc.rendering.engine.presentation

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class PptxSlide(
    val slideNumber: Int,
    val title: String,
    val content: List<String>
)

data class PptxPresentation(
    val slides: List<PptxSlide>
)

class PptxParser {

    suspend fun parse(inputStream: InputStream): PptxPresentation = withContext(Dispatchers.IO) {
        val bytes = inputStream.readBytes()
        val slidesMap = mutableMapOf<Int, PptxSlide>()

        val zip = ZipInputStream(ByteArrayInputStream(bytes))
        var entry: ZipEntry? = zip.nextEntry

        while (entry != null) {
            val name = entry.name
            if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                val numStr = name.removePrefix("ppt/slides/slide").removeSuffix(".xml")
                val slideNum = numStr.toIntOrNull() ?: 1
                val slide = parseSlideXml(zip, slideNum)
                slidesMap[slideNum] = slide
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        val sortedSlides = slidesMap.toSortedMap().values.toList()
        if (sortedSlides.isEmpty()) {
            PptxPresentation(listOf(PptxSlide(1, "Empty Presentation", listOf("No slides found in presentation."))))
        } else {
            PptxPresentation(sortedSlides)
        }
    }

    private fun parseSlideXml(stream: InputStream, slideNum: Int): PptxSlide {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(stream, "UTF-8")
        }

        val paragraphs = mutableListOf<String>()
        val currentPara = StringBuilder()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == "a:p") {
                        currentPara.clear()
                    } else if (name == "a:t") {
                        val text = parser.nextText()
                        if (text.isNotEmpty()) {
                            currentPara.append(text)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name == "a:p") {
                        val trimmed = currentPara.toString().trim()
                        if (trimmed.isNotEmpty()) {
                            paragraphs.add(trimmed)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        val title = paragraphs.firstOrNull() ?: "Slide $slideNum"
        val content = if (paragraphs.size > 1) paragraphs.drop(1) else emptyList()

        return PptxSlide(
            slideNumber = slideNum,
            title = title,
            content = content
        )
    }
}
