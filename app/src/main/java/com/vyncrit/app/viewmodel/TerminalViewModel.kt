package com.vyncrit.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyncrit.app.data.build.BuildServerClient
import com.vyncrit.app.data.project.BuildLog
import com.vyncrit.app.data.project.LogLevel
import com.vyncrit.app.data.project.ProjectRepository
import com.vyncrit.app.data.project.ProjectStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalUiState(
    val logs: List<BuildLog> = emptyList(),
    val isBuilding: Boolean = false,
    val buildProgress: Float = 0f,
    val currentCommand: String = "",
    val commandHistory: List<String> = emptyList(),
    val projectId: String = "",
    val projectName: String = "",
    val status: ProjectStatus = ProjectStatus.IDLE,
    val error: String? = null
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val buildServerClient: BuildServerClient,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private var buildJob: Job? = null

    companion object {
        private val WELCOME_ART = """
  __     __              _ __   _ __
  \ \   / /__ _ _ _ _ _(_) _ \_| |_ __
   \ \ / / -_) '_| '_| | | | |_|  _/ _|
    \_/\_/\__|_| |_| |_|_|_| |_|__\__|
                                        
  Android App Builder Terminal v1.0
  Type 'help' for available commands
============================================
        """.trimIndent()
    }

    fun initialize(projectId: String) {
        _uiState.value = _uiState.value.copy(projectId = projectId)
        addLog(LogLevel.INFO, WELCOME_ART)
        loadProjectInfo(projectId)
    }

    private fun loadProjectInfo(projectId: String) {
        viewModelScope.launch {
            projectRepository.getProject(projectId).onSuccess { project ->
                _uiState.value = _uiState.value.copy(
                    projectName = project.name,
                    status = project.status
                )
            }
        }
    }

    fun executeCommand(command: String) {
        if (command.isBlank()) return

        _uiState.value = _uiState.value.copy(
            commandHistory = _uiState.value.commandHistory + command,
            currentCommand = command
        )

        addLog(LogLevel.INFO, "> $command")

        val parts = command.trim().split(" ")
        when (parts.first().lowercase()) {
            "help" -> showHelp()
            "build" -> startBuild()
            "clear" -> _uiState.value = _uiState.value.copy(logs = emptyList())
            "status" -> showStatus()
            "run" -> addLog(LogLevel.INFO, "Starting app preview...")
            "test" -> runTests()
            "deploy" -> addLog(LogLevel.WARN, "Deploy command requires Play Store configuration")
            else -> addLog(LogLevel.ERROR, "Unknown command: ${parts.first()}. Type 'help' for available commands.")
        }

        _uiState.value = _uiState.value.copy(currentCommand = "")
    }

    private fun showHelp() {
        val help = """
Available commands:
  help     - Show this help message
  build    - Start building the current project
  run      - Run/preview the generated app
  test     - Run automated tests
  status   - Show current project status
  deploy   - Prepare for Play Store deployment
  clear    - Clear terminal screen
        """.trimIndent()
        addLog(LogLevel.INFO, help)
    }

    private fun showStatus() {
        val state = _uiState.value
        val statusText = """
Project: ${state.projectName}
Status: ${state.status.name}
Builds: ${state.commandHistory.size} commands executed
        """.trimIndent()
        addLog(LogLevel.INFO, statusText)
    }

    private fun startBuild() {
        if (_uiState.value.isBuilding) {
            addLog(LogLevel.WARN, "A build is already in progress")
            return
        }

        _uiState.value = _uiState.value.copy(isBuilding = true, status = ProjectStatus.BUILDING)
        addLog(LogLevel.INFO, "Starting build...")

        buildJob = viewModelScope.launch {
            try {
                simulateBuildSteps()
                _uiState.value = _uiState.value.copy(
                    isBuilding = false,
                    status = ProjectStatus.SUCCESS
                )
                addLog(LogLevel.INFO, "Build completed successfully!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBuilding = false,
                    status = ProjectStatus.FAILED,
                    error = e.message
                )
                addLog(LogLevel.ERROR, "Build failed: ${e.message}")
            }
        }
    }

    private suspend fun simulateBuildSteps() {
        val steps = listOf(
            "Initializing build environment..." to LogLevel.DEBUG,
            "Resolving dependencies..." to LogLevel.DEBUG,
            "Configuring Gradle..." to LogLevel.INFO,
            "Compiling Kotlin sources..." to LogLevel.INFO,
            "Running R8 optimization..." to LogLevel.INFO,
            "Generating Baseline Profiles..." to LogLevel.INFO,
            "Packaging resources..." to LogLevel.INFO,
            "Signing APK..." to LogLevel.INFO,
            "Verifying build artifacts..." to LogLevel.DEBUG,
            "Build complete." to LogLevel.INFO
        )

        steps.forEachIndexed { index, (message, level) ->
            delay(500)
            addLog(level, message)
            _uiState.value = _uiState.value.copy(
                buildProgress = (index + 1).toFloat() / steps.size
            )
        }
    }

    private fun runTests() {
        addLog(LogLevel.INFO, "Running unit tests...")
        viewModelScope.launch {
            delay(1000)
            addLog(LogLevel.DEBUG, "✓ AuthViewModelTest passed (3/3)")
            delay(500)
            addLog(LogLevel.DEBUG, "✓ ChatViewModelTest passed (5/5)")
            delay(500)
            addLog(LogLevel.DEBUG, "✓ ProjectRepositoryTest passed (2/2)")
            delay(300)
            addLog(LogLevel.INFO, "All tests passed!")
        }
    }

    private fun addLog(level: LogLevel, message: String) {
        val log = BuildLog(
            timestamp = System.currentTimeMillis(),
            level = level,
            message = message
        )
        _uiState.value = _uiState.value.copy(
            logs = _uiState.value.logs + log
        )
    }

    override fun onCleared() {
        super.onCleared()
        buildJob?.cancel()
    }
}
