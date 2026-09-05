package com.example.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val context: Context
) {
    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    val recentDocuments: Flow<List<DocumentEntity>> = documentDao.getRecentDocuments(5)

    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>> {
        return if (category == DocumentCategory.ALL.name) {
            documentDao.getAllDocuments()
        } else {
            documentDao.getDocumentsByCategory(category)
        }
    }

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> {
        return documentDao.searchDocuments(query)
    }

    suspend fun getDocumentById(id: Long): DocumentEntity? {
        return withContext(Dispatchers.IO) {
            documentDao.getDocumentById(id)
        }
    }

    suspend fun insertDocument(
        file: File,
        category: DocumentCategory,
        pageCount: Int = 1,
        customName: String? = null
    ): Long {
        return withContext(Dispatchers.IO) {
            val entity = DocumentEntity(
                fileName = customName ?: file.name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                pageCount = pageCount,
                category = category.name
            )
            documentDao.insertDocument(entity)
        }
    }

    suspend fun renameDocument(document: DocumentEntity, newName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val oldFile = File(document.filePath)
                val sanitizedNewName = if (newName.endsWith(".pdf") || newName.endsWith(".txt")) {
                    newName
                } else {
                    if (document.category == DocumentCategory.OCR_TEXT.name) "$newName.txt" else "$newName.pdf"
                }

                val newFile = File(oldFile.parentFile, sanitizedNewName)
                if (oldFile.exists() && oldFile.renameTo(newFile)) {
                    documentDao.updateDocument(
                        document.copy(
                            fileName = sanitizedNewName,
                            filePath = newFile.absolutePath,
                            modifiedAt = System.currentTimeMillis()
                        )
                    )
                    true
                } else {
                    // Update database name anyway if file wasn't renamed or already exists
                    documentDao.updateDocument(
                        document.copy(
                            fileName = sanitizedNewName,
                            modifiedAt = System.currentTimeMillis()
                        )
                    )
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun deleteDocument(document: DocumentEntity): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(document.filePath)
                if (file.exists()) {
                    file.delete()
                }
                documentDao.deleteDocument(document)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
