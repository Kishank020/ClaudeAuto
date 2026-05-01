# Claude Auto 🚗🎙️

Voice chat with Claude via Android Auto — hands-free AI conversation while driving.

---

## Features

- **Full voice pipeline**: Speech-to-text → Claude API → Text-to-speech
- **Multi-turn conversations**: Maintains context throughout your drive
- **Safety-focused**: Claude is prompted to give concise, spoken-friendly answers
- **Simple UI**: Designed for Android Auto's distraction-optimized templates
- **Secure API key storage**: Key is stored in EncryptedSharedPreferences

---

## Setup

### 1. Get an Anthropic API Key
Go to [console.anthropic.com](https://console.anthropic.com) → API Keys → Create key.

### 2. Open the App on Your Phone
Launch **Claude Auto** on your Android phone. Enter your API key and tap **Save**.

### 3. Grant Microphone Permission
Accept the microphone permission prompt when asked.

### 4. Connect to Android Auto
- **Real car**: Connect your phone via USB or wireless Android Auto.
- **Testing on desktop**: Install [Desktop Head Unit (DHU)](https://developer.android.com/training/cars/testing/dhu) from the Android SDK.

### 5. Start Talking
Open Claude Auto from the Android Auto launcher. Tap **Speak** and ask anything!

---

## Project Structure

```
app/src/main/java/com/claudeauto/
├── MainActivity.kt          # Phone UI — API key entry + status
├── ClaudeCarAppService.kt   # Android Auto service entry point
├── ClaudeSession.kt         # Car app session
├── ClaudeScreen.kt          # Main Auto UI screen (state machine)
├── VoiceManager.kt          # STT + TTS wrapper
├── AnthropicClient.kt       # Anthropic API HTTP client
└── Prefs.kt                 # Encrypted API key storage
```

---

## How It Works

```
User taps "Speak"
    → SpeechRecognizer listens
    → Text sent to AnthropicClient
    → Claude responds (concise, voice-optimized)
    → TextToSpeech reads response aloud
    → App returns to IDLE, ready for next turn
```

Conversation history is kept in memory for the session, giving Claude full context.
Tap **New Chat** to clear history and start fresh.

---

## Building

```bash
./gradlew assembleDebug
```

Install to device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Notes

- The `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` is used for development.  
  For production, switch to a validated allowlist.
- Apps using the `NAVIGATION` category must pass Google's driving safety review before Play Store publication.
- The system prompt instructs Claude to keep answers short and conversational — ideal for TTS.
