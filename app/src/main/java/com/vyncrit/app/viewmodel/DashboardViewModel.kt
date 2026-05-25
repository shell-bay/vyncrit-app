package com.vyncrit.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyncrit.app.data.project.Project
import com.vyncrit.app.data.project.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showNewProjectDialog: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            projectRepository.getProjects().collect { projects ->
                _uiState.value = _uiState.value.copy(
                    projects = projects,
                    isLoading = false
                )
            }
        }
    }

    fun showNewProjectDialog() {
        _uiState.value = _uiState.value.copy(showNewProjectDialog = true)
    }

    fun hideNewProjectDialog() {
        _uiState.value = _uiState.value.copy(showNewProjectDialog = false)
    }

    fun createProject(name: String, description: String, packageName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, showNewProjectDialog = false)
            val result = projectRepository.createProject(name, description, packageName)
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
