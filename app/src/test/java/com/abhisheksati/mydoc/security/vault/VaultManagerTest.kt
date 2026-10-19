package com.abhisheksati.mydoc.security.vault

import android.content.Context
import android.content.ContextWrapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class VaultManagerTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var testKey: SecretKey
    private lateinit var vaultManager: VaultManager

    @Before
    fun setUp() {
        val root = tempFolder.root
        context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(root, "cache").apply { mkdirs() }
        }

        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        testKey = keyGen.generateKey()

        vaultManager = VaultManager(context, keyProvider = { testKey })
    }

    @Test
    fun encryptAndDecrypt_preservesContentIdentically() = runBlocking {
        val sourceFile = tempFolder.newFile("secret_document.pdf")
        val secretContent = "Confidential financial report payload with unicode: 🔒 ✓ €"
        sourceFile.writeText(secretContent)

        // 1. Encrypt and move to vault
        val encryptResult = vaultManager.encryptAndMoveToVault(sourceFile)
        assertTrue(encryptResult.isSuccess)
        val vaultItem = encryptResult.getOrThrow()

        // Source file must be shredded/deleted
        assertFalse("Source file must be shredded after moving to vault", sourceFile.exists())
        assertTrue("Encrypted file must exist in vault", vaultItem.encryptedFile.exists())
        assertEquals("secret_document.pdf", vaultItem.originalName)

        // 2. Query vault items
        val items = vaultManager.getVaultItems()
        assertEquals(1, items.size)
        assertEquals(vaultItem.id, items[0].id)
        assertEquals("secret_document.pdf", items[0].originalName)

        // 3. Decrypt and restore
        val restoreDir = tempFolder.newFolder("restored_files")
        val restoreResult = vaultManager.decryptAndRestore(vaultItem, restoreDir)
        assertTrue(restoreResult.isSuccess)
        val restoredFile = restoreResult.getOrThrow()

        assertTrue(restoredFile.exists())
        assertEquals("secret_document.pdf", restoredFile.name)
        assertEquals(secretContent, restoredFile.readText())

        // Encrypted vault file must be removed after restoration
        assertFalse(vaultItem.encryptedFile.exists())
    }

    @Test
    fun decryptToTemp_createsPreviewWithoutDeletingVaultItem() = runBlocking {
        val sourceFile = tempFolder.newFile("notes.txt")
        val content = "Temporary decrypted notes"
        sourceFile.writeText(content)

        val encryptResult = vaultManager.encryptAndMoveToVault(sourceFile)
        val vaultItem = encryptResult.getOrThrow()

        val tempResult = vaultManager.decryptToTemp(vaultItem)
        assertTrue(tempResult.isSuccess)
        val tempFile = tempResult.getOrThrow()

        assertTrue(tempFile.exists())
        assertEquals(content, tempFile.readText())
        assertTrue("Encrypted vault file must remain intact during preview", vaultItem.encryptedFile.exists())
    }

    @Test
    fun deleteVaultItem_shredsAndRemovesEncryptedFile() = runBlocking {
        val sourceFile = tempFolder.newFile("discard.txt")
        sourceFile.writeText("Data to delete permanently")

        val encryptResult = vaultManager.encryptAndMoveToVault(sourceFile)
        val vaultItem = encryptResult.getOrThrow()
        assertTrue(vaultItem.encryptedFile.exists())

        val deleted = vaultManager.deleteVaultItem(vaultItem)
        assertTrue(deleted)
        assertFalse(vaultItem.encryptedFile.exists())

        val items = vaultManager.getVaultItems()
        assertTrue(items.isEmpty())
    }

    @Test
    fun corruptedVaultFile_returnsFailureGracefully() = runBlocking {
        val corruptFile = File(context.filesDir, "safe_vault/corrupt.vault").apply {
            parentFile?.mkdirs()
            writeBytes("NOT_A_VAULT_HEADER_TRUNCATED".toByteArray())
        }
        assertTrue(corruptFile.exists())

        // Should not be listed as valid vault item
        val items = vaultManager.getVaultItems()
        assertTrue(items.isEmpty())
    }
}
