package com.applebooks.android.domain.model

data class UserPreferences(
    val readingTheme: ReadingTheme = ReadingTheme.WHITE,
    val pageTurnEffect: PageTurnEffect = PageTurnEffect.SLIDE,
    val readerFont: ReaderFont = ReaderFont.SYSTEM_SERIF,
    val fontSize: Int = 18,
    val libraryGridView: Boolean = true
)
