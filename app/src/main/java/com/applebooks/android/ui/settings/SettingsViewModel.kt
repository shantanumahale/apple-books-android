package com.applebooks.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applebooks.android.data.local.preferences.UserPreferencesDataStore
import com.applebooks.android.domain.model.PageTurnEffect
import com.applebooks.android.domain.model.ReaderFont
import com.applebooks.android.domain.model.ReadingTheme
import com.applebooks.android.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = userPreferencesDataStore
        .preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun updateReadingTheme(theme: ReadingTheme) {
        viewModelScope.launch {
            userPreferencesDataStore.updateReadingTheme(theme)
        }
    }

    fun updatePageTurnEffect(effect: PageTurnEffect) {
        viewModelScope.launch {
            userPreferencesDataStore.updatePageTurnEffect(effect)
        }
    }

    fun updateReaderFont(font: ReaderFont) {
        viewModelScope.launch {
            userPreferencesDataStore.updateReaderFont(font)
        }
    }

    fun updateFontSize(size: Int) {
        val clamped = size.coerceIn(12, 32)
        viewModelScope.launch {
            userPreferencesDataStore.updateFontSize(clamped)
        }
    }
}
