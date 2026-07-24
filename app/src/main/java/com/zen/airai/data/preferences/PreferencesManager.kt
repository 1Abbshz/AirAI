package com.zen.airai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "airai_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val ENDPOINT = stringPreferencesKey("endpoint")
        private val MODEL = stringPreferencesKey("model")
        private val TEMPERATURE = doublePreferencesKey("temperature")
        private val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val THEME = stringPreferencesKey("theme")
    }

    private fun <T> blockingGet(key: Preferences.Key<T>, default: T): T = runBlocking {
        context.dataStore.data.map { it[key] ?: default }.first()
    }

    fun getApiKey(): String = blockingGet(API_KEY, "")
    fun getEndpoint(): String = blockingGet(ENDPOINT, "https://api.openai.com/v1")
    fun getModel(): String = blockingGet(MODEL, "gpt-4o")
    fun getTemperature(): Double = blockingGet(TEMPERATURE, 0.7)
    fun getSystemPrompt(): String = blockingGet(SYSTEM_PROMPT, "You are AirAI, a helpful AI coding assistant powered by OpenCode Zen.")
    fun getTheme(): String = blockingGet(THEME, "system")

    fun getApiKeyFlow(): Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    fun getEndpointFlow(): Flow<String> = context.dataStore.data.map { it[ENDPOINT] ?: "https://api.openai.com/v1" }
    fun getModelFlow(): Flow<String> = context.dataStore.data.map { it[MODEL] ?: "gpt-4o" }
    fun getSystemPromptFlow(): Flow<String> = context.dataStore.data.map { it[SYSTEM_PROMPT] ?: "You are AirAI, a helpful AI coding assistant powered by OpenCode Zen." }
    fun getThemeFlow(): Flow<String> = context.dataStore.data.map { it[THEME] ?: "system" }

    suspend fun setApiKey(key: String) { context.dataStore.edit { it[API_KEY] = key } }
    suspend fun setEndpoint(endpoint: String) { context.dataStore.edit { it[ENDPOINT] = endpoint } }
    suspend fun setModel(model: String) { context.dataStore.edit { it[MODEL] = model } }
    suspend fun setTemperature(temp: Double) { context.dataStore.edit { it[TEMPERATURE] = temp } }
    suspend fun setSystemPrompt(prompt: String) { context.dataStore.edit { it[SYSTEM_PROMPT] = prompt } }
    suspend fun setTheme(theme: String) { context.dataStore.edit { it[THEME] = theme } }
}
