package com.applebooks.android.ui.collections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applebooks.android.domain.model.Book
import com.applebooks.android.domain.repository.BookRepository
import com.applebooks.android.domain.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionDetailUiState(
    val collectionName: String = "",
    val books: List<Book> = emptyList()
)

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val collectionRepository: CollectionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val collectionId: Long = savedStateHandle.get<Long>("collectionId")
        ?: throw IllegalArgumentException("collectionId is required")

    private val _collectionName = MutableStateFlow("")
    val collectionName: StateFlow<String> = _collectionName.asStateFlow()

    val books: StateFlow<List<Book>> = bookRepository
        .getBooksInCollectionFlow(collectionId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            val collection = collectionRepository.getCollectionById(collectionId)
            _collectionName.value = collection?.name ?: "Collection"
        }
    }

    fun removeBookFromCollection(bookId: Long) {
        viewModelScope.launch {
            collectionRepository.removeBookFromCollection(bookId, collectionId)
        }
    }
}
