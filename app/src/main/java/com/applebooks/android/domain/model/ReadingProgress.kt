package com.applebooks.android.domain.model

data class ReadingProgress(
    val bookId: Long,
    val locatorJson: String = "",
    val progressPercent: Float = 0f,
    val lastReadAt: Long = System.currentTimeMillis()
)
