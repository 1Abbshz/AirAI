package com.zen.airai.di

import android.content.Context
import com.zen.airai.core.ai.AiClient
import com.zen.airai.core.session.SessionManager
import com.zen.airai.data.db.AppDatabase
import com.zen.airai.data.preferences.PreferencesManager
import com.zen.airai.data.repository.ChatRepository

class AppContainer(context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.create(context)
    }

    val preferences: PreferencesManager by lazy {
        PreferencesManager(context)
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(database.chatDao(), database.messageDao())
    }

    val aiClient: AiClient by lazy {
        AiClient(preferences)
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(chatRepository, aiClient)
    }
}
