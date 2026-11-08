package com.abhisheksati.mydoc.rendering.engine.word

import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import org.libreoffice.kit.LibreOfficeKit
import org.libreoffice.kit.Office
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicBoolean

internal class WordPasswordRequired : IOException("This document requires a password")

/** The native office API is confined to the dedicated worker process and its single executor. */
internal object NativeWordCore {
    const val VERSION = "26.2.6.3"
    private var office: Office? = null

    @Synchronized
    fun convert(context: Context, source: File, output: File, password: String? = null): String? {
        check(isWordEngineProcess())
        val engine = office ?: run {
            val profile = File(context.noBackupFilesDir, "word-engine-profile-$VERSION")
            require(profile.mkdirs() || profile.isDirectory)
            require(File(profile, "cache").mkdirs() || File(profile, "cache").isDirectory)
            val ready = File(profile, ".prepared")
            if (!ready.isFile || ready.readText() != VERSION) {
                unpack(context.assets, "unpack", profile)
                ready.writeText(VERSION)
            }
            val user = File(profile, "user").apply { mkdirs() }
            atomicText(File(user, "registrymodifications.xcu"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <oor:items xmlns:oor="http://openoffice.org/2001/registry">
                  <item oor:path="/org.openoffice.Office.Common/Security/Scripting">
                    <prop oor:name="DisableMacrosExecution" oor:op="fuse"><value>true</value></prop>
                    <prop oor:name="DisableActiveContent" oor:op="fuse"><value>true</value></prop>
                    <prop oor:name="BlockUntrustedRefererLinks" oor:op="fuse"><value>true</value></prop>
                  </item>
                  <item oor:path="/org.openoffice.Office.Writer/Content/Update">
                    <prop oor:name="Link" oor:op="fuse"><value>2</value></prop>
                  </item>
                  <item oor:path="/org.openoffice.Office.Calc/Content/Update">
                    <prop oor:name="Link" oor:op="fuse"><value>2</value></prop>
                  </item>
                </oor:items>
            """.trimIndent())
            if (!com.abhisheksati.mydoc.BuildConfig.DEBUG) LibreOfficeKit.putenv("SAL_LOG=-WARN-INFO")
            LibreOfficeKit.init(context, profile.absolutePath)
            Office(requireNotNull(LibreOfficeKit.getLibreOfficeKitHandle()) { "Office engine initialization failed" })
                .also { office = it }
        }
        val url = Uri.fromFile(source).toString()
        val passwordNeeded = AtomicBoolean(false)
        val submitted = AtomicBoolean(false)
        engine.setOptionalFeatures(org.libreoffice.kit.Document.LOK_FEATURE_DOCUMENT_PASSWORD)
        engine.setMessageCallback { signal, requestedUrl ->
            if (signal == org.libreoffice.kit.Document.CALLBACK_DOCUMENT_PASSWORD) {
                val response = if (requestedUrl == url) {
                    passwordNeeded.set(true)
                    if (password != null && submitted.compareAndSet(false, true)) password else null
                } else null
                engine.setDocumentPassword(requestedUrl, response)
            }
        }
        var document: org.libreoffice.kit.Document? = null
        try {
            val opened = engine.documentLoad(url) ?: if (passwordNeeded.get()) throw WordPasswordRequired()
                else throw IOException("The Office engine could not load this document")
            document = opened
            opened.initializeForRendering()
            val renderPassword = if (passwordNeeded.get()) {
                val key = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
                java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(key)
            } else null
            opened.saveAs(Uri.fromFile(output).toString(), "pdf", wordPdfOptions(renderPassword))
            if (!output.isFile || output.length() == 0L) throw IOException("The Office engine produced no pages")
            return renderPassword
        } finally {
            document?.destroy()
            engine.setMessageCallback(null)
        }
    }

    private fun unpack(assets: AssetManager, path: String, directory: File) {
        for (name in assets.list(path).orEmpty()) {
            require('/' !in name && '\\' !in name && name != "..")
            val assetPath = "$path/$name"
            val target = File(directory, name)
            if (assets.list(assetPath).orEmpty().isNotEmpty()) {
                require(target.mkdirs() || target.isDirectory)
                unpack(assets, assetPath, target)
            } else {
                val temporary = File.createTempFile("asset-", ".part", directory)
                try {
                    assets.open(assetPath).use { input -> temporary.outputStream().use { input.copyTo(it) } }
                    Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } finally { temporary.delete() }
            }
        }
    }

    private fun atomicText(target: File, text: String) {
        val temporary = File.createTempFile("settings-", ".part", target.parentFile)
        try {
            temporary.writeText(text)
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { temporary.delete() }
    }
}
