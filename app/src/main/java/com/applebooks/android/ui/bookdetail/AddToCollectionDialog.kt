package com.applebooks.android.ui.bookdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.applebooks.android.domain.model.Collection
import com.applebooks.android.ui.theme.AppleBlue

@Composable
fun AddToCollectionDialog(
    collections: List<Collection>,
    currentCollectionIds: List<Long>,
    onDismiss: () -> Unit,
    onToggleCollection: (collectionId: Long, add: Boolean) -> Unit
) {
    var selectedIds by remember { mutableStateOf(currentCollectionIds.toSet()) }

    LaunchedEffect(currentCollectionIds) {
        selectedIds = currentCollectionIds.toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add to Collection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (collections.isEmpty()) {
                Column {
                    Text(
                        "No collections yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Create a collection from the Collections tab first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(collections) { collection ->
                        val isSelected = collection.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        selectedIds = selectedIds - collection.id
                                        onToggleCollection(collection.id, false)
                                    } else {
                                        selectedIds = selectedIds + collection.id
                                        onToggleCollection(collection.id, true)
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (isSelected) AppleBlue else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = collection.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = AppleBlue)
            }
        }
    )
}
