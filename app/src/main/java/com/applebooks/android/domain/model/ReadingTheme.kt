package com.applebooks.android.domain.model

import androidx.compose.ui.graphics.Color

enum class ReadingTheme(
    val backgroundColor: Color,
    val textColor: Color,
    val displayName: String
) {
    WHITE(Color(0xFFFFFFFF), Color(0xFF000000), "White"),
    SEPIA(Color(0xFFF4ECD8), Color(0xFF5B4636), "Sepia"),
    GRAY(Color(0xFF3D3D3D), Color(0xFFD4D4D4), "Gray"),
    DARK(Color(0xFF1C1C1E), Color(0xFFE5E5E5), "Dark")
}
