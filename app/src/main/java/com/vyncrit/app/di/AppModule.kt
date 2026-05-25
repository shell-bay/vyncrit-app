package com.vyncrit.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.vyncrit.app.data.ai.AnthropicProvider
import com.vyncrit.app.data.ai.GeminiProvider
import com.vyncrit.app.data.ai.OpenAiProvider
import com.vyncrit.app.data.build.BuildServerClient
import com.vyncrit.app.data.project.ProjectRepository
import com.vyncrit.app.data.auth.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vyncrit_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(WebSockets)
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): AuthRepository {
        return AuthRepository(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideProjectRepository(
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth
    ): ProjectRepository {
        return ProjectRepository(firestore, firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideBuildServerClient(httpClient: HttpClient): BuildServerClient {
        return BuildServerClient(httpClient)
    }

    @Provides
    @Singleton
    fun provideOpenAiProvider(httpClient: HttpClient, json: Json): OpenAiProvider {
        return OpenAiProvider(httpClient, json)
    }

    @Provides
    @Singleton
    fun provideAnthropicProvider(httpClient: HttpClient, json: Json): AnthropicProvider {
        return AnthropicProvider(httpClient, json)
    }

    @Provides
    @Singleton
    fun provideGeminiProvider(httpClient: HttpClient, json: Json): GeminiProvider {
        return GeminiProvider(httpClient, json)
    }
}
