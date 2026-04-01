package com.applebooks.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.applebooks.android.domain.model.PageTurnEffect
import com.applebooks.android.domain.model.ReaderFont
import com.applebooks.android.domain.model.ReadingTheme
import com.applebooks.android.domain.model.UserPreferences
import com.applebooks.android.ui.theme.AppleBlue
import com.applebooks.android.ui.theme.AppleGray
import com.applebooks.android.ui.theme.AppleGray3
import com.applebooks.android.ui.theme.AppleGray5
import com.applebooks.android.ui.theme.AppleGray6

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.preferences.collectAsState()
    var showFontPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SettingsTopBar()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            ReadingSection(
                preferences = preferences,
                onThemeSelected = viewModel::updateReadingTheme,
                onPageTurnEffectSelected = viewModel::updatePageTurnEffect
            )

            Spacer(modifier = Modifier.height(24.dp))

            TypographySection(
                preferences = preferences,
                onFontPickerClick = { showFontPicker = true },
                onFontSizeChange = viewModel::updateFontSize
            )

            Spacer(modifier = Modifier.height(24.dp))

            AboutSection()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showFontPicker) {
        FontPickerDialog(
            currentFont = preferences.readerFont,
            onFontSelected = { font ->
                viewModel.updateReaderFont(font)
                showFontPicker = false
            },
            onDismiss = { showFontPicker = false }
        )
    }
}

@Composable
private fun SettingsTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = AppleGray,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 4.dp)
    )
}

@Composable
private fun ReadingSection(
    preferences: UserPreferences,
    onThemeSelected: (ReadingTheme) -> Unit,
    onPageTurnEffectSelected: (PageTurnEffect) -> Unit
) {
    SectionHeader("Reading")

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Reading Theme",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ReadingTheme.entries.forEach { theme ->
                        ThemeCircle(
                            theme = theme,
                            isSelected = preferences.readingTheme == theme,
                            onClick = { onThemeSelected(theme) }
                        )
                    }
                }
            }

            HorizontalDivider(
                color = AppleGray5,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Page Turn Effect",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                SegmentedButton(
                    options = PageTurnEffect.entries.map {
                        when (it) {
                            PageTurnEffect.CURL -> "Curl"
                            PageTurnEffect.SLIDE -> "Slide"
                        }
                    },
                    selectedIndex = PageTurnEffect.entries.indexOf(preferences.pageTurnEffect),
                    onSelectionChanged = { index ->
                        onPageTurnEffectSelected(PageTurnEffect.entries[index])
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeCircle(
    theme: ReadingTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(theme.backgroundColor)
                .then(
                    if (theme == ReadingTheme.WHITE) {
                        Modifier.border(1.dp, AppleGray3, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = theme.textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = theme.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) AppleBlue else AppleGray,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SegmentedButton(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppleGray6)
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(MaterialTheme.colorScheme.surface)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelectionChanged(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TypographySection(
    preferences: UserPreferences,
    onFontPickerClick: () -> Unit,
    onFontSizeChange: (Int) -> Unit
) {
    SectionHeader("Typography")

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onFontPickerClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Font Family",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = preferences.readerFont.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppleGray
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = AppleGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(
                color = AppleGray5,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Font Size",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onFontSizeChange(preferences.fontSize - 1) },
                        enabled = preferences.fontSize > 12,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (preferences.fontSize > 12) AppleGray6
                                else AppleGray6.copy(alpha = 0.5f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Decrease font size",
                            tint = if (preferences.fontSize > 12) AppleBlue else AppleGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "${preferences.fontSize}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp)
                    )

                    IconButton(
                        onClick = { onFontSizeChange(preferences.fontSize + 1) },
                        enabled = preferences.fontSize < 32,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (preferences.fontSize < 32) AppleGray6
                                else AppleGray6.copy(alpha = 0.5f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase font size",
                            tint = if (preferences.fontSize < 32) AppleBlue else AppleGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = AppleGray5,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppleGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(preferences.readingTheme.backgroundColor)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "The quick brown fox jumps over the lazy dog. Reading should be a comfortable and enjoyable experience.",
                        color = preferences.readingTheme.textColor,
                        fontSize = preferences.fontSize.sp,
                        lineHeight = (preferences.fontSize * 1.5).sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSection() {
    SectionHeader("About")

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "App Version",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = AppleGray
            )
        }
    }
}

@Composable
private fun FontPickerDialog(
    currentFont: ReaderFont,
    onFontSelected: (ReaderFont) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Font Family",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                ReaderFont.entries.forEach { font ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFontSelected(font) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = font == currentFont,
                            onClick = { onFontSelected(font) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppleBlue,
                                unselectedColor = AppleGray
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = font.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done", color = AppleBlue)
            }
        },
        shape = RoundedCornerShape(14.dp)
    )
}
