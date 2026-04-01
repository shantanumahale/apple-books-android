package com.applebooks.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Collections : Screen("collections")
    data object Settings : Screen("settings")
    data object CollectionDetail : Screen("collection/{collectionId}") {
        fun createRoute(collectionId: Long) = "collection/$collectionId"
    }
    data object PdfReader : Screen("reader/pdf/{bookId}") {
        fun createRoute(bookId: Long) = "reader/pdf/$bookId"
    }
    data object EpubReader : Screen("reader/epub/{bookId}") {
        fun createRoute(bookId: Long) = "reader/epub/$bookId"
    }
}

enum class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    LIBRARY(Screen.Library, "Library", Icons.Filled.Book, Icons.Outlined.Book),
    COLLECTIONS(Screen.Collections, "Collections", Icons.Filled.CollectionsBookmark, Icons.Outlined.CollectionsBookmark),
    SETTINGS(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}
