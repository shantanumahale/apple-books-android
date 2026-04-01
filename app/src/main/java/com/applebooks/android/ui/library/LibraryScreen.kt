package com.applebooks.android.ui.library

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.applebooks.android.domain.model.Book
import com.applebooks.android.domain.model.BookFormat
import com.applebooks.android.ui.components.CoverImage
import com.applebooks.android.ui.theme.AppleBlue
import com.applebooks.android.ui.theme.AppleGray
import com.applebooks.android.ui.theme.AppleGray5

@Composable
fun LibraryScreen(
    onBookClick: (Book) -> Unit,
    onBookLongPress: (Book) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isAddingBook by viewModel.isAddingBook.collectAsState()
    val context = LocalContext.current

    var isSearchVisible by rememberSaveable { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri)
            viewModel.addBookFromUri(uri, mimeType) { bookId, format ->
                if (bookId != null && format != null) {
                    val book = Book(
                        id = bookId,
                        title = "",
                        fileUri = uri.toString(),
                        format = format
                    )
                    onBookClick(book)
                } else {
                    Toast.makeText(context, "Could not open file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    filePickerLauncher.launch(
                        arrayOf("application/pdf", "application/epub+zip")
                    )
                },
                containerColor = AppleBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add book"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LibraryTopBar(
                isGridView = uiState.isGridView,
                isSearchVisible = isSearchVisible,
                searchQuery = searchQuery,
                onSearchToggle = {
                    isSearchVisible = !isSearchVisible
                    if (!isSearchVisible) {
                        viewModel.onSearchQueryChanged("")
                    }
                },
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onToggleGridView = viewModel::toggleGridView
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = AppleBlue
                        )
                    }
                    uiState.books.isEmpty() -> {
                        EmptyLibraryState(
                            isSearching = searchQuery.isNotBlank(),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        Crossfade(
                            targetState = uiState.isGridView,
                            label = "library_view_mode"
                        ) { isGrid ->
                            if (isGrid) {
                                LibraryGrid(
                                    books = uiState.books,
                                    onBookClick = onBookClick,
                                    onBookLongPress = onBookLongPress
                                )
                            } else {
                                LibraryList(
                                    books = uiState.books,
                                    onBookClick = onBookClick,
                                    onBookLongPress = onBookLongPress
                                )
                            }
                        }
                    }
                }

                if (isAddingBook) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppleBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryTopBar(
    isGridView: Boolean,
    isSearchVisible: Boolean,
    searchQuery: String,
    onSearchToggle: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleGridView: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Library",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row {
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = if (isSearchVisible) Icons.Outlined.Close else Icons.Filled.Search,
                        contentDescription = if (isSearchVisible) "Close search" else "Search",
                        tint = AppleBlue
                    )
                }
                IconButton(onClick = onToggleGridView) {
                    Icon(
                        imageVector = if (isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = if (isGridView) "List view" else "Grid view",
                        tint = AppleBlue
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isSearchVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = "Search books",
                        color = AppleGray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = AppleGray
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppleGray5,
                    unfocusedContainerColor = AppleGray5,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AppleBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onBookLongPress: (Book) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = books,
            key = { it.id }
        ) { book ->
            GridBookItem(
                book = book,
                onClick = { onBookClick(book) },
                onLongPress = { onBookLongPress(book) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridBookItem(
    book: Book,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        CoverImage(
            coverCachePath = book.coverCachePath,
            title = book.title,
            format = book.format,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = book.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (book.author != null) {
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = AppleGray
            )
        }

        if (book.progressPercent > 0f) {
            LinearProgressIndicator(
                progress = { book.progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = AppleBlue,
                trackColor = AppleGray5,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryList(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onBookLongPress: (Book) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = books,
            key = { it.id }
        ) { book ->
            ListBookItem(
                book = book,
                onClick = { onBookClick(book) },
                onLongPress = { onBookLongPress(book) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListBookItem(
    book: Book,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            coverCachePath = book.coverCachePath,
            title = book.title,
            format = book.format,
            modifier = Modifier
                .height(80.dp)
                .width(54.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (book.author != null) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = AppleGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                FormatBadge(format = book.format)

                if (book.progressPercent > 0f) {
                    LinearProgressIndicator(
                        progress = { book.progressPercent },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = AppleBlue,
                        trackColor = AppleGray5,
                        strokeCap = StrokeCap.Round
                    )

                    Text(
                        text = "${(book.progressPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleGray,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatBadge(format: BookFormat) {
    val backgroundColor = when (format) {
        BookFormat.PDF -> Color(0xFFFFEBEE)
        BookFormat.EPUB -> Color(0xFFE3F2FD)
    }
    val textColor = when (format) {
        BookFormat.PDF -> Color(0xFFCC2D37)
        BookFormat.EPUB -> Color(0xFF2E6BB5)
    }

    Text(
        text = format.name,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun EmptyLibraryState(
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = AppleGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSearching) "No results found" else "No books yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSearching) {
                "Try a different search term"
            } else {
                "Tap the + button to add a PDF or EPUB"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = AppleGray
        )
    }
}

