package com.applebooks.android.domain.model

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String? = null,
    val fileUri: String,
    val coverCachePath: String? = null,
    val format: BookFormat,
    val totalPages: Int? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null,
    val progressPercent: Float = 0f
)
