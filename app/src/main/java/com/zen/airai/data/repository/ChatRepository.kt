package com.zen.airai.data.repository

import com.zen.airai.data.db.dao.ChatDao
import com.zen.airai.data.db.dao.MessageDao
import com.zen.airai.data.db.entity.ChatEntity
import com.zen.airai.data.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    fun getAllChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForChat(chatId)

    suspend fun createChat(title: String, systemPrompt: String? = null): ChatEntity {
        val chat = ChatEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            systemPrompt = systemPrompt
        )
        chatDao.insertChat(chat)
        return chat
    }

    suspend fun updateChatTitle(chatId: String, title: String) {
        val chat = chatDao.getChatById(chatId) ?: return
        chatDao.updateChat(chat.copy(title = title, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteChat(chatId: String) {
        chatDao.deleteChatById(chatId)
    }

    suspend fun sendMessage(chatId: String, role: String, content: String): MessageEntity {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            role = role,
            content = content
        )
        messageDao.insertMessage(message)

        chatDao.getChatById(chatId)?.let { chat ->
            chatDao.updateChat(chat.copy(updatedAt = System.currentTimeMillis()))
        }

        return message
    }

    suspend fun createStreamingMessage(chatId: String): MessageEntity {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            role = "assistant",
            content = "",
            isStreaming = true
        )
        messageDao.insertMessage(message)
        return message
    }

    suspend fun updateMessageContent(messageId: String, content: String) {
        val existing = messageDao.getMessageById(messageId) ?: return
        messageDao.updateMessage(existing.copy(content = content))
    }

    suspend fun finalizeStreamingMessage(messageId: String, content: String) {
        val existing = messageDao.getMessageById(messageId) ?: return
        messageDao.updateMessage(existing.copy(content = content, isStreaming = false))
    }
}
