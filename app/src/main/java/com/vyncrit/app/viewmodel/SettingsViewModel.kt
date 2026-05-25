package com.vyncrit.app.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyncrit.app.data.ai.AiProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class SettingsUiState(
    val openAiKey: String = "",
    val anthropicKey: String = "",
    val geminiKey: String = "",
    val deepSeekKey: String = "",
    val preferredProvider: AiProviderType = AiProviderType.OPENAI,
    val buildServerUrl: String = "https://build.vyncrit.app",
    val isDarkMode: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    companion object {
        private val OPENAI_KEY = stringPreferencesKey("openai_api_key")
        private val ANTHROPIC_KEY = stringPreferencesKey("anthropic_api_key")
        private val GEMINI_KEY = stringPreferencesKey("gemini_api_key")
        private val DEEPSEEK_KEY = stringPreferencesKey("deepseek_api_key")
        private val PREFERRED_PROVIDER = stringPreferencesKey("preferred_provider")
        private val BUILD_SERVER_URL = stringPreferencesKey("build_server_url")
        private val DARK_MODE = stringPreferencesKey("dark_mode")
    }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val dataStore = context.settingsDataStore
            val prefs = dataStore.data.first()
            _uiState.value = _uiState.value.copy(
                openAiKey = prefs[OPENAI_KEY] ?: "",
                anthropicKey = prefs[ANTHROPIC_KEY] ?: "",
                geminiKey = prefs[GEMINI_KEY] ?: "",
                deepSeekKey = prefs[DEEPSEEK_KEY] ?: "",
                preferredProvider = try {
                    AiProviderType.valueOf(prefs[PREFERRED_PROVIDER] ?: AiProviderType.OPENAI.name)
                } catch (_: Exception) { AiProviderType.OPENAI },
                buildServerUrl = prefs[BUILD_SERVER_URL] ?: "https://build.vyncrit.app",
                isDarkMode = prefs[DARK_MODE]?.toBoolean() ?: false
            )
        }
    }

    fun updateOpenAiKey(key: String) {
        _uiState.value = _uiState.value.copy(openAiKey = key)
    }

    fun updateAnthropicKey(key: String) {
        _uiState.value = _uiState.value.copy(anthropicKey = key)
    }

    fun updateGeminiKey(key: String) {
        _uiState.value = _uiState.value.copy(geminiKey = key)
    }

    fun updateDeepSeekKey(key: String) {
        _uiState.value = _uiState.value.copy(deepSeekKey = key)
    }

    fun updatePreferredProvider(provider: AiProviderType) {
        _uiState.value = _uiState.value.copy(preferredProvider = provider)
    }

    fun updateBuildServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(buildServerUrl = url)
    }

    fun toggleDarkMode() {
        _uiState.value = _uiState.value.copy(isDarkMode = !_uiState.value.isDarkMode)
    }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val dataStore = context.settingsDataStore
            dataStore.edit { prefs ->
                prefs[OPENAI_KEY] = _uiState.value.openAiKey
                prefs[ANTHROPIC_KEY] = _uiState.value.anthropicKey
                prefs[GEMINI_KEY] = _uiState.value.geminiKey
                prefs[DEEPSEEK_KEY] = _uiState.value.deepSeekKey
                prefs[PREFERRED_PROVIDER] = _uiState.value.preferredProvider.name
                prefs[BUILD_SERVER_URL] = _uiState.value.buildServerUrl
                prefs[DARK_MODE] = _uiState.value.isDarkMode.toString()
            }
            _uiState.value = _uiState.value.copy(isSaving = false, message = "Settings saved")
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
