package com.vyncrit.app.data.project

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    private val collection get() = firestore.collection("projects")

    suspend fun createProject(
        name: String,
        description: String,
        packageName: String
    ): Result<Project> {
        return try {
            val userId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val project = Project(
                name = name,
                description = description,
                packageName = packageName.ifBlank { "com.${name.lowercase().replace(" ", "")}.app" },
                ownerId = userId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val docRef = collection.add(project).await()
            Result.success(project.copy(id = docRef.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProject(project: Project): Result<Unit> {
        return try {
            collection.document(project.id).set(project.copy(updatedAt = System.currentTimeMillis())).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        return try {
            collection.document(projectId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getProjects(): Flow<List<Project>> = callbackFlow {
        val userId = firebaseAuth.currentUser?.uid ?: run {
            close(Exception("Not authenticated"))
            return@callbackFlow
        }
        val subscription = collection
            .whereEqualTo("ownerId", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val projects = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Project::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(projects)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getProject(projectId: String): Result<Project> {
        return try {
            val doc = collection.document(projectId).get().await()
            val project = doc.toObject(Project::class.java)?.copy(id = doc.id)
                ?: return Result.failure(Exception("Project not found"))
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
