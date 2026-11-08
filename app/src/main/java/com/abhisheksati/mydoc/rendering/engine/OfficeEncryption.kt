package com.abhisheksati.mydoc.rendering.engine

/** Built-in readers must not scrape encryption metadata into invented document contents. */
internal fun rejectEncryptedOfficeSource(bytes: ByteArray) {
    if (bytes.size < 512 || bytes[0] != 0xD0.toByte() || bytes[1] != 0xCF.toByte() ||
        bytes[2] != 0x11.toByte() || bytes[3] != 0xE0.toByte()) return
    val container = runCatching { Ole2File(bytes) }.getOrNull() ?: return
    if (container.hasStream("EncryptedPackage") && container.hasStream("EncryptionInfo")) {
        throw com.abhisheksati.mydoc.domain.model.DocumentLoadException(
            com.abhisheksati.mydoc.domain.model.LoadFailure.PASSWORD_PROTECTED,
            "Install enhanced Office support from Settings → Reading to open password-protected Office documents."
        )
    }
}
