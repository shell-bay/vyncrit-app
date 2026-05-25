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
class GeminiProvider @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json
) : AiProvider {

    override val type = AiProviderType.GEMINI

    override val availableModels = listOf(
        AiModel("gemini-2.5-pro", "Gemini 2.5 Pro", AiProviderType.GEMINI, isDefault = true),
        AiModel("gemini-2.5-flash", "Gemini 2.5 Flash", AiProviderType.GEMINI),
        AiModel("gemini-2.0-flash", "Gemini 2.0 Flash", AiProviderType.GEMINI)
    )

    override val defaultModel = availableModels.first { it.isDefault }

    override suspend fun generateCode(
        prompt: String,
        model: AiModel,
        apiKey: String,
        systemPrompt: String
    ): Result<CodeGenerationResult> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.id}:generateContent?key=$apiKey"
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(
                    GenerateRequest(
                        systemInstruction = Part(systemPrompt),
                        contents = listOf(Content(listOf(Part(prompt))))
                    )
                )
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Gemini error: ${response.bodyAsText()}"))
            }
            val geminiResponse = response.body<GenerateResponse>()
            val text = geminiResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text ?: ""
            Result.success(CodeGenerationResult(code = text))
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
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.id}:streamGenerateContent?key=$apiKey&alt=sse"
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(
                GenerateRequest(
                    systemInstruction = Part(systemPrompt),
                    contents = messages.map { Content(listOf(Part(it.content))) }
                )
            )
        }
        val text = response.bodyAsText()
        text.lines().forEach { line ->
            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ")
                try {
                    val chunk = json.decodeFromString<GenerateResponse>(data)
                    emit(chunk.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "")
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
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.id}:generateContent?key=$apiKey"
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(
                    GenerateRequest(
                        systemInstruction = Part(systemPrompt),
                        contents = messages.map { Content(listOf(Part(it.content))) }
                    )
                )
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Gemini error: ${response.bodyAsText()}"))
            }
            val geminiResponse = response.body<GenerateResponse>()
            val text = geminiResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text ?: ""
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Serializable
    data class GenerateRequest(
        val systemInstruction: Part? = null,
        val contents: List<Content> = emptyList()
    )

    @Serializable
    data class Part(val text: String)

    @Serializable
    data class Content(val parts: List<Part>)

    @Serializable
    data class GenerateResponse(
        val candidates: List<Candidate>? = null
    )

    @Serializable
    data class Candidate(
        val content: Content? = null
    )
}
