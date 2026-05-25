package com.vyncrit.app.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiProvider @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json
) : AiProvider {

    override val type = AiProviderType.OPENAI

    override val availableModels = listOf(
        AiModel("gpt-4o", "GPT-4o", AiProviderType.OPENAI, isDefault = true),
        AiModel("gpt-4o-mini", "GPT-4o Mini", AiProviderType.OPENAI),
        AiModel("o3-mini", "o3 Mini", AiProviderType.OPENAI),
        AiModel("gpt-4.1", "GPT-4.1", AiProviderType.OPENAI),
        AiModel("gpt-4.1-mini", "GPT-4.1 Mini", AiProviderType.OPENAI)
    )

    override val defaultModel = availableModels.first { it.isDefault }

    override suspend fun generateCode(
        prompt: String,
        model: AiModel,
        apiKey: String,
        systemPrompt: String
    ): Result<CodeGenerationResult> {
        return try {
            val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    ChatRequest(
                        model = model.id,
                        messages = listOf(
                            ChatMessage("system", systemPrompt),
                            ChatMessage("user", prompt)
                        ),
                        maxTokens = model.maxTokens,
                        temperature = 0.7
                    )
                )
            }
            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                return Result.failure(Exception("OpenAI API error: $errorBody"))
            }
            val chatResponse = response.body<ChatResponse>()
            val content = chatResponse.choices.firstOrNull()?.message?.content ?: ""
            Result.success(CodeGenerationResult(code = content))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun chatStream(
        messages: List<AiChatMessage>,
        model: AiModel,
        apiKey: String,
        systemPrompt: String
    ): Flow<String> = flow {
        val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(
                ChatRequest(
                    model = model.id,
                    messages = listOf(ChatMessage("system", systemPrompt)) +
                            messages.map { ChatMessage(it.role.name.lowercase(), it.content) },
                    maxTokens = model.maxTokens,
                    temperature = 0.7,
                    stream = true
                )
            )
        }
        val text = response.bodyAsText()
        text.lines().forEach { line ->
            if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                val data = line.removePrefix("data: ")
                try {
                    val chunk = json.decodeFromString<ChunkResponse>(data)
                    emit(chunk.choices.firstOrNull()?.delta?.content ?: "")
                } catch (_: Exception) { }
            }
        }
    }

    override suspend fun chat(
        messages: List<AiChatMessage>,
        model: AiModel,
        apiKey: String,
        systemPrompt: String
    ): Result<String> {
        return try {
            val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    ChatRequest(
                        model = model.id,
                        messages = listOf(ChatMessage("system", systemPrompt)) +
                                messages.map { ChatMessage(it.role.name.lowercase(), it.content) },
                        maxTokens = model.maxTokens,
                        temperature = 0.7
                    )
                )
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("OpenAI error: ${response.bodyAsText()}"))
            }
            val chatResponse = response.body<ChatResponse>()
            val content = chatResponse.choices.firstOrNull()?.message?.content ?: ""
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Serializable
    data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val maxTokens: Int = 4096,
        val temperature: Double = 0.7,
        val stream: Boolean = false
    )

    @Serializable
    data class ChatMessage(
        val role: String,
        val content: String
    )

    @Serializable
    data class ChatResponse(
        val choices: List<Choice>
    )

    @Serializable
    data class Choice(
        val message: Message
    )

    @Serializable
    data class Message(
        val content: String
    )

    @Serializable
    data class ChunkResponse(
        val choices: List<ChunkChoice>
    )

    @Serializable
    data class ChunkChoice(
        val delta: Delta
    )

    @Serializable
    data class Delta(
        val content: String? = null
    )
}
