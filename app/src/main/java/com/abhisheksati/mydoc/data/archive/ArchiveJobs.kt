package com.abhisheksati.mydoc.data.archive

import android.content.Context
import android.content.Intent
import android.util.AtomicFile
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.domain.archive.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class ArchiveJobStatus { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED, INTERRUPTED }

internal fun retainArchiveHistory(records: List<ArchiveJobRecord>): List<ArchiveJobRecord> {
    val finishedSlots = (50 - records.count { it.active }).coerceAtLeast(0)
    val finishedIds = records.filterNot { it.active }.take(finishedSlots).mapTo(HashSet()) { it.id }
    return records.filter { it.active || it.id in finishedIds }
}

data class ArchiveJobRecord(
    val id: String, val title: String, val status: ArchiveJobStatus = ArchiveJobStatus.QUEUED,
    val progress: ArchiveProgress = ArchiveProgress(), val outputs: List<File> = emptyList(),
    val problem: ArchiveProblem? = null, val startedAt: Long = 0, val elapsedMillis: Long = 0, val canPause: Boolean = false
) {
    val active get() = status in setOf(ArchiveJobStatus.QUEUED, ArchiveJobStatus.RUNNING, ArchiveJobStatus.PAUSED)
    val bytesPerSecond get() = if (elapsedMillis > 0) progress.completedBytes * 1000 / elapsedMillis else 0
    val remainingSeconds: Long? get() = bytesPerSecond.takeIf { it > 0 && progress.totalBytes > progress.completedBytes }
        ?.let { (progress.totalBytes - progress.completedBytes) / it }
}

sealed interface ArchiveRequest {
    data class Create(val sources: List<File>, val parent: File, val name: String, val options: ArchiveCreateOptions) : ArchiveRequest
    data class Extract(val source: File, val parent: File, val selected: Set<String>? = null,
                       val conflict: ArchiveConflictPolicy = ArchiveConflictPolicy.NEW_FOLDER) : ArchiveRequest
    data class Test(val source: File) : ArchiveRequest
    data class Edit(val source: File, val parent: File, val remove: Set<String> = emptySet(),
                    val renames: Map<String, String> = emptyMap(), val additions: List<File> = emptyList()) : ArchiveRequest
}

/** One bounded queue per application. Credentials and live handles never enter history. */
class ArchiveJobs(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val records = MutableStateFlow<List<ArchiveJobRecord>>(emptyList())
    val state = records.asStateFlow()
    private data class Pending(val request: ArchiveRequest, val password: CharArray?)
    private val pending = ConcurrentHashMap<String, Pending>()
    private var worker: Job? = null
    @Volatile private var operation: Job? = null
    @Volatile private var control: ArchiveProcessControl? = null
    @Volatile private var currentId: String? = null
    private val history = AtomicFile(File(context.filesDir, "archive-history.json"))
    private val historyLock = Any()
    private val historyReady = CompletableDeferred<Unit>()

    init {
        scope.launch {
            val restored = runCatching {
                val bytes = synchronized(historyLock) { history.openRead().use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val size = input.read(buffer)
                        if (size < 0) break
                        require(size <= 512 * 1024 - output.size())
                        output.write(buffer, 0, size)
                    }
                    output.toByteArray()
                } }
                require(bytes.size <= 512 * 1024)
                val entries = JSONArray(bytes.toString(Charsets.UTF_8))
                (0 until minOf(entries.length(), 50)).map { index ->
                    val item = entries.getJSONObject(index)
                    val status = ArchiveJobStatus.valueOf(item.getString("status"))
                    ArchiveJobRecord(item.getString("id"), item.getString("title").take(240),
                        if (status in setOf(ArchiveJobStatus.QUEUED, ArchiveJobStatus.RUNNING, ArchiveJobStatus.PAUSED)) ArchiveJobStatus.INTERRUPTED else status,
                        outputs = (item.optJSONArray("outputs") ?: JSONArray()).let { paths ->
                            (0 until minOf(paths.length(), 100)).map { File(paths.getString(it)) }
                        })
                }
            }.getOrDefault(emptyList())
            records.update { current -> retainArchiveHistory(current + restored.filter { saved -> current.none { it.id == saved.id } }) }
            historyReady.complete(Unit)
        }
    }

    @Synchronized fun submit(title: String, request: ArchiveRequest, password: CharArray? = null): String {
        require(records.value.count { it.active } < 20) { "Archive queue is full" }
        val id = UUID.randomUUID().toString()
        pending[id] = Pending(request, password?.copyOf())
        records.update { retainArchiveHistory(listOf(ArchiveJobRecord(id, title.take(240))) + it) }
        scope.launch { persist() }
        try { context.startForegroundService(Intent(context, ArchiveJobService::class.java)) }
        catch (error: Exception) {
            pending.remove(id)?.password?.fill('\u0000')
            update(id) { it.copy(status = ArchiveJobStatus.FAILED, problem = ArchiveProblem.ENGINE) }
            throw error
        }
        return id
    }

    @Synchronized internal fun start(onIdle: () -> Unit) {
        if (worker?.isActive == true) return
        worker = scope.launch {
            while (true) {
                val next = records.value.lastOrNull { it.status == ArchiveJobStatus.QUEUED } ?: break
                val task = pending.remove(next.id)
                if (task == null) { update(next.id) { if (it.status == ArchiveJobStatus.QUEUED) it.copy(status = ArchiveJobStatus.INTERRUPTED) else it }; continue }
                currentId = next.id
                val start = android.os.SystemClock.elapsedRealtime()
                update(next.id) { it.copy(status = ArchiveJobStatus.RUNNING, startedAt = start) }
                val engine = NativeArchiveEngine(context, onControl = { handle ->
                    control = handle; update(next.id) { it.copy(canPause = handle != null) }
                }, onProgress = { progress ->
                    update(next.id) { it.copy(progress = progress, elapsedMillis = android.os.SystemClock.elapsedRealtime() - start) }
                })
                operation = launch {
                    try {
                        if (records.value.none { it.id == next.id && it.status == ArchiveJobStatus.RUNNING }) throw CancellationException()
                        val outputs = when (val request = task.request) {
                            is ArchiveRequest.Create -> engine.create(request.sources, request.parent, request.name, request.options, task.password)
                            is ArchiveRequest.Extract -> listOf(engine.extract(request.source, request.parent, request.selected, task.password, request.conflict))
                            is ArchiveRequest.Test -> { engine.test(request.source, task.password); emptyList() }
                            is ArchiveRequest.Edit -> engine.editCopy(request.source, request.parent, request.remove, request.renames, request.additions, task.password)
                        }
                        update(next.id) { it.copy(status = ArchiveJobStatus.COMPLETED, outputs = outputs, progress = it.progress.copy(percent = 100)) }
                    } catch (error: CancellationException) {
                        update(next.id) { it.copy(status = ArchiveJobStatus.CANCELLED,
                            outputs = (task.request as? ArchiveRequest.Extract)?.takeIf { request -> request.conflict != ArchiveConflictPolicy.NEW_FOLDER }?.let { request -> listOf(request.parent) }.orEmpty()) }
                    } catch (error: Exception) {
                        update(next.id) { it.copy(status = ArchiveJobStatus.FAILED, problem = (error as? ArchiveFailure)?.problem ?: ArchiveProblem.ENGINE,
                            outputs = (error as? ArchiveMergeRecovery)?.let { recovery -> listOf(recovery.directory) }.orEmpty()) }
                    } finally { task.password?.fill('\u0000'); control = null }
                }
                operation?.join()
                operation = null
                currentId = null
                persist()
            }
            synchronized(this@ArchiveJobs) { worker = null; onIdle() }
        }
    }

    fun cancel(id: String) {
        if (records.value.none { it.id == id && it.active }) return
        if (id == currentId) { update(id) { it.copy(status = ArchiveJobStatus.CANCELLED) }; operation?.cancel() }
        else {
            pending.remove(id)?.password?.fill('\u0000')
            update(id) { if (it.status == ArchiveJobStatus.QUEUED) it.copy(status = ArchiveJobStatus.CANCELLED) else it }
        }
        scope.launch { persist() }
    }
    fun pause(id: String) { if (id == currentId && control?.pause() == true) update(id) { it.copy(status = ArchiveJobStatus.PAUSED) } }
    fun resume(id: String) { if (id == currentId && control?.resume() == true) update(id) { it.copy(status = ArchiveJobStatus.RUNNING) } }
    internal fun cancelAll() { records.value.filter { it.active }.forEach { cancel(it.id) } }
    fun clearFinished() { records.update { it.filter(ArchiveJobRecord::active) }; scope.launch { persist() } }
    private fun update(id: String, transform: (ArchiveJobRecord) -> ArchiveJobRecord) { records.update { list -> list.map { if (it.id == id) transform(it) else it } } }

    suspend fun awaitHistory() { historyReady.await() }

    private suspend fun persist() {
        historyReady.await()
        synchronized(historyLock) {
        // History is auxiliary. A full/unavailable disk must not stop the queue.
        runCatching {
        val data = JSONArray()
        records.value.forEach { record -> data.put(JSONObject().put("id", record.id).put("title", record.title)
            .put("status", record.status.name).put("outputs", JSONArray(record.outputs.take(1).map { it.absolutePath }))) }
        val stream = history.startWrite()
        try { stream.write(data.toString().toByteArray(Charsets.UTF_8)); history.finishWrite(stream) }
        catch (error: Exception) { history.failWrite(stream) }
        }
        }
    }

    companion object {
        fun get(context: Context): ArchiveJobs = (context.applicationContext as MyDocApp).archiveJobs
    }
}
