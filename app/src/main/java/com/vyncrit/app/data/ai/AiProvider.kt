package com.vyncrit.app.data.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
enum class AiProviderType(val displayName: String) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic"),
    GEMINI("Google Gemini"),
    DEEPSEEK("DeepSeek"),
    OLLAMA("Ollama (Local)"),
    OPENROUTER("OpenRouter")
}

@Serializable
data class AiModel(
    val id: String,
    val name: String,
    val provider: AiProviderType,
    val supportsStreaming: Boolean = true,
    val isDefault: Boolean = false,
    val maxTokens: Int = 4096
)

data class AiChatMessage(
    val role: AiChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AiChatRole {
    SYSTEM, USER, ASSISTANT
}

data class CodeGenerationResult(
    val code: String = "",
    val explanation: String = "",
    val filePaths: List<String> = emptyList()
)

interface AiProvider {
    val type: AiProviderType
    val availableModels: List<AiModel>
    val defaultModel: AiModel

    suspend fun generateCode(
        prompt: String,
        model: AiModel,
        apiKey: String,
        systemPrompt: String = DEFAULT_SYSTEM_PROMPT
    ): Result<CodeGenerationResult>

    suspend fun chatStream(
        messages: List<AiChatMessage>,
        model: AiModel,
        apiKey: String,
        systemPrompt: String = DEFAULT_SYSTEM_PROMPT
    ): Flow<String>

    suspend fun chat(
        messages: List<AiChatMessage>,
        model: AiModel,
        apiKey: String,
        systemPrompt: String = DEFAULT_SYSTEM_PROMPT
    ): Result<String>

    companion object {
        val DEFAULT_SYSTEM_PROMPT = """
You are Vyncr.it, an expert Android app builder AI assistant. 
You help users create professional Android applications using Kotlin and Jetpack Compose.
Generate complete, production-ready code following Material 3 design guidelines.
Always include proper error handling, null safety, and follow Clean Architecture patterns.
Use modern Android development practices: Hilt DI, Room Database, Retrofit/Ktor networking, 
Navigation Compose, Baseline Profiles, and R8 optimization.
Generate ONLY the source code files needed. Be concise but complete.
        """.trimIndent()
    }
}
