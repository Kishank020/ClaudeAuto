package com.claudeauto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*

enum class ConversationState {
    IDLE,        // Waiting for user to tap mic
    LISTENING,   // STT active
    THINKING,    // Waiting for Claude response
    SPEAKING     // TTS playing
}

class ClaudeScreen(carContext: CarContext) : Screen(carContext), VoiceManager.Listener {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val voiceManager = VoiceManager(carContext)
    private val conversationHistory = mutableListOf<Message>()

    private var state = ConversationState.IDLE
    private var lastResponse = "Tap the microphone to start talking with Claude."
    private var lastUserMessage = ""
    private var errorMessage: String? = null

    init {
        voiceManager.setListener(this)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                voiceManager.destroy()
                scope.cancel()
            }
        })
    }

    override fun onGetTemplate(): Template {
        val apiKey = Prefs.getApiKey(carContext)

        // No API key set
        if (apiKey == null) {
            return MessageTemplate.Builder(
                "No API key found.\n\nOpen the Claude Auto app on your phone and enter your Anthropic API key to get started."
            )
                .setTitle("Claude Auto")
                .setHeaderAction(Action.APP_ICON)
                .build()
        }

        val statusText = when (state) {
            ConversationState.IDLE -> if (errorMessage != null) "⚠ $errorMessage" else lastResponse
            ConversationState.LISTENING -> "🎙 Listening…"
            ConversationState.THINKING -> "💭 Claude is thinking…"
            ConversationState.SPEAKING -> "🔊 $lastResponse"
        }

        val micAction = Action.Builder()
            .setTitle(
                when (state) {
                    ConversationState.IDLE -> "Speak"
                    ConversationState.LISTENING -> "Cancel"
                    ConversationState.THINKING -> "Cancel"
                    ConversationState.SPEAKING -> "Stop"
                }
            )
            .setOnClickListener {
                when (state) {
                    ConversationState.IDLE -> startListeningIfReady()
                    ConversationState.LISTENING -> {
                        voiceManager.stopListening()
                        setState(ConversationState.IDLE)
                    }
                    ConversationState.THINKING -> {
                        scope.coroutineContext.cancelChildren()
                        setState(ConversationState.IDLE)
                    }
                    ConversationState.SPEAKING -> {
                        voiceManager.stopSpeaking()
                        setState(ConversationState.IDLE)
                    }
                }
            }
            .build()

        val newConvoAction = Action.Builder()
            .setTitle("New Chat")
            .setOnClickListener {
                voiceManager.stopSpeaking()
                voiceManager.stopListening()
                scope.coroutineContext.cancelChildren()
                conversationHistory.clear()
                lastResponse = "New conversation started. Tap Speak to begin."
                lastUserMessage = ""
                errorMessage = null
                setState(ConversationState.IDLE)
            }
            .build()

        // Show conversation context if we have history
        val description = if (lastUserMessage.isNotEmpty() && state != ConversationState.LISTENING) {
            "You: $lastUserMessage\n\nClaude: $statusText"
        } else {
            statusText
        }

        return MessageTemplate.Builder(description)
            .setTitle("Claude Auto")
            .setHeaderAction(Action.APP_ICON)
            .addAction(micAction)
            .addAction(newConvoAction)
            .build()
    }

    private fun startListeningIfReady() {
        errorMessage = null
        setState(ConversationState.LISTENING)
        voiceManager.startListening()
    }

    private fun setState(newState: ConversationState) {
        state = newState
        invalidate()
    }

    // VoiceManager.Listener callbacks

    override fun onListeningStarted() {
        setState(ConversationState.LISTENING)
    }

    override fun onSpeechResult(text: String) {
        lastUserMessage = text
        conversationHistory.add(Message("user", text))
        setState(ConversationState.THINKING)
        fetchClaudeResponse()
    }

    override fun onSpeechError(error: String) {
        errorMessage = error
        setState(ConversationState.IDLE)
    }

    override fun onSpeakingDone() {
        setState(ConversationState.IDLE)
    }

    private fun fetchClaudeResponse() {
        val apiKey = Prefs.getApiKey(carContext) ?: run {
            errorMessage = "API key missing"
            setState(ConversationState.IDLE)
            return
        }

        scope.launch {
            val client = AnthropicClient(apiKey)
            val result = client.sendMessage(conversationHistory.toList())

            result.fold(
                onSuccess = { response ->
                    conversationHistory.add(Message("assistant", response))
                    lastResponse = response
                    setState(ConversationState.SPEAKING)
                    voiceManager.speak(response)
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "Unknown error"
                    // Remove the last user message from history on failure
                    if (conversationHistory.lastOrNull()?.role == "user") {
                        conversationHistory.removeLastOrNull()
                    }
                    setState(ConversationState.IDLE)
                }
            )
        }
    }
}
