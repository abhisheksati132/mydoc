package com.abhisheksati.mydoc.data.local

import com.abhisheksati.mydoc.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties
import java.util.UUID

/** Private, bounded preference data. File paths are keys, never instructions to mutate files. */
class ExplorerPreferences(read: () -> String? = { null }, private val write: (String) -> Unit = {}) {
    private val initial = decode(runCatching(read).getOrNull())
    private val searches = MutableStateFlow(initial.first)
    val savedSearches = searches.asStateFlow()
    private val views = MutableStateFlow(initial.second)
    // Map equality ignores insertion order; keep eviction order outside StateFlow conflation.
    private var viewOrder = LinkedHashMap(initial.second)
    val folderViews = views.asStateFlow()

    fun folderView(directory: File): FolderViewPreferences? = views.value[pathKey(directory)]

    @Synchronized
    fun setFolderView(directory: File, view: FolderViewPreferences): Boolean {
        val next = LinkedHashMap(viewOrder)
        val key = pathKey(directory)
        if (key.length > 4096) return false
        next.remove(key)
        next[key] = view
        while (next.size > 200) next.remove(next.keys.first())
        return persist(searches.value, next)
    }

    @Synchronized
    fun resetFolderView(directory: File): Boolean = persist(searches.value, viewOrder - pathKey(directory))

    @Synchronized
    fun saveSearch(label: String, directory: File, options: ExplorerSearchOptions, now: Long = System.currentTimeMillis()): Boolean {
        if (searches.value.size >= 50 || !options.isValid) return false
        val days = options.modifiedWithinDays ?: options.modifiedAfter?.let { cutoff ->
            if (cutoff > now || cutoff < now - 365_000L * 86_400_000L) return false
            ((now - cutoff + 86_399_999L) / 86_400_000L).coerceAtLeast(1).toInt()
        }
        val search = SavedExplorerSearch(UUID.randomUUID().toString(), label.trim(), pathKey(directory), options.copy(modifiedAfter = null, modifiedWithinDays = null), days)
        if (!valid(search)) return false
        return persist(listOf(search) + searches.value, viewOrder)
    }

    @Synchronized
    fun deleteSearch(id: String): Boolean = persist(searches.value.filterNot { it.id == id }, viewOrder)

    private fun persist(nextSearches: List<SavedExplorerSearch>, nextViews: Map<String, FolderViewPreferences>): Boolean {
        val props = Properties().apply {
            setProperty("version", "1")
            setProperty("searches", nextSearches.size.toString())
            nextSearches.forEachIndexed { i, s ->
                val prefix = "s$i."
                setProperty(prefix + "id", s.id); setProperty(prefix + "label", s.label); setProperty(prefix + "path", s.directory)
                setProperty(prefix + "name", s.filters.name); setProperty(prefix + "ext", s.filters.extension)
                s.filters.minBytes?.let { setProperty(prefix + "min", it.toString()) }
                s.filters.maxBytes?.let { setProperty(prefix + "max", it.toString()) }
                s.modifiedWithinDays?.let { setProperty(prefix + "days", it.toString()) }
                setProperty(prefix + "recursive", s.filters.recursive.toString())
            }
            setProperty("folders", nextViews.size.toString())
            nextViews.entries.forEachIndexed { i, (path, view) ->
                setProperty("f$i.path", path); setProperty("f$i.sort", view.sortOrder.field.name)
                setProperty("f$i.ascending", view.sortOrder.isAscending.toString()); setProperty("f$i.grid", view.grid.toString())
            }
        }
        val encoded = StringWriter().also { props.store(it, "MyDoc explorer preferences") }.toString()
        if (encoded.length > 1_000_000 || runCatching { write(encoded) }.isFailure) return false
        searches.value = nextSearches
        viewOrder = LinkedHashMap(nextViews)
        views.value = nextViews.toMap()
        return true
    }

    companion object {
        fun pathKey(directory: File): String = directory.absoluteFile.normalize().path

        private fun valid(s: SavedExplorerSearch): Boolean = s.id.length in 1..128 &&
            s.label.length in 1..80 && s.label.none { it.isISOControl() } &&
            s.directory.length in 1..4096 && File(s.directory).isAbsolute &&
            s.filters.isValid && s.filters.name.length <= 512 && s.filters.extension.length <= 64 &&
            (s.modifiedWithinDays == null || s.modifiedWithinDays in 1..365_000)

        private fun decode(raw: String?): Pair<List<SavedExplorerSearch>, Map<String, FolderViewPreferences>> {
            if (raw == null || raw.length > 1_000_000) return emptyList<SavedExplorerSearch>() to emptyMap()
            val props = runCatching { Properties().apply { load(StringReader(raw)) } }.getOrNull()
                ?: return emptyList<SavedExplorerSearch>() to emptyMap()
            if (props.getProperty("version") != "1") return emptyList<SavedExplorerSearch>() to emptyMap()
            fun text(key: String): String = requireNotNull(props.getProperty(key))
            fun number(key: String): Long? = props.getProperty(key)?.let { requireNotNull(it.toLongOrNull()) }
            val searches = (0 until (props.getProperty("searches")?.toIntOrNull() ?: 0).coerceIn(0, 50)).mapNotNull { i ->
                runCatching {
                    SavedExplorerSearch(text("s$i.id"), text("s$i.label"), text("s$i.path"),
                        ExplorerSearchOptions(text("s$i.name"), text("s$i.ext"), number("s$i.min"), number("s$i.max"),
                            recursive = text("s$i.recursive").toBooleanStrict()),
                        props.getProperty("s$i.days")?.toInt()).takeIf(::valid)
                }.getOrNull()
            }.distinctBy { it.id }
            val views = linkedMapOf<String, FolderViewPreferences>()
            repeat((props.getProperty("folders")?.toIntOrNull() ?: 0).coerceIn(0, 200)) { i ->
                runCatching {
                    val path = text("f$i.path")
                    require(path.length in 1..4096 && File(path).isAbsolute)
                    views[path] = FolderViewPreferences(FileSortOrder(FileSortField.valueOf(text("f$i.sort")), text("f$i.ascending").toBooleanStrict()), text("f$i.grid").toBooleanStrict())
                }
            }
            return searches to views
        }
    }
}
