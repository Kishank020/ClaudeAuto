package com.claudeauto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class ClaudeSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return ClaudeScreen(carContext)
    }
}
