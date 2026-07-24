package com.zen.airai.ui.screens.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zen.airai.AirAI
import com.zen.airai.data.db.entity.ChatEntity
import com.zen.airai.data.db.entity.MessageEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val chats: List<ChatEntity> = emptyList(),
    val currentChatId: String? = null,
    val messages: List<MessageEntity> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val streamingContent: String = "",
    val showSidebar: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AirAI).container
    private val sessionManager = container.sessionManager
    private val chatRepository = container.chatRepository
    private val preferences = container.preferences

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.getAllChats().collect { chats ->
                _uiState.update { it.copy(chats = chats) }
            }
        }

        viewModelScope.launch {
            sessionManager.isGenerating.collect { generating ->
                _uiState.update { it.copy(isGenerating = generating) }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun toggleSidebar() {
        _uiState.update { it.copy(showSidebar = !it.showSidebar) }
    }

    fun selectChat(chatId: String) {
        _uiState.update { it.copy(currentChatId = chatId, showSidebar = false) }

        viewModelScope.launch {
            sessionManager.getMessages(chatId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val title = _uiState.value.inputText.take(50).ifBlank { "New Chat" }
            val chat = sessionManager.createChat(title)
            _uiState.update { it.copy(currentChatId = chat.id, inputText = "", showSidebar = false) }

            sessionManager.getMessages(chat.id).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            sessionManager.deleteChat(chatId)
            if (_uiState.value.currentChatId == chatId) {
                _uiState.update { it.copy(currentChatId = null, messages = emptyList()) }
            }
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val content = state.inputText.trim()
        if (content.isEmpty() || state.isGenerating) return

        viewModelScope.launch {
            var chatId = state.currentChatId

            if (chatId == null) {
                val chat = sessionManager.createChat(content.take(50))
                chatId = chat.id
                _uiState.update { it.copy(currentChatId = chatId) }

                sessionManager.getMessages(chatId).collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
            }

            val finalChatId = chatId ?: return@launch
            _uiState.update { it.copy(inputText = "", isGenerating = true, streamingContent = "") }

            sessionManager.sendMessage(
                chatId = finalChatId,
                content = content,
                preferences = preferences,
                onToken = { accumulated ->
                    _uiState.update { it.copy(streamingContent = accumulated) }
                }
            )

            _uiState.update { it.copy(isGenerating = false, streamingContent = "") }
        }
    }
}
