package com.applebooks.android.domain.model

enum class ReaderFont(val displayName: String, val cssName: String) {
    SYSTEM_SERIF("System Serif", "serif"),
    SYSTEM_SANS("System Sans", "sans-serif"),
    EB_GARAMOND("EB Garamond", "'EB Garamond'"),
    LIBRE_BASKERVILLE("Libre Baskerville", "'Libre Baskerville'"),
    MERRIWEATHER("Merriweather", "'Merriweather'"),
    OPEN_SANS("Open Sans", "'Open Sans'"),
    LATO("Lato", "'Lato'")
}
