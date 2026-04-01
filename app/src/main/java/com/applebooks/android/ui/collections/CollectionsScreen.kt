package com.applebooks.android.ui.collections

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.applebooks.android.domain.model.Collection
import com.applebooks.android.ui.navigation.Screen
import com.applebooks.android.ui.theme.AppleBlue
import com.applebooks.android.ui.theme.AppleGray
import com.applebooks.android.ui.theme.AppleGray5
import com.applebooks.android.ui.theme.AppleGray6
import com.applebooks.android.ui.theme.AppleRed

@Composable
fun CollectionsScreen(
    navController: NavController,
    viewModel: CollectionsViewModel = hiltViewModel()
) {
    val collections by viewModel.collections.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val renameTarget by viewModel.renameTarget.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CollectionsTopBar(
            onNewCollectionClick = { viewModel.showCreateDialog() }
        )

        if (collections.isEmpty()) {
            EmptyCollectionsState(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        } else {
            CollectionsList(
                collections = collections,
                onCollectionClick = { collection ->
                    navController.navigate(Screen.CollectionDetail.createRoute(collection.id))
                },
                onCollectionLongPress = { collection ->
                    viewModel.showRenameDialog(collection)
                },
                onCollectionDelete = { collection ->
                    viewModel.deleteCollection(collection.id)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }
    }

    if (showCreateDialog) {
        CollectionNameDialog(
            title = "New Collection",
            initialName = "",
            confirmLabel = "Create",
            onConfirm = { name -> viewModel.createCollection(name) },
            onDismiss = { viewModel.dismissCreateDialog() }
        )
    }

    if (renameTarget != null) {
        CollectionNameDialog(
            title = "Rename Collection",
            initialName = renameTarget!!.name,
            confirmLabel = "Save",
            onConfirm = { name -> viewModel.renameCollection(renameTarget!!.id, name) },
            onDismiss = { viewModel.dismissRenameDialog() }
        )
    }
}

@Composable
private fun CollectionsTopBar(
    onNewCollectionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Collections",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        IconButton(onClick = onNewCollectionClick) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "New Collection",
                tint = AppleBlue
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CollectionsList(
    collections: List<Collection>,
    onCollectionClick: (Collection) -> Unit,
    onCollectionLongPress: (Collection) -> Unit,
    onCollectionDelete: (Collection) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(
            items = collections,
            key = { it.id }
        ) { collection ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { dismissValue ->
                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                        onCollectionDelete(collection)
                        true
                    } else {
                        false
                    }
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val color by animateColorAsState(
                        targetValue = when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.EndToStart -> AppleRed
                            else -> Color.Transparent
                        },
                        label = "swipe_bg"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Color.White
                            )
                        }
                    }
                },
                enableDismissFromStartToEnd = false,
                modifier = Modifier.animateItemPlacement()
            ) {
                CollectionCard(
                    collection = collection,
                    onClick = { onCollectionClick(collection) },
                    onLongPress = { onCollectionLongPress(collection) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionCard(
    collection: Collection,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = AppleBlue,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${collection.bookCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = AppleGray,
                modifier = Modifier.padding(end = 4.dp)
            )

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AppleGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyCollectionsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CollectionsBookmark,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AppleGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No collections yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap the + button to create a collection",
                style = MaterialTheme.typography.bodyMedium,
                color = AppleGray
            )
        }
    }
}

@Composable
private fun CollectionNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Collection name") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppleGray6,
                    unfocusedContainerColor = AppleGray6,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AppleBlue
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(
                    text = confirmLabel,
                    color = if (name.isNotBlank()) AppleBlue else AppleGray
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = AppleBlue)
            }
        },
        shape = RoundedCornerShape(14.dp)
    )
}
