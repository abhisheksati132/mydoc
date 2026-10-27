package com.abhisheksati.mydoc.rendering.engine.presentation

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Makes inherited text formatting explicit on "orphan" placeholders in PPTX slides.
 * Decks pasted between templates often keep placeholders the layout no longer has
 * (e.g. idx="4294967295"). This copies master anchor, insets, wrap, and font levels
 * directly onto slide XML so titles and body placeholders aren't truncated or misplaced.
 */
object PptxPlaceholderFix {

    private const val P = "http://schemas.openxmlformats.org/presentationml/2006/main"
    private const val A = "http://schemas.openxmlformats.org/drawingml/2006/main"
    private const val R = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val NoIndex = "4294967295"

    fun apply(file: File): Boolean {
        val patched = runCatching { patch(file) }.getOrNull() ?: return false
        if (patched.isEmpty()) return false
        val tmp = File(file.parentFile, file.name + ".fix")
        return runCatching {
            ZipFile(file).use { zip ->
                ZipOutputStream(tmp.outputStream().buffered()).use { out ->
                    for (entry in zip.entries()) {
                        out.putNextEntry(ZipEntry(entry.name))
                        val replacement = patched[entry.name]
                        if (replacement != null) {
                            out.write(replacement)
                        } else {
                            zip.getInputStream(entry).use { it.copyTo(out) }
                        }
                        out.closeEntry()
                    }
                }
            }
            file.delete() && tmp.renameTo(file)
        }.getOrElse { tmp.delete(); false }
    }

    private fun patch(file: File): Map<String, ByteArray> = ZipFile(file).use { zip ->
        fun read(path: String): ByteArray? = zip.getEntry(path)?.let { e -> zip.getInputStream(e).use { it.readBytes() } }
        val docs = HashMap<String, Document?>()
        fun dom(path: String): Document? = docs.getOrPut(path) { read(path)?.let { parse(it) } }
        fun relTargets(path: String): List<String> {
            val dir = path.substringBeforeLast('/', "")
            val rels = dom("$dir/_rels/${path.substringAfterLast('/')}.rels") ?: return emptyList()
            return rels.getElementsByTagNameNS(R, "Relationship").elements().mapNotNull { it.getAttribute("Target").takeIf { t -> t.isNotEmpty() } }
                .map { resolve(dir, it) }
        }

        val out = HashMap<String, ByteArray>()
        val slides = zip.entries().toList().map { it.name }
            .filter { it.startsWith("ppt/slides/slide") && it.endsWith(".xml") }
        for (slidePath in slides) {
            val layoutPath = relTargets(slidePath).firstOrNull { it.startsWith("ppt/slideLayouts/") } ?: continue
            val masterPath = relTargets(layoutPath).firstOrNull { it.startsWith("ppt/slideMasters/") } ?: continue
            val slide = dom(slidePath) ?: continue
            val layout = dom(layoutPath) ?: continue
            val master = dom(masterPath) ?: continue
            var changed = false
            for (sp in slide.getElementsByTagNameNS(P, "sp").elements()) {
                val ph = sp.firstDescendant(P, "ph") ?: continue
                val type = ph.getAttribute("type").ifEmpty { "obj" }
                val idx = ph.getAttribute("idx").takeIf { it.isNotEmpty() && it != NoIndex }
                if (findPlaceholder(layout, type, idx, byTypeOnly = false) != null) continue
                val masterType = when (type) {
                    "title", "ctrTitle" -> "title"
                    "body", "obj", "subTitle" -> "body"
                    else -> type
                }
                val masterSp = findPlaceholder(master, masterType, null, byTypeOnly = true) ?: continue
                if (inherit(slide, sp, masterSp, master, masterType)) changed = true
            }
            if (changed) out[slidePath] = serialize(slide)
        }
        out
    }

    private fun findPlaceholder(part: Document, type: String, idx: String?, byTypeOnly: Boolean): Element? {
        val shapes = part.getElementsByTagNameNS(P, "sp").elements()
        fun phOf(sp: Element) = sp.firstDescendant(P, "ph")
        if (!byTypeOnly && idx != null) {
            shapes.firstOrNull { phOf(it)?.getAttribute("idx") == idx }?.let { return it }
        }
        val wanted = if (type == "ctrTitle") setOf("ctrTitle", "title") else if (type == "title") setOf("title", "ctrTitle") else setOf(type)
        return shapes.firstOrNull { sp ->
            val ph = phOf(sp) ?: return@firstOrNull false
            ph.getAttribute("type").ifEmpty { "obj" } in wanted && (byTypeOnly || idx == null)
        }
    }

    private fun inherit(slide: Document, sp: Element, masterSp: Element, master: Document, masterType: String): Boolean {
        var changed = false
        val txBody = sp.firstChild(P, "txBody") ?: return false
        val masterBody = masterSp.firstChild(P, "txBody")

        val bodyPr = txBody.firstChild(A, "bodyPr") ?: slide.createElementNS(A, "a:bodyPr").also {
            txBody.insertBefore(it, txBody.firstChild)
        }
        masterBody?.firstChild(A, "bodyPr")?.let { mb ->
            val attrs = mb.attributes
            for (i in 0 until attrs.length) {
                val a = attrs.item(i)
                if (a.nodeName.startsWith("xmlns")) continue
                if (!bodyPr.hasAttribute(a.nodeName)) {
                    bodyPr.setAttribute(a.nodeName, a.nodeValue)
                    changed = true
                }
            }
        }

        val txStyles = master.getElementsByTagNameNS(P, "txStyles").elements().firstOrNull()
        val styleName = when (masterType) {
            "title" -> "titleStyle"
            "body" -> "bodyStyle"
            else -> "otherStyle"
        }
        val base = txStyles?.firstChild(P, styleName)
        val masterLst = masterBody?.firstChild(A, "lstStyle")
        val lst = txBody.firstChild(A, "lstStyle") ?: slide.createElementNS(A, "a:lstStyle").also {
            txBody.insertBefore(it, bodyPr.nextSibling)
        }
        for (level in 1..9) {
            val name = "lvl${level}pPr"
            if (lst.firstChild(A, name) != null) continue
            val source = masterLst?.firstChild(A, name) ?: base?.firstChild(A, name) ?: continue
            val copy = slide.importNode(source, true) as Element
            val after = lst.childElements().firstOrNull { el ->
                val n = el.localName ?: ""
                n == "extLst" || (n.startsWith("lvl") && (n.removePrefix("lvl").removeSuffix("pPr").toIntOrNull() ?: 0) > level)
            }
            lst.insertBefore(copy, after)
            changed = true
        }

        val spPr = sp.firstChild(P, "spPr")
        if (spPr != null && spPr.firstChild(A, "xfrm") == null) {
            masterSp.firstChild(P, "spPr")?.firstChild(A, "xfrm")?.let {
                spPr.insertBefore(slide.importNode(it, true), spPr.firstChild)
                changed = true
            }
        }
        return changed
    }

    private fun parse(bytes: ByteArray): Document? = runCatching {
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(bytes.inputStream())
    }.getOrNull()

    private fun serialize(doc: Document): ByteArray {
        val out = ByteArrayOutputStream()
        TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.STANDALONE, "yes")
        }.transform(DOMSource(doc), StreamResult(out))
        return out.toByteArray()
    }

    private fun resolve(baseDir: String, target: String): String {
        if (target.startsWith("/")) return target.removePrefix("/")
        val stack = ArrayDeque(baseDir.split('/').filter { it.isNotEmpty() })
        for (segment in target.split('/')) when (segment) {
            "", "." -> Unit
            ".." -> stack.removeLastOrNull()
            else -> stack.addLast(segment)
        }
        return stack.joinToString("/")
    }

    private fun org.w3c.dom.NodeList.elements(): List<Element> =
        (0 until length).mapNotNull { item(it) as? Element }

    private fun Element.childElements(): List<Element> =
        (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }

    private fun Element.firstChild(ns: String, local: String): Element? =
        childElements().firstOrNull { it.namespaceURI == ns && it.localName == local }

    private fun Element.firstDescendant(ns: String, local: String): Element? {
        val list = getElementsByTagNameNS(ns, local)
        return if (list.length > 0) list.item(0) as? Element else null
    }
}
