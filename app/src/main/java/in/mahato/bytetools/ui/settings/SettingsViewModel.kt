package `in`.mahato.bytetools.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.mahato.bytetools.data.local.PreferenceManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val themeMode = preferenceManager.themeMode
    val dynamicColor = preferenceManager.dynamicColor

    fun cycleThemeMode(current: String) {
        val nextMode = when (current) {
            "system" -> "light"
            "light" -> "dark"
            "dark" -> "system"
            else -> "system"
        }
        viewModelScope.launch {
            preferenceManager.setThemeMode(nextMode)
        }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.setDynamicColor(enabled)
        }
    }
}
