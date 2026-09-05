package com.example

import com.example.pdf.PdfSecurity
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPdfSecurityEncryptionDecryption() {
    kotlinx.coroutines.runBlocking {
      val tempDir = File(System.getProperty("java.io.tmpdir"), "pdf_test_${System.currentTimeMillis()}").apply { mkdirs() }
      val srcFile = File(tempDir, "test_doc.pdf")
      val encFile = File(tempDir, "test_enc.pdf")
      val decFile = File(tempDir, "test_dec.pdf")

      val sampleBytes = "Sample PDF Content Stream %PDF-1.4 123456789".toByteArray()
      srcFile.writeBytes(sampleBytes)

      val password = "SecretPassword123!"

      // Encrypt
      val encResult = PdfSecurity.protectPdf(srcFile, encFile, password)
      assertTrue("Encryption should succeed", encResult.isSuccess)
      assertTrue("Encrypted file must exist", encFile.exists())
      assertTrue("Encrypted file must be different from source", !encFile.readBytes().contentEquals(sampleBytes))

      // Decrypt with correct password
      val decResult = PdfSecurity.unlockPdf(encFile, decFile, password)
      assertTrue("Decryption should succeed with correct password", decResult.isSuccess)
      assertArrayEquals("Decrypted content must match original", sampleBytes, decFile.readBytes())

      // Decrypt with incorrect password should fail
      val failDecFile = File(tempDir, "test_fail_dec.pdf")
      val wrongPassResult = PdfSecurity.unlockPdf(encFile, failDecFile, "WrongPassword!")
      assertFalse("Decryption should fail with wrong password", wrongPassResult.isSuccess)

      // Clean up
      tempDir.deleteRecursively()
    }
  }
}

