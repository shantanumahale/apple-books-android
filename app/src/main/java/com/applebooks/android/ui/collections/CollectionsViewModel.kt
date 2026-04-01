package com.applebooks.android.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applebooks.android.domain.model.Collection
import com.applebooks.android.domain.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionsUiState(
    val collections: List<Collection> = emptyList(),
    val showCreateDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val renameTarget: Collection? = null
)

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    val collectionRepository: CollectionRepository
) : ViewModel() {

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _renameTarget = MutableStateFlow<Collection?>(null)
    val renameTarget: StateFlow<Collection?> = _renameTarget.asStateFlow()

    val collections: StateFlow<List<Collection>> = collectionRepository
        .getAllCollectionsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun dismissCreateDialog() {
        _showCreateDialog.value = false
    }

    fun showRenameDialog(collection: Collection) {
        _renameTarget.value = collection
    }

    fun dismissRenameDialog() {
        _renameTarget.value = null
    }

    fun createCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            collectionRepository.createCollection(name.trim())
            _showCreateDialog.value = false
        }
    }

    fun renameCollection(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            collectionRepository.renameCollection(id, name.trim())
            _renameTarget.value = null
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            collectionRepository.deleteCollection(id)
        }
    }
}
