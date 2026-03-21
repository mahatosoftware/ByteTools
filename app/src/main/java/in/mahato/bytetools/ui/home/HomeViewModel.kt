package `in`.mahato.bytetools.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.mahato.bytetools.data.local.PreferenceManager
import `in`.mahato.bytetools.ui.navigation.Screen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import `in`.mahato.bytetools.utils.AdManager
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    val adManager: AdManager
) : ViewModel() {

    val recentTools: StateFlow<List<String>> = preferenceManager.recentToolsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val mostUsedTool: StateFlow<String> = preferenceManager.mostUsedToolFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun addRecentTool(route: String) {
        viewModelScope.launch {
            preferenceManager.addRecentTool(route)
        }
    }
}
