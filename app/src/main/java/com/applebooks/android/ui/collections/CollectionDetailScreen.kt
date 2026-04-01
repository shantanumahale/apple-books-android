package com.applebooks.android.ui.collections

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.applebooks.android.domain.model.Book
import com.applebooks.android.domain.model.BookFormat
import com.applebooks.android.ui.components.CoverImage
import com.applebooks.android.ui.navigation.Screen
import com.applebooks.android.ui.theme.AppleBlue
import com.applebooks.android.ui.theme.AppleGray
import com.applebooks.android.ui.theme.AppleGray5
import com.applebooks.android.ui.theme.AppleRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    navController: NavController,
    viewModel: CollectionDetailViewModel = hiltViewModel()
) {
    val collectionName by viewModel.collectionName.collectAsState()
    val books by viewModel.books.collectAsState()
    var bookToRemove by remember { mutableStateOf<Book?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = collectionName,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppleBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (books.isEmpty()) {
            EmptyCollectionDetailState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            CollectionBooksGrid(
                books = books,
                onBookClick = { book -> navigateToReader(navController, book) },
                onBookLongPress = { book -> bookToRemove = book },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }

    if (bookToRemove != null) {
        AlertDialog(
            onDismissRequest = { bookToRemove = null },
            title = {
                Text(
                    text = "Remove from Collection",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text("Remove \"${bookToRemove!!.title}\" from this collection?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeBookFromCollection(bookToRemove!!.id)
                        bookToRemove = null
                    }
                ) {
                    Text(text = "Remove", color = AppleRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToRemove = null }) {
                    Text(text = "Cancel", color = AppleBlue)
                }
            },
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionBooksGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onBookLongPress: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = books,
            key = { it.id }
        ) { book ->
            CollectionBookItem(
                book = book,
                onClick = { onBookClick(book) },
                onLongPress = { onBookLongPress(book) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionBookItem(
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

@Composable
private fun EmptyCollectionDetailState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AppleGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No books in this collection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add books from your library",
                style = MaterialTheme.typography.bodyMedium,
                color = AppleGray
            )
        }
    }
}

private fun navigateToReader(navController: NavController, book: Book) {
    val route = when (book.format) {
        BookFormat.PDF -> Screen.PdfReader.createRoute(book.id)
        BookFormat.EPUB -> Screen.EpubReader.createRoute(book.id)
    }
    navController.navigate(route)
}
