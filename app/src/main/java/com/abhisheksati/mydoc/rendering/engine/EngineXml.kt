package com.abhisheksati.mydoc.rendering.engine

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Creates the pull parser every Office engine reads XML with.
 *
 * Engines previously each wrote their own "platform parser, else JVM parser" try/catch. That
 * duplicated a subtlety: under `unitTests.isReturnDefaultValues` the platform parser returns
 * null instead of throwing, so a try/catch around the call alone never reaches the fallback and
 * the caller dies on a null receiver. Here the result is checked, not just the exception.
 *
 * Namespace processing stays off, matching how the parsers match qualified names like `x:row`
 * by local name.
 */
object EngineXml {

    fun newPullParser(): XmlPullParser {
        val platform = try {
            Xml.newPullParser()
        } catch (_: Throwable) {
            null
        }
        if (platform != null) {
            runCatching { platform.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false) }
            return platform
        }
        return XmlPullParserFactory.newInstance()
            .apply { isNamespaceAware = false }
            .newPullParser()
    }

    fun newPullParser(input: java.io.InputStream): XmlPullParser =
        newPullParser().apply { setInput(input, "UTF-8") }
}
