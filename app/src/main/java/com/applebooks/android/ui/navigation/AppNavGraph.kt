package com.applebooks.android.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.applebooks.android.domain.model.Book
import com.applebooks.android.domain.model.BookFormat
import com.applebooks.android.domain.repository.CollectionRepository
import com.applebooks.android.reader.epub.EpubReaderScreen
import com.applebooks.android.reader.pdf.PdfReaderScreen
import com.applebooks.android.ui.bookdetail.AddToCollectionDialog
import com.applebooks.android.ui.bookdetail.BookDetailSheet
import com.applebooks.android.ui.collections.CollectionDetailScreen
import com.applebooks.android.ui.collections.CollectionsScreen
import com.applebooks.android.ui.library.LibraryScreen
import com.applebooks.android.ui.library.LibraryViewModel
import com.applebooks.android.ui.settings.SettingsScreen
import com.applebooks.android.ui.theme.AppleBlue
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    initialBookId: Long? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isReaderScreen = currentRoute?.startsWith("reader/") == true
    val isDetailScreen = currentRoute?.startsWith("collection/") == true

    val showBottomBar = !isReaderScreen && !isDetailScreen

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = androidx.compose.ui.unit.dp.times(0)
                ) {
                    BottomNavItem.entries.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppleBlue,
                                selectedTextColor = AppleBlue,
                                indicatorColor = AppleBlue.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(
                bottom = if (showBottomBar) innerPadding.calculateBottomPadding()
                else androidx.compose.ui.unit.dp.times(0)
            )
        ) {
            composable(Screen.Library.route) {
                var selectedBook by remember { mutableStateOf<Book?>(null) }
                var showAddToCollection by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                LibraryScreen(
                    onBookClick = { book ->
                        val route = when (book.format) {
                            BookFormat.PDF -> Screen.PdfReader.createRoute(book.id)
                            BookFormat.EPUB -> Screen.EpubReader.createRoute(book.id)
                        }
                        navController.navigate(route)
                    },
                    onBookLongPress = { book ->
                        selectedBook = book
                    }
                )

                selectedBook?.let { book ->
                    val libraryViewModel: LibraryViewModel = hiltViewModel()
                    BookDetailSheet(
                        book = book,
                        onDismiss = { selectedBook = null },
                        onOpenBook = {
                            selectedBook = null
                            val route = when (book.format) {
                                BookFormat.PDF -> Screen.PdfReader.createRoute(book.id)
                                BookFormat.EPUB -> Screen.EpubReader.createRoute(book.id)
                            }
                            navController.navigate(route)
                        },
                        onAddToCollection = {
                            showAddToCollection = true
                        },
                        onRemoveFromLibrary = {
                            scope.launch {
                                libraryViewModel.deleteBook(book.id)
                            }
                            selectedBook = null
                        }
                    )

                    if (showAddToCollection) {
                        val collectionsViewModel: com.applebooks.android.ui.collections.CollectionsViewModel = hiltViewModel()
                        val collections by collectionsViewModel.collections.collectAsState()
                        var currentCollectionIds by remember { mutableStateOf<List<Long>>(emptyList()) }

                        LaunchedCollectionIds(book.id, collectionsViewModel.collectionRepository) {
                            currentCollectionIds = it
                        }

                        AddToCollectionDialog(
                            collections = collections,
                            currentCollectionIds = currentCollectionIds,
                            onDismiss = { showAddToCollection = false },
                            onToggleCollection = { collectionId, add ->
                                scope.launch {
                                    if (add) {
                                        collectionsViewModel.collectionRepository.addBookToCollection(book.id, collectionId)
                                    } else {
                                        collectionsViewModel.collectionRepository.removeBookFromCollection(book.id, collectionId)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            composable(Screen.Collections.route) {
                CollectionsScreen(
                    onCollectionClick = { collectionId ->
                        navController.navigate(Screen.CollectionDetail.createRoute(collectionId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.CollectionDetail.route,
                arguments = listOf(navArgument("collectionId") { type = NavType.LongType })
            ) {
                CollectionDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onBookClick = { book ->
                        val route = when (book.format) {
                            BookFormat.PDF -> Screen.PdfReader.createRoute(book.id)
                            BookFormat.EPUB -> Screen.EpubReader.createRoute(book.id)
                        }
                        navController.navigate(route)
                    }
                )
            }

            composable(
                route = Screen.PdfReader.route,
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) {
                PdfReaderScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EpubReader.route,
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) {
                EpubReaderScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }

    // Handle initial book navigation
    if (initialBookId != null) {
        val libraryViewModel: LibraryViewModel = hiltViewModel()
        val scope = rememberCoroutineScope()
        androidx.compose.runtime.LaunchedEffect(initialBookId) {
            val book = libraryViewModel.getBookById(initialBookId)
            if (book != null) {
                val route = when (book.format) {
                    BookFormat.PDF -> Screen.PdfReader.createRoute(book.id)
                    BookFormat.EPUB -> Screen.EpubReader.createRoute(book.id)
                }
                navController.navigate(route)
            }
        }
    }
}

@Composable
private fun LaunchedCollectionIds(
    bookId: Long,
    collectionRepository: CollectionRepository,
    onResult: (List<Long>) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(bookId) {
        val ids = collectionRepository.getCollectionIdsForBook(bookId)
        onResult(ids)
    }
}
