package com.applebooks.android.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [Index(value = ["fileUri"], unique = true)]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String? = null,
    val fileUri: String,
    val coverCachePath: String? = null,
    val format: String, // "PDF" or "EPUB"
    val totalPages: Int? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null
)
