package com.claudeauto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Build layout programmatically (no XML needed)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 80, 64, 64)
        }

        val title = TextView(this).apply {
            text = "Claude Auto"
            textSize = 28f
            setPadding(0, 0, 0, 8)
        }

        val subtitle = TextView(this).apply {
            text = "Voice chat with Claude via Android Auto"
            textSize = 14f
            setPadding(0, 0, 0, 40)
        }

        val apiKeyLabel = TextView(this).apply {
            text = "Anthropic API Key"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }

        val apiKeyInput = EditText(this).apply {
            hint = "sk-ant-..."
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(Prefs.getApiKey(this@MainActivity) ?: "")
            setPadding(16, 16, 16, 16)
        }

        val saveButton = Button(this).apply {
            text = "Save API Key"
            setOnClickListener {
                val key = apiKeyInput.text.toString().trim()
                if (key.isBlank()) {
                    Toast.makeText(this@MainActivity, "Please enter an API key", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                Prefs.setApiKey(this@MainActivity, key)
                Toast.makeText(this@MainActivity, "API key saved!", Toast.LENGTH_SHORT).show()
            }
        }

        val divider = TextView(this).apply {
            setPadding(0, 40, 0, 0)
        }

        val statusTitle = TextView(this).apply {
            text = "Status"
            textSize = 18f
            setPadding(0, 0, 0, 8)
        }

        val permissionStatus = TextView(this).apply {
            text = if (hasAudioPermission()) "✅ Microphone permission granted"
            else "⚠ Microphone permission not granted"
            textSize = 14f
        }

        val apiKeyStatus = TextView(this).apply {
            text = if (Prefs.getApiKey(this@MainActivity) != null) "✅ API key configured"
            else "⚠ No API key set"
            textSize = 14f
            setPadding(0, 8, 0, 0)
        }

        val instructionsTitle = TextView(this).apply {
            text = "\nHow to use"
            textSize = 18f
        }

        val instructions = TextView(this).apply {
            text = """
1. Enter your Anthropic API key above and tap Save.
2. Connect your phone to Android Auto in your car (or use Desktop Head Unit for testing).
3. Open "Claude Auto" from the Android Auto launcher.
4. Tap "Speak" to talk to Claude hands-free.

Your conversation history is maintained until you tap "New Chat".
            """.trimIndent()
            textSize = 13f
            setPadding(0, 8, 0, 0)
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(apiKeyLabel)
        layout.addView(apiKeyInput)
        layout.addView(saveButton)
        layout.addView(divider)
        layout.addView(statusTitle)
        layout.addView(permissionStatus)
        layout.addView(apiKeyStatus)
        layout.addView(instructionsTitle)
        layout.addView(instructions)

        val scrollView = ScrollView(this)
        scrollView.addView(layout)
        setContentView(scrollView)

        // Request microphone permission if not granted
        if (!hasAudioPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            Toast.makeText(
                this,
                if (granted) "Microphone permission granted!" else "Microphone permission denied — voice won't work.",
                Toast.LENGTH_LONG
            ).show()
            recreate() // refresh status display
        }
    }
}
