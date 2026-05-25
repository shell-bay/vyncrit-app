package com.vyncrit.app.data.project

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val packageName: String = "",
    val appIconUrl: String = "",
    val minSdk: Int = 26,
    val targetSdk: Int = 35,
    val status: ProjectStatus = ProjectStatus.IDLE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val ownerId: String = "",
    val buildCount: Int = 0,
    val lastBuildId: String = ""
)

@Serializable
enum class ProjectStatus {
    IDLE, GENERATING, BUILDING, SUCCESS, FAILED
}

@Serializable
data class BuildLog(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val message: String = ""
)

@Serializable
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

@Serializable
data class BuildResult(
    val buildId: String = "",
    val success: Boolean = false,
    val apkUrl: String = "",
    val aabUrl: String = "",
    val appSizeBytes: Long = 0,
    val logs: List<BuildLog> = emptyList()
)
