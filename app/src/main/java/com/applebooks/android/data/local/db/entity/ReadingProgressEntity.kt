package com.applebooks.android.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReadingProgressEntity(
    @PrimaryKey
    val bookId: Long,
    val locatorJson: String = "",
    val progressPercent: Float = 0f,
    val lastReadAt: Long = System.currentTimeMillis()
)
