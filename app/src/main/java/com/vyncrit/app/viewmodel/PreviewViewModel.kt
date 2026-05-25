package com.vyncrit.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyncrit.app.data.build.BuildServerClient
import com.vyncrit.app.data.project.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreviewUiState(
    val projectId: String = "",
    val projectName: String = "",
    val apkUrl: String = "",
    val aabUrl: String = "",
    val apkSize: String = "",
    val isDownloading: Boolean = false,
    val isInstalling: Boolean = false,
    val status: String = "No build yet. Go to Terminal to build.",
    val hasBuild: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val buildServerClient: BuildServerClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    fun initialize(projectId: String) {
        _uiState.value = _uiState.value.copy(projectId = projectId)
        loadProjectInfo(projectId)
    }

    private fun loadProjectInfo(projectId: String) {
        viewModelScope.launch {
            projectRepository.getProject(projectId).onSuccess { project ->
                _uiState.value = _uiState.value.copy(
                    projectName = project.name,
                    hasBuild = project.lastBuildId.isNotEmpty(),
                    status = if (project.lastBuildId.isNotEmpty()) "Build available"
                    else "No build yet. Go to Terminal to build."
                )
            }
        }
    }

    fun downloadApk() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, error = null)
            try {
                // Simulate download
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    status = "APK downloaded successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = "Download failed: ${e.message}"
                )
            }
        }
    }

    fun installOnDevice() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInstalling = true, error = null)
            try {
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(
                    isInstalling = false,
                    status = "App installed on device!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isInstalling = false,
                    error = "Installation failed: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
