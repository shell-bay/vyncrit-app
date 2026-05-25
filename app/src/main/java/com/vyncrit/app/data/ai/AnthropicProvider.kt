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
class AnthropicProvider @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json
) : AiProvider {

    override val type = AiProviderType.ANTHROPIC

    override val availableModels = listOf(
        AiModel("claude-sonnet-4-20250514", "Claude Sonnet 4", AiProviderType.ANTHROPIC, isDefault = true),
        AiModel("claude-opus-4-20250514", "Claude Opus 4", AiProviderType.ANTHROPIC),
        AiModel("claude-haiku-3-5-20241022", "Claude Haiku 3.5", AiProviderType.ANTHROPIC)
    )

    override val defaultModel = availableModels.first { it.isDefault }

    override suspend fun generateCode(
        prompt: String,
        model: AiModel,
        apiKey: String,
        systemPrompt: String
    ): Result<CodeGenerationResult> {
        return try {
            val response = httpClient.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(
                    MessageRequest(
                        model = model.id,
                        system = systemPrompt,
                        messages = listOf(UserMessage("user", prompt)),
                        maxTokens = model.maxTokens
                    )
                )
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Anthropic error: ${response.bodyAsText()}"))
            }
            val msgResponse = response.body<MessageResponse>()
            val content = msgResponse.content.firstOrNull()?.text ?: ""
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
        val response = httpClient.post("https://api.anthropic.com/v1/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(
                MessageRequest(
                    model = model.id,
                    system = systemPrompt,
                    messages = messages.map { UserMessage("user", it.content) },
                    maxTokens = model.maxTokens,
                    stream = true
                )
            )
        }
        val text = response.bodyAsText()
        text.lines().forEach { line ->
            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ")
                try {
                    val chunk = json.decodeFromString<StreamEvent>(data)
                    if (chunk.type == "content_block_delta") {
                        emit(chunk.delta?.text ?: "")
                    }
                } catch (_: Exception) {}
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
            val response = httpClient.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(
                    MessageRequest(
                        model = model.id,
                        system = systemPrompt,
                        messages = messages.map { UserMessage("user", it.content) },
                        maxTokens = model.maxTokens
                    )
                )
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Anthropic error: ${response.bodyAsText()}"))
            }
            val msgResponse = response.body<MessageResponse>()
            val content = msgResponse.content.firstOrNull()?.text ?: ""
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Serializable
    data class MessageRequest(
        val model: String,
        val system: String = "",
        val messages: List<UserMessage>,
        val maxTokens: Int = 4096,
        val stream: Boolean = false
    )

    @Serializable
    data class UserMessage(
        val role: String,
        val content: String
    )

    @Serializable
    data class MessageResponse(
        val content: List<ContentBlock>
    )

    @Serializable
    data class ContentBlock(
        val text: String
    )

    @Serializable
    data class StreamEvent(
        val type: String = "",
        val delta: DeltaBlock? = null
    )

    @Serializable
    data class DeltaBlock(
        val text: String? = null
    )
}
