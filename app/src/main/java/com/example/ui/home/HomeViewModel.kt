package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DocumentEntity
import com.example.data.local.DocumentRepository
import com.example.data.preferences.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DocumentRepository
    private val preferences = AppPreferences(application)

    val recentDocuments: StateFlow<List<DocumentEntity>>
    val isProUser: StateFlow<Boolean> = preferences.isProUser

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DocumentRepository(database.documentDao(), application)
        recentDocuments = repository.recentDocuments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun renameDocument(document: DocumentEntity, newName: String) {
        viewModelScope.launch {
            repository.renameDocument(document, newName)
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(document)
        }
    }
}
