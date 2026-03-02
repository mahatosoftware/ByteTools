package `in`.mahato.bytetools.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore = context.dataStore

    object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode") // "light", "dark", "system"
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val RECENT_TOOLS = stringPreferencesKey("recent_tools") // comma separated list
        val MOST_USED_TOOL = stringPreferencesKey("most_used_tool")
    }

    val themeMode: Flow<String> = dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }
    val dynamicColor: Flow<Boolean> = dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    val recentToolsFlow: Flow<List<String>> = dataStore.data.map { preferences ->
        val recentStr = preferences[Keys.RECENT_TOOLS] ?: ""
        if (recentStr.isEmpty()) emptyList() else recentStr.split(",")
    }

    val mostUsedToolFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.MOST_USED_TOOL] ?: ""
    }

    suspend fun addRecentTool(route: String) {
        dataStore.edit { preferences ->
            val currentRecentStr = preferences[Keys.RECENT_TOOLS] ?: ""
            val currentList = if (currentRecentStr.isEmpty()) mutableListOf() else currentRecentStr.split(",").toMutableList()
            
            // Remove if already exists to push it to the top
            currentList.remove(route)
            
            // Add to the top
            currentList.add(0, route)
            
            // Keep only the top 10
            val maxItems = 10
            val newList = if (currentList.size > maxItems) currentList.take(maxItems) else currentList

            preferences[Keys.RECENT_TOOLS] = newList.joinToString(",")
            preferences[Keys.MOST_USED_TOOL] = newList.firstOrNull() ?: ""
        }
    }
}
