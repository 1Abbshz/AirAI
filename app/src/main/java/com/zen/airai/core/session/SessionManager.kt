package com.zen.airai.core.session

import com.zen.airai.core.ai.AiClient
import com.zen.airai.core.ai.AiMessage
import com.zen.airai.data.db.entity.ChatEntity
import com.zen.airai.data.db.entity.MessageEntity
import com.zen.airai.data.preferences.PreferencesManager
import com.zen.airai.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SessionManager(
    private val chatRepository: ChatRepository,
    private val aiClient: AiClient
) {
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun getAllChats(): Flow<List<ChatEntity>> = chatRepository.getAllChats()

    fun getMessages(chatId: String): Flow<List<MessageEntity>> =
        chatRepository.getMessagesForChat(chatId)

    suspend fun createChat(title: String, systemPrompt: String? = null): ChatEntity {
        return chatRepository.createChat(title, systemPrompt)
    }

    suspend fun deleteChat(chatId: String) {
        chatRepository.deleteChat(chatId)
    }

    suspend fun sendMessage(
        chatId: String,
        content: String,
        preferences: PreferencesManager,
        onToken: (String) -> Unit = {}
    ) {
        if (_isGenerating.value) return

        chatRepository.sendMessage(chatId, "user", content)
        _isGenerating.value = true

        try {
            val allMessages = chatRepository.getMessagesForChat(chatId).first()

            val aiMessages = allMessages.map {
                AiMessage(role = it.role, content = it.content)
            }

            val streamMessage = chatRepository.createStreamingMessage(chatId)
            var accumulated = ""

            withContext(Dispatchers.IO) {
                aiClient.chatStream(
                    messages = aiMessages.dropLast(1),
                    systemPrompt = preferences.getSystemPrompt()
                ).collect { token ->
                    accumulated += token
                    onToken(accumulated)
                    chatRepository.updateMessageContent(streamMessage.id, accumulated)
                }
            }

            chatRepository.finalizeStreamingMessage(streamMessage.id, accumulated)
        } finally {
            _isGenerating.value = false
        }
    }
}
