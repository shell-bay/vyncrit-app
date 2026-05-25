package com.vyncrit.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyncrit.app.data.ai.AiChatMessage
import com.vyncrit.app.data.ai.AiChatRole
import com.vyncrit.app.data.ai.AiModel
import com.vyncrit.app.data.ai.AiProvider
import com.vyncrit.app.data.ai.AiProviderType
import com.vyncrit.app.data.ai.AnthropicProvider
import com.vyncrit.app.data.ai.GeminiProvider
import com.vyncrit.app.data.ai.OpenAiProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val selectedProvider: AiProviderType = AiProviderType.OPENAI,
    val selectedModel: AiModel? = null,
    val error: String? = null,
    val projectId: String = "",
    val streamingContent: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val openAiProvider: OpenAiProvider,
    private val anthropicProvider: AnthropicProvider,
    private val geminiProvider: GeminiProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val providers: Map<AiProviderType, AiProvider> = mapOf(
        AiProviderType.OPENAI to openAiProvider,
        AiProviderType.ANTHROPIC to anthropicProvider,
        AiProviderType.GEMINI to geminiProvider
    )

    fun initialize(projectId: String) {
        _uiState.value = _uiState.value.copy(projectId = projectId)
        val provider = providers[_uiState.value.selectedProvider] ?: openAiProvider
        _uiState.value = _uiState.value.copy(
            selectedModel = provider.defaultModel,
            messages = listOf(
                AiChatMessage(
                    role = AiChatRole.ASSISTANT,
                    content = "Hello! I'm Vyncr.it AI. Describe the Android app you'd like to build, and I'll generate the code for you. What kind of app are you thinking of?"
                )
            )
        )
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun selectProvider(providerType: AiProviderType) {
        val provider = providers[providerType]
        _uiState.value = _uiState.value.copy(
            selectedProvider = providerType,
            selectedModel = provider?.defaultModel
        )
    }

    fun selectModel(model: AiModel) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun sendMessage() {
        val input = _uiState.value.inputText.trim()
        if (input.isEmpty()) return

        val userMessage = AiChatMessage(
            role = AiChatRole.USER,
            content = input
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            inputText = "",
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            val provider = providers[_uiState.value.selectedProvider] ?: openAiProvider
            val model = _uiState.value.selectedModel ?: provider.defaultModel

            try {
                val result = provider.chat(
                    messages = _uiState.value.messages,
                    model = model,
                    apiKey = "" // API key from settings
                )

                result.onSuccess { response ->
                    val assistantMessage = AiChatMessage(
                        role = AiChatRole.ASSISTANT,
                        content = response
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
                        isLoading = false
                    )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "AI Error: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
