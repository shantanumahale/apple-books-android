package com.applebooks.android.reader.epub

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class EpubBook(
    val title: String,
    val author: String?,
    val chapters: List<EpubChapter>,
    val tableOfContents: List<TocEntry>,
    val resources: Map<String, ByteArray>,
    val basePath: String
)

data class EpubChapter(
    val id: String,
    val title: String,
    val href: String,
    val htmlContent: String
)

data class TocEntry(
    val title: String,
    val href: String
)

private data class ManifestItem(
    val id: String,
    val href: String,
    val mediaType: String
)

class EpubParser(private val contentResolver: ContentResolver) {

    fun parse(uri: Uri): EpubBook {
        val zipEntries = readZipEntries(uri)

        val containerXml = zipEntries["META-INF/container.xml"]
            ?: throw EpubParseException("Missing META-INF/container.xml")

        val opfPath = parseContainerXml(String(containerXml, Charsets.UTF_8))
        val basePath = opfPath.substringBeforeLast('/', "")

        val opfContent = zipEntries[opfPath]
            ?: throw EpubParseException("Missing OPF file: $opfPath")

        val opfData = parseOpf(String(opfContent, Charsets.UTF_8))

        val resources = mutableMapOf<String, ByteArray>()
        for ((path, bytes) in zipEntries) {
            if (path != opfPath && !path.startsWith("META-INF/")) {
                resources[path] = bytes
            }
        }

        val chapters = buildChapters(
            spineIds = opfData.spineIds,
            manifest = opfData.manifest,
            basePath = basePath,
            zipEntries = zipEntries,
            resources = resources,
            tocEntries = emptyList()
        )

        val tableOfContents = parseToc(opfData, basePath, zipEntries)

        val chaptersWithTitles = assignChapterTitles(chapters, tableOfContents)

        return EpubBook(
            title = opfData.title ?: "Untitled",
            author = opfData.author,
            chapters = chaptersWithTitles,
            tableOfContents = tableOfContents,
            resources = resources,
            basePath = basePath
        )
    }

    private fun readZipEntries(uri: Uri): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw EpubParseException("Cannot open EPUB file")

        inputStream.use { stream ->
            val zipStream = ZipInputStream(stream)
            var entry: ZipEntry? = zipStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zipStream.readBytes()
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }
        return entries
    }

    private fun parseContainerXml(xml: String): String {
        val parser = createXmlParser(xml)
        var opfPath = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                val fullPath = parser.getAttributeValue(null, "full-path")
                if (fullPath != null) {
                    opfPath = fullPath
                    break
                }
            }
            parser.next()
        }

        if (opfPath.isEmpty()) {
            throw EpubParseException("Cannot find rootfile in container.xml")
        }
        return opfPath
    }

    private data class OpfData(
        val title: String?,
        val author: String?,
        val manifest: Map<String, ManifestItem>,
        val spineIds: List<String>,
        val tocId: String?
    )

    private fun parseOpf(xml: String): OpfData {
        val parser = createXmlParser(xml)
        var title: String? = null
        var author: String? = null
        val manifest = mutableMapOf<String, ManifestItem>()
        val spineIds = mutableListOf<String>()
        var tocId: String? = null

        var inMetadata = false
        var inManifest = false
        var inSpine = false
        var currentTag = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name
                    when {
                        tagName == "metadata" || tagName.endsWith(":metadata") -> inMetadata = true
                        tagName == "manifest" -> inManifest = true
                        tagName == "spine" -> {
                            inSpine = true
                            tocId = parser.getAttributeValue(null, "toc")
                        }
                        inMetadata && (tagName == "title" || tagName.endsWith(":title")) -> {
                            currentTag = "title"
                        }
                        inMetadata && (tagName == "creator" || tagName.endsWith(":creator")) -> {
                            currentTag = "creator"
                        }
                        inManifest && tagName == "item" -> {
                            val id = parser.getAttributeValue(null, "id") ?: ""
                            val href = parser.getAttributeValue(null, "href") ?: ""
                            val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
                            if (id.isNotEmpty()) {
                                manifest[id] = ManifestItem(id, href, mediaType)
                            }
                        }
                        inSpine && tagName == "itemref" -> {
                            val idref = parser.getAttributeValue(null, "idref")
                            if (idref != null) {
                                spineIds.add(idref)
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        when (currentTag) {
                            "title" -> if (title == null) title = text
                            "creator" -> if (author == null) author = text
                        }
                    }
                    currentTag = ""
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "metadata" -> inMetadata = false
                        "manifest" -> inManifest = false
                        "spine" -> inSpine = false
                    }
                }
            }
            parser.next()
        }

        return OpfData(title, author, manifest, spineIds, tocId)
    }

    private fun buildChapters(
        spineIds: List<String>,
        manifest: Map<String, ManifestItem>,
        basePath: String,
        zipEntries: Map<String, ByteArray>,
        resources: Map<String, ByteArray>,
        tocEntries: List<TocEntry>
    ): List<EpubChapter> {
        return spineIds.mapIndexedNotNull { index, spineId ->
            val item = manifest[spineId] ?: return@mapIndexedNotNull null
            val fullPath = resolvePath(basePath, item.href)
            val contentBytes = zipEntries[fullPath] ?: return@mapIndexedNotNull null
            var html = String(contentBytes, Charsets.UTF_8)

            html = inlineImages(html, basePath, fullPath, resources, zipEntries)
            html = inlineCss(html, basePath, fullPath, resources, zipEntries)

            EpubChapter(
                id = spineId,
                title = "Chapter ${index + 1}",
                href = item.href,
                htmlContent = html
            )
        }
    }

    private fun parseToc(
        opfData: OpfData,
        basePath: String,
        zipEntries: Map<String, ByteArray>
    ): List<TocEntry> {
        val navItem = opfData.manifest.values.find { it.mediaType == "application/xhtml+xml" && it.href.contains("nav", ignoreCase = true) }
        if (navItem != null) {
            val navPath = resolvePath(basePath, navItem.href)
            val navContent = zipEntries[navPath]
            if (navContent != null) {
                val entries = parseNavXhtml(String(navContent, Charsets.UTF_8))
                if (entries.isNotEmpty()) return entries
            }
        }

        val tocId = opfData.tocId
        if (tocId != null) {
            val ncxItem = opfData.manifest[tocId]
            if (ncxItem != null) {
                val ncxPath = resolvePath(basePath, ncxItem.href)
                val ncxContent = zipEntries[ncxPath]
                if (ncxContent != null) {
                    return parseNcx(String(ncxContent, Charsets.UTF_8))
                }
            }
        }

        val ncxItem = opfData.manifest.values.find { it.mediaType == "application/x-dtbncx+xml" }
        if (ncxItem != null) {
            val ncxPath = resolvePath(basePath, ncxItem.href)
            val ncxContent = zipEntries[ncxPath]
            if (ncxContent != null) {
                return parseNcx(String(ncxContent, Charsets.UTF_8))
            }
        }

        return emptyList()
    }

    private fun parseNavXhtml(html: String): List<TocEntry> {
        val entries = mutableListOf<TocEntry>()
        val parser = createXmlParser(html)
        var inNav = false
        var inOl = false
        var depth = 0
        var currentHref: String? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "nav" -> {
                            val type = parser.getAttributeValue("http://www.idpf.org/2007/ops", "type")
                                ?: parser.getAttributeValue(null, "epub:type")
                            if (type == "toc") inNav = true
                        }
                        "ol" -> if (inNav) { inOl = true; depth++ }
                        "a" -> {
                            if (inNav && inOl && depth == 1) {
                                currentHref = parser.getAttributeValue(null, "href")
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (currentHref != null) {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            entries.add(TocEntry(title = text, href = currentHref!!))
                            currentHref = null
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "nav" -> { inNav = false; inOl = false }
                        "ol" -> if (inNav) depth--
                        "a" -> currentHref = null
                    }
                }
            }
            parser.next()
        }
        return entries
    }

    private fun parseNcx(xml: String): List<TocEntry> {
        val entries = mutableListOf<TocEntry>()
        val parser = createXmlParser(xml)
        var inNavPoint = false
        var navPointDepth = 0
        var currentTitle: String? = null
        var currentHref: String? = null
        var inText = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "navPoint" -> {
                            navPointDepth++
                            inNavPoint = true
                            currentTitle = null
                            currentHref = null
                        }
                        "text" -> if (inNavPoint) inText = true
                        "content" -> {
                            if (inNavPoint) {
                                currentHref = parser.getAttributeValue(null, "src")
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inText) {
                        currentTitle = parser.text?.trim()
                        inText = false
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "navPoint" -> {
                            if (navPointDepth == 1 && currentTitle != null && currentHref != null) {
                                entries.add(TocEntry(title = currentTitle!!, href = currentHref!!))
                            }
                            navPointDepth--
                            if (navPointDepth == 0) inNavPoint = false
                        }
                        "text" -> inText = false
                    }
                }
            }
            parser.next()
        }
        return entries
    }

    private fun assignChapterTitles(
        chapters: List<EpubChapter>,
        toc: List<TocEntry>
    ): List<EpubChapter> {
        if (toc.isEmpty()) return chapters

        return chapters.map { chapter ->
            val matchingEntry = toc.find { entry ->
                val entryFile = entry.href.substringBefore('#').substringBefore('?')
                val chapterFile = chapter.href.substringBefore('#').substringBefore('?')
                entryFile == chapterFile
            }
            if (matchingEntry != null) {
                chapter.copy(title = matchingEntry.title)
            } else {
                chapter
            }
        }
    }

    private fun inlineImages(
        html: String,
        basePath: String,
        chapterFullPath: String,
        resources: Map<String, ByteArray>,
        zipEntries: Map<String, ByteArray>
    ): String {
        val chapterDir = chapterFullPath.substringBeforeLast('/', "")
        val imgPattern = Regex("""(src\s*=\s*["'])([^"']+)(["'])""", RegexOption.IGNORE_CASE)

        return imgPattern.replace(html) { match ->
            val prefix = match.groupValues[1]
            val src = match.groupValues[2]
            val suffix = match.groupValues[3]

            if (src.startsWith("data:")) {
                match.value
            } else {
                val resolvedPath = when {
                    src.startsWith("/") -> src.removePrefix("/")
                    src.startsWith("../") || !src.contains("://") -> {
                        val fromChapterDir = resolveRelativePath(chapterDir, src)
                        fromChapterDir
                    }
                    else -> src
                }

                val imageBytes = zipEntries[resolvedPath]
                    ?: resources[resolvedPath]
                    ?: zipEntries[resolvePath(basePath, src)]
                    ?: resources[resolvePath(basePath, src)]

                if (imageBytes != null) {
                    val mediaType = guessMediaType(resolvedPath)
                    val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                    "${prefix}data:$mediaType;base64,$base64$suffix"
                } else {
                    match.value
                }
            }
        }
    }

    private fun inlineCss(
        html: String,
        basePath: String,
        chapterFullPath: String,
        resources: Map<String, ByteArray>,
        zipEntries: Map<String, ByteArray>
    ): String {
        val chapterDir = chapterFullPath.substringBeforeLast('/', "")
        val linkPattern = Regex(
            """<link[^>]+rel\s*=\s*["']stylesheet["'][^>]*href\s*=\s*["']([^"']+)["'][^>]*/?\s*>""",
            RegexOption.IGNORE_CASE
        )
        val linkPattern2 = Regex(
            """<link[^>]+href\s*=\s*["']([^"']+)["'][^>]*rel\s*=\s*["']stylesheet["'][^>]*/?\s*>""",
            RegexOption.IGNORE_CASE
        )

        var result = html

        val allMatches = (linkPattern.findAll(result) + linkPattern2.findAll(result))
            .distinctBy { it.range }
            .sortedByDescending { it.range.first }

        for (match in allMatches) {
            val cssHref = match.groupValues[1]
            val resolvedPath = when {
                cssHref.startsWith("/") -> cssHref.removePrefix("/")
                else -> resolveRelativePath(chapterDir, cssHref)
            }

            val cssBytes = zipEntries[resolvedPath]
                ?: resources[resolvedPath]
                ?: zipEntries[resolvePath(basePath, cssHref)]
                ?: resources[resolvePath(basePath, cssHref)]

            if (cssBytes != null) {
                val cssContent = String(cssBytes, Charsets.UTF_8)
                val inlinedCssContent = inlineCssUrls(cssContent, resolvedPath.substringBeforeLast('/', ""), resources, zipEntries)
                result = result.replaceRange(match.range, "<style>\n$inlinedCssContent\n</style>")
            }
        }

        return result
    }

    private fun inlineCssUrls(
        css: String,
        cssDir: String,
        resources: Map<String, ByteArray>,
        zipEntries: Map<String, ByteArray>
    ): String {
        val urlPattern = Regex("""url\(\s*["']?([^"')]+)["']?\s*\)""")
        return urlPattern.replace(css) { match ->
            val url = match.groupValues[1]
            if (url.startsWith("data:") || url.startsWith("http://") || url.startsWith("https://")) {
                match.value
            } else {
                val resolvedPath = resolveRelativePath(cssDir, url)
                val bytes = zipEntries[resolvedPath] ?: resources[resolvedPath]
                if (bytes != null) {
                    val mediaType = guessMediaType(resolvedPath)
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    "url(data:$mediaType;base64,$base64)"
                } else {
                    match.value
                }
            }
        }
    }

    private fun resolvePath(basePath: String, href: String): String {
        if (basePath.isEmpty()) return href
        return "$basePath/$href"
    }

    private fun resolveRelativePath(fromDir: String, relativePath: String): String {
        if (relativePath.startsWith("/")) return relativePath.removePrefix("/")

        val parts = if (fromDir.isEmpty()) mutableListOf() else fromDir.split('/').toMutableList()
        val relParts = relativePath.split('/')

        for (part in relParts) {
            when (part) {
                ".." -> { if (parts.isNotEmpty()) parts.removeAt(parts.size - 1) }
                "." -> { }
                else -> parts.add(part)
            }
        }
        return parts.joinToString("/")
    }

    private fun guessMediaType(path: String): String {
        val ext = path.substringAfterLast('.').lowercase()
        return when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "webp" -> "image/webp"
            "css" -> "text/css"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            else -> "application/octet-stream"
        }
    }

    private fun createXmlParser(content: String): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(StringReader(content))
        return parser
    }
}

class EpubParseException(message: String) : Exception(message)
