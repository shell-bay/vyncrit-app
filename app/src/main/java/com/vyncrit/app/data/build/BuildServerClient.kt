package com.vyncrit.app.data.build

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.client.plugins.websocket.webSocket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildServerClient @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        private const val DEFAULT_SERVER_URL = "https://build.vyncrit.app"
    }

    suspend fun startBuild(
        projectName: String,
        packageName: String,
        codeFiles: Map<String, String>,
        serverUrl: String = DEFAULT_SERVER_URL
    ): Result<BuildSession> {
        return try {
            val response = httpClient.post(serverUrl) {
                contentType(ContentType.Application.Json)
                setBody(
                    BuildRequest(
                        projectName = projectName,
                        packageName = packageName,
                        codeFiles = codeFiles
                    )
                )
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Build server error: ${response.bodyAsText()}"))
            }
            Result.success(response.body<BuildSession>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamBuildLogs(sessionId: String, serverUrl: String = DEFAULT_SERVER_URL): Flow<String> = callbackFlow {
        val wsUrl = serverUrl.replace("http", "ws") + "/ws/$sessionId"
        httpClient.webSocket(urlString = wsUrl) {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    trySend(frame.readText())
                }
            }
        }
        awaitClose { }
    }

    @Serializable
    data class BuildRequest(
        val projectName: String,
        val packageName: String,
        val codeFiles: Map<String, String>
    )

    @Serializable
    data class BuildSession(
        val sessionId: String,
        val status: String
    )
}
