package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DocumentCategory(val displayName: String) {
    ALL("All"),
    SCANNED("Scanned"),
    IMAGE_TO_PDF("Created"),
    COMPRESSED("Compressed"),
    MERGED("Merged"),
    SPLIT("Split"),
    SIGNED("Signed"),
    PROTECTED("Protected"),
    OCR_TEXT("OCR Texts")
}

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 1,
    val category: String = DocumentCategory.SCANNED.name
)
