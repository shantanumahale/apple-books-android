package com.applebooks.android.domain.model

data class Collection(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val bookCount: Int = 0
)
