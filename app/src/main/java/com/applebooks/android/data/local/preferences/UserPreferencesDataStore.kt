package com.applebooks.android.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.applebooks.android.domain.model.PageTurnEffect
import com.applebooks.android.domain.model.ReaderFont
import com.applebooks.android.domain.model.ReadingTheme
import com.applebooks.android.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val READING_THEME = stringPreferencesKey("reading_theme")
        val PAGE_TURN_EFFECT = stringPreferencesKey("page_turn_effect")
        val READER_FONT = stringPreferencesKey("reader_font")
        val FONT_SIZE = intPreferencesKey("font_size")
        val LIBRARY_GRID_VIEW = booleanPreferencesKey("library_grid_view")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            readingTheme = prefs[Keys.READING_THEME]?.let {
                try { ReadingTheme.valueOf(it) } catch (_: Exception) { ReadingTheme.WHITE }
            } ?: ReadingTheme.WHITE,
            pageTurnEffect = prefs[Keys.PAGE_TURN_EFFECT]?.let {
                try { PageTurnEffect.valueOf(it) } catch (_: Exception) { PageTurnEffect.SLIDE }
            } ?: PageTurnEffect.SLIDE,
            readerFont = prefs[Keys.READER_FONT]?.let {
                try { ReaderFont.valueOf(it) } catch (_: Exception) { ReaderFont.SYSTEM_SERIF }
            } ?: ReaderFont.SYSTEM_SERIF,
            fontSize = prefs[Keys.FONT_SIZE] ?: 18,
            libraryGridView = prefs[Keys.LIBRARY_GRID_VIEW] ?: true
        )
    }

    suspend fun updateReadingTheme(theme: ReadingTheme) {
        context.dataStore.edit { it[Keys.READING_THEME] = theme.name }
    }

    suspend fun updatePageTurnEffect(effect: PageTurnEffect) {
        context.dataStore.edit { it[Keys.PAGE_TURN_EFFECT] = effect.name }
    }

    suspend fun updateReaderFont(font: ReaderFont) {
        context.dataStore.edit { it[Keys.READER_FONT] = font.name }
    }

    suspend fun updateFontSize(size: Int) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = size }
    }

    suspend fun updateLibraryGridView(isGrid: Boolean) {
        context.dataStore.edit { it[Keys.LIBRARY_GRID_VIEW] = isGrid }
    }
}
