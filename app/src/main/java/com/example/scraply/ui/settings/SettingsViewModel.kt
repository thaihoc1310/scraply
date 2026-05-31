package com.example.scraply.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.preferences.AppLanguage
import com.example.scraply.data.preferences.AppPreferencesRepository
import com.example.scraply.data.preferences.AppThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = appPreferencesRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppThemeMode.SYSTEM
        )

    val appLanguage: StateFlow<AppLanguage> = appPreferencesRepository.appLanguage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.ENGLISH
        )

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            appPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            appPreferencesRepository.setAppLanguage(language)
        }
    }
}
