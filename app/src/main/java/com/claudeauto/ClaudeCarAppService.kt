package com.claudeauto

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class ClaudeCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // During development, allow all hosts.
        // For production, restrict to known Android Auto hosts:
        // return HostValidator.Builder(applicationContext)
        //     .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
        //     .build()
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return ClaudeSession()
    }
}
