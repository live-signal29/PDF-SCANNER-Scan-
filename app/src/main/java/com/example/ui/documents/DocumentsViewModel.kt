package com.example.ui.documents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DocumentCategory
import com.example.data.local.DocumentEntity
import com.example.data.local.DocumentRepository
import com.example.data.preferences.AppPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DocumentRepository
    private val preferences = AppPreferences(application)

    val isProUser: StateFlow<Boolean> = preferences.isProUser

    private val _selectedCategory = MutableStateFlow(DocumentCategory.ALL)
    val selectedCategory: StateFlow<DocumentCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val documents: StateFlow<List<DocumentEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DocumentRepository(database.documentDao(), application)

        documents = _selectedCategory.flatMapLatest { category ->
            if (category == DocumentCategory.ALL) {
                repository.allDocuments
            } else {
                repository.getDocumentsByCategory(category.name)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun setCategory(category: DocumentCategory) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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
