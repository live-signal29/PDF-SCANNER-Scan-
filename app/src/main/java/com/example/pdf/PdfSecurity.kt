package com.example.pdf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object PdfSecurity {
    private const val HEADER_MAGIC = "PDF_SECURE_ENVELOPE_V1"
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12

    suspend fun protectPdf(
        sourceFile: File,
        outputFile: File,
        password: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH)
            random.nextBytes(salt)

            val iv = ByteArray(IV_LENGTH)
            random.nextBytes(iv)

            val keySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            val fileBytes = FileInputStream(sourceFile).use { it.readBytes() }
            val encryptedBytes = cipher.doFinal(fileBytes)

            FileOutputStream(outputFile).use { fos ->
                fos.write(HEADER_MAGIC.toByteArray(Charsets.UTF_8))
                fos.write(salt)
                fos.write(iv)
                fos.write(encryptedBytes)
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun isProtected(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() < HEADER_MAGIC.length) return@withContext false
            val buffer = ByteArray(HEADER_MAGIC.length)
            FileInputStream(file).use { fis ->
                val read = fis.read(buffer)
                if (read == buffer.size) {
                    val header = String(buffer, Charsets.UTF_8)
                    header == HEADER_MAGIC
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unlockPdf(
        sourceFile: File,
        outputFile: File,
        password: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            FileInputStream(sourceFile).use { fis ->
                val headerBuf = ByteArray(HEADER_MAGIC.length)
                fis.read(headerBuf)
                val header = String(headerBuf, Charsets.UTF_8)
                if (header != HEADER_MAGIC) {
                    return@withContext Result.failure(Exception("File is not protected with this system"))
                }

                val salt = ByteArray(SALT_LENGTH)
                fis.read(salt)

                val iv = ByteArray(IV_LENGTH)
                fis.read(iv)

                val cipherText = fis.readBytes()

                val keySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val keyBytes = factory.generateSecret(keySpec).encoded
                val secretKey = SecretKeySpec(keyBytes, "AES")

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

                val decryptedBytes = cipher.doFinal(cipherText)

                FileOutputStream(outputFile).use { fos ->
                    fos.write(decryptedBytes)
                }
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Incorrect password or damaged file"))
        }
    }
}
