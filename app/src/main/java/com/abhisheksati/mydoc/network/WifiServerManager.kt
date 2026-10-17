package com.abhisheksati.mydoc.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class WifiServerManager(private val context: Context) {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                val addrs = intf.inetAddresses ?: continue
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun startServer(rootDirectory: File, preferredPort: Int = 8080) {
        if (_isRunning.value) return

        val ip = getLocalIpAddress()
        if (ip == null) {
            _serverUrl.value = null
            return
        }

        serverJob = scope.launch {
            var port = preferredPort
            var socket: ServerSocket? = null
            while (socket == null && port < preferredPort + 10) {
                try {
                    socket = ServerSocket(port)
                } catch (_: Exception) {
                    port++
                }
            }

            if (socket == null) return@launch
            serverSocket = socket
            val url = "http://$ip:$port"
            _serverUrl.value = url
            _isRunning.value = true

            while (_isRunning.value) {
                try {
                    val client = socket.accept()
                    launch {
                        handleClient(client, rootDirectory)
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    fun stopServer() {
        _isRunning.value = false
        _serverUrl.value = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
    }

    private suspend fun handleClient(client: Socket, rootDir: File) = withContext(Dispatchers.IO) {
        try {
            client.soTimeout = 15000
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())

            val reader = BufferedReader(InputStreamReader(input))
            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val fullUrl = parts[1]
            val path = fullUrl.substringBefore('?')
            val queryString = if (fullUrl.contains('?')) fullUrl.substringAfter('?') else ""
            val queryParams = parseQueryParams(queryString)

            // Read headers to get content length if POST
            var contentLength = 0L
            var line: String? = reader.readLine()
            while (!line.isNullOrEmpty()) {
                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line.substringAfter(':').trim().toLongOrNull() ?: 0L
                }
                line = reader.readLine()
            }

            when {
                method == "GET" && path == "/" -> {
                    serveWebUi(output)
                }

                method == "GET" && path == "/api/list" -> {
                    val reqPath = queryParams["path"] ?: rootDir.absolutePath
                    serveDirectoryList(output, File(reqPath), rootDir)
                }

                method == "GET" && path == "/api/download" -> {
                    val reqPath = queryParams["path"] ?: ""
                    serveFileDownload(output, File(reqPath), rootDir)
                }

                method == "POST" && path == "/api/upload" -> {
                    val destFolder = File(queryParams["path"] ?: rootDir.absolutePath)
                    val filename = queryParams["name"] ?: "upload_${System.currentTimeMillis()}"
                    handleFileUpload(input, output, destFolder, filename, contentLength)
                }

                method == "POST" && path == "/api/mkdir" -> {
                    val parentFolder = File(queryParams["path"] ?: rootDir.absolutePath)
                    val name = queryParams["name"] ?: "New Folder"
                    handleCreateFolder(output, parentFolder, name)
                }

                method == "POST" && path == "/api/delete" -> {
                    val target = File(queryParams["path"] ?: "")
                    handleDelete(output, target, rootDir)
                }

                else -> {
                    sendResponse(output, 404, "text/plain", "Not Found".toByteArray())
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                client.close()
            } catch (_: Exception) {}
        }
    }

    private fun parseQueryParams(queryString: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (queryString.isEmpty()) return map
        val pairs = queryString.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[key] = value
            }
        }
        return map
    }

    private fun serveWebUi(output: OutputStream) {
        val html = getHtmlContent()
        sendResponse(output, 200, "text/html; charset=UTF-8", html.toByteArray(Charsets.UTF_8))
    }

    private fun serveDirectoryList(output: OutputStream, dir: File, rootDir: File) {
        val target = if (dir.exists() && dir.isDirectory) dir else rootDir
        val files = target.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) })) ?: emptyList()

        val jsonBuilder = StringBuilder("[")
        files.forEachIndexed { i, f ->
            val isDir = f.isDirectory
            val size = if (isDir) 0L else f.length()
            val time = f.lastModified()
            val escName = escapeJson(f.name)
            val escPath = escapeJson(f.absolutePath)
            jsonBuilder.append("""{"name":"$escName","path":"$escPath","isDir":$isDir,"size":$size,"modified":$time}""")
            if (i < files.size - 1) jsonBuilder.append(",")
        }
        jsonBuilder.append("]")

        sendResponse(output, 200, "application/json", jsonBuilder.toString().toByteArray(Charsets.UTF_8))
    }

    private fun serveFileDownload(output: OutputStream, file: File, rootDir: File) {
        if (!file.exists() || file.isDirectory) {
            sendResponse(output, 404, "text/plain", "File not found".toByteArray())
            return
        }

        val len = file.length()
        val writer = PrintWriter(output)
        writer.print("HTTP/1.1 200 OK\r\n")
        writer.print("Content-Type: application/octet-stream\r\n")
        writer.print("Content-Length: $len\r\n")
        writer.print("Content-Disposition: attachment; filename=\"${file.name}\"\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.flush()

        FileInputStream(file).use { fis ->
            val buf = ByteArray(64 * 1024)
            var n: Int
            while (fis.read(buf).also { n = it } > 0) {
                output.write(buf, 0, n)
            }
        }
        output.flush()
    }

    private fun handleFileUpload(input: BufferedInputStream, output: OutputStream, destFolder: File, filename: String, contentLength: Long) {
        if (!destFolder.exists()) destFolder.mkdirs()
        val destFile = File(destFolder, filename)

        FileOutputStream(destFile).use { fos ->
            val buf = ByteArray(64 * 1024)
            var remaining = contentLength
            while (remaining > 0) {
                val toRead = minOf(buf.size.toLong(), remaining).toInt()
                val read = input.read(buf, 0, toRead)
                if (read <= 0) break
                fos.write(buf, 0, read)
                remaining -= read
            }
        }

        sendResponse(output, 200, "application/json", """{"status":"ok","name":"${destFile.name}"}""".toByteArray())
    }

    private fun handleCreateFolder(output: OutputStream, parent: File, name: String) {
        val target = File(parent, name)
        val created = target.mkdirs()
        val code = if (created) 200 else 400
        sendResponse(output, code, "application/json", """{"status":"${if (created) "ok" else "error"}"}""".toByteArray())
    }

    private fun handleDelete(output: OutputStream, target: File, rootDir: File) {
        if (!target.exists() || target == rootDir) {
            sendResponse(output, 400, "application/json", """{"status":"error"}""".toByteArray())
            return
        }
        val deleted = if (target.isDirectory) target.deleteRecursively() else target.delete()
        sendResponse(output, 200, "application/json", """{"status":"${if (deleted) "ok" else "error"}"}""".toByteArray())
    }

    private fun sendResponse(output: OutputStream, status: Int, contentType: String, data: ByteArray) {
        val writer = PrintWriter(output)
        writer.print("HTTP/1.1 $status ${if (status == 200) "OK" else "Error"}\r\n")
        writer.print("Content-Type: $contentType\r\n")
        writer.print("Content-Length: ${data.size}\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.flush()
        output.write(data)
        output.flush()
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun getHtmlContent(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MyDoc Wi-Fi File Transfer</title>
    <style>
        :root {
            --bg: #121214;
            --surface: #1e1e24;
            --surface-hover: #282830;
            --accent: #8AB4F8;
            --text-primary: #ededed;
            --text-secondary: #9aa0a6;
            --border: #33333d;
            --danger: #ef5350;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: var(--bg); color: var(--text-primary); padding: 24px; }
        header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
        .logo-area { display: flex; align-items: center; gap: 12px; }
        .logo { width: 36px; height: 36px; border-radius: 8px; background: var(--accent); display: flex; align-items: center; justify-content: center; font-weight: bold; color: #121214; }
        h1 { font-size: 20px; font-weight: 600; }
        .actions { display: flex; gap: 10px; align-items: center; }
        button, label.btn {
            background: var(--surface); color: var(--text-primary); border: 1px solid var(--border); padding: 8px 14px;
            border-radius: 8px; font-size: 13px; font-weight: 500; cursor: pointer; transition: 0.15s ease; display: inline-flex; align-items: center; gap: 6px;
        }
        button:hover, label.btn:hover { background: var(--surface-hover); border-color: var(--accent); }
        button.primary, label.btn.primary { background: var(--accent); color: #121214; border: none; }
        button.primary:hover, label.btn.primary:hover { opacity: 0.9; }
        .breadcrumbs { display: flex; align-items: center; gap: 6px; padding: 10px 14px; background: var(--surface); border-radius: 8px; margin-bottom: 16px; font-size: 13px; overflow-x: auto; white-space: nowrap; border: 1px solid var(--border); }
        .crumb { color: var(--accent); cursor: pointer; }
        .crumb:hover { text-decoration: underline; }
        .crumb.current { color: var(--text-primary); cursor: default; text-decoration: none; font-weight: 600; }
        .dropzone {
            border: 2px dashed var(--border); border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 20px;
            transition: 0.2s; background: rgba(30,30,36,0.5);
        }
        .dropzone.dragover { border-color: var(--accent); background: rgba(138,180,248,0.1); }
        .file-list { background: var(--surface); border-radius: 12px; overflow: hidden; border: 1px solid var(--border); }
        .file-row { display: flex; align-items: center; padding: 12px 16px; border-bottom: 1px solid var(--border); transition: 0.1s; }
        .file-row:last-child { border-bottom: none; }
        .file-row:hover { background: var(--surface-hover); }
        .file-icon { font-size: 20px; width: 32px; }
        .file-name { flex: 1; cursor: pointer; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding-right: 12px; }
        .file-name:hover { color: var(--accent); }
        .file-meta { font-size: 12px; color: var(--text-secondary); width: 140px; text-align: right; }
        .file-actions { display: flex; gap: 8px; margin-left: 16px; }
        .btn-icon { background: transparent; border: none; font-size: 14px; padding: 6px; cursor: pointer; color: var(--text-secondary); border-radius: 6px; }
        .btn-icon:hover { color: var(--text-primary); background: var(--border); }
        .btn-icon.del:hover { color: var(--danger); }
        .progress-bar-container { height: 6px; background: var(--border); border-radius: 3px; overflow: hidden; margin-top: 10px; display: none; }
        .progress-bar { height: 100%; width: 0%; background: var(--accent); transition: width 0.1s; }
    </style>
</head>
<body>
    <header>
        <div class="logo-area">
            <div class="logo">MD</div>
            <div>
                <h1>MyDoc Wi-Fi Transfer</h1>
                <p style="font-size: 12px; color: var(--text-secondary);">Direct phone-to-browser high-speed transfer</p>
            </div>
        </div>
        <div class="actions">
            <button onclick="createNewFolder()">📁 New Folder</button>
            <label class="btn primary">
                ⬆ Upload File
                <input type="file" id="fileInput" multiple style="display: none;" onchange="handleFilesSelected(this.files)">
            </label>
            <button onclick="loadDir(currentPath)">🔄 Refresh</button>
        </div>
    </header>

    <div class="breadcrumbs" id="breadcrumbs"></div>

    <div class="dropzone" id="dropzone">
        <p style="font-size: 14px; color: var(--text-primary); font-weight: 500;">Drag & drop files here to upload</p>
        <p style="font-size: 12px; color: var(--text-secondary); margin-top: 4px;">Files transfer directly over Wi-Fi without internet</p>
        <div class="progress-bar-container" id="progressContainer">
            <div class="progress-bar" id="progressBar"></div>
        </div>
    </div>

    <div class="file-list" id="fileList"></div>

    <script>
        let currentPath = '';
        let rootPath = '';

        async function init() {
            setupDropzone();
            await loadDir('');
        }

        async function loadDir(path) {
            try {
                const res = await fetch('/api/list?path=' + encodeURIComponent(path));
                const items = await res.json();
                if (!rootPath && path === '') {
                    rootPath = path;
                }
                currentPath = path;
                renderBreadcrumbs(path);
                renderItems(items);
            } catch (e) {
                alert('Failed to load folder: ' + e);
            }
        }

        function renderBreadcrumbs(path) {
            const container = document.getElementById('breadcrumbs');
            container.innerHTML = '';
            const rootCrumb = document.createElement('span');
            rootCrumb.className = 'crumb' + (!path ? ' current' : '');
            rootCrumb.textContent = 'Storage';
            rootCrumb.onclick = () => loadDir('');
            container.appendChild(rootCrumb);

            if (path) {
                const parts = path.split('/').filter(p => p.length > 0);
                let accum = '';
                parts.forEach((p, idx) => {
                    accum += '/' + p;
                    const sep = document.createElement('span');
                    sep.textContent = ' / ';
                    sep.style.color = 'var(--text-secondary)';
                    container.appendChild(sep);

                    const c = document.createElement('span');
                    const isLast = idx === parts.length - 1;
                    c.className = 'crumb' + (isLast ? ' current' : '');
                    c.textContent = p;
                    const targetPath = accum;
                    if (!isLast) c.onclick = () => loadDir(targetPath);
                    container.appendChild(c);
                });
            }
        }

        function renderItems(items) {
            const list = document.getElementById('fileList');
            list.innerHTML = '';
            if (items.length === 0) {
                list.innerHTML = '<div style="padding: 24px; text-align: center; color: var(--text-secondary); font-size: 13px;">Empty folder</div>';
                return;
            }

            items.forEach(item => {
                const row = document.createElement('div');
                row.className = 'file-row';

                const icon = document.createElement('div');
                icon.className = 'file-icon';
                icon.textContent = item.isDir ? '📁' : '📄';

                const name = document.createElement('div');
                name.className = 'file-name';
                name.textContent = item.name;
                if (item.isDir) {
                    name.onclick = () => loadDir(item.path);
                } else {
                    name.onclick = () => downloadFile(item.path);
                }

                const meta = document.createElement('div');
                meta.className = 'file-meta';
                meta.textContent = item.isDir ? 'Folder' : formatSize(item.size);

                const actions = document.createElement('div');
                actions.className = 'file-actions';

                if (!item.isDir) {
                    const dlBtn = document.createElement('button');
                    dlBtn.className = 'btn-icon';
                    dlBtn.title = 'Download';
                    dlBtn.textContent = '⬇';
                    dlBtn.onclick = (e) => { e.stopPropagation(); downloadFile(item.path); };
                    actions.appendChild(dlBtn);
                }

                const delBtn = document.createElement('button');
                delBtn.className = 'btn-icon del';
                delBtn.title = 'Delete';
                delBtn.textContent = '🗑';
                delBtn.onclick = (e) => { e.stopPropagation(); deleteItem(item.path, item.name); };
                actions.appendChild(delBtn);

                row.appendChild(icon);
                row.appendChild(name);
                row.appendChild(meta);
                row.appendChild(actions);
                list.appendChild(row);
            });
        }

        function downloadFile(path) {
            window.location.href = '/api/download?path=' + encodeURIComponent(path);
        }

        async function deleteItem(path, name) {
            if (!confirm('Are you sure you want to delete "' + name + '"?')) return;
            try {
                await fetch('/api/delete?path=' + encodeURIComponent(path), { method: 'POST' });
                loadDir(currentPath);
            } catch (e) {
                alert('Failed to delete: ' + e);
            }
        }

        async function createNewFolder() {
            const name = prompt('Enter new folder name:');
            if (!name || !name.trim()) return;
            try {
                await fetch('/api/mkdir?path=' + encodeURIComponent(currentPath) + '&name=' + encodeURIComponent(name.trim()), { method: 'POST' });
                loadDir(currentPath);
            } catch (e) {
                alert('Failed to create folder: ' + e);
            }
        }

        function setupDropzone() {
            const dz = document.getElementById('dropzone');
            ['dragenter', 'dragover'].forEach(name => {
                dz.addEventListener(name, (e) => { e.preventDefault(); dz.classList.add('dragover'); }, false);
            });
            ['dragleave', 'drop'].forEach(name => {
                dz.addEventListener(name, (e) => { e.preventDefault(); dz.classList.remove('dragover'); }, false);
            });
            dz.addEventListener('drop', (e) => {
                const files = e.dataTransfer.files;
                handleFilesSelected(files);
            }, false);
        }

        async function handleFilesSelected(files) {
            if (!files || files.length === 0) return;
            const container = document.getElementById('progressContainer');
            const bar = document.getElementById('progressBar');
            container.style.display = 'block';

            for (let i = 0; i < files.length; i++) {
                const file = files[i];
                bar.style.width = ((i / files.length) * 100) + '%';
                try {
                    await fetch('/api/upload?path=' + encodeURIComponent(currentPath) + '&name=' + encodeURIComponent(file.name), {
                        method: 'POST',
                        body: file
                    });
                } catch (e) {
                    alert('Failed to upload ' + file.name + ': ' + e);
                }
            }

            bar.style.width = '100%';
            setTimeout(() => {
                container.style.display = 'none';
                bar.style.width = '0%';
                loadDir(currentPath);
            }, 400);
        }

        function formatSize(bytes) {
            if (bytes <= 0) return '0 B';
            const k = 1024;
            const sizes = ['B', 'KB', 'MB', 'GB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
        }

        window.onload = init;
    </script>
</body>
</html>
        """.trimIndent()
    }
}
