package `in`.mahato.bytetools

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.mahato.bytetools.data.local.PreferenceManager
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferenceManager: PreferenceManager
) : ViewModel() {
    val themeMode = preferenceManager.themeMode
    val dynamicColor = preferenceManager.dynamicColor
}
