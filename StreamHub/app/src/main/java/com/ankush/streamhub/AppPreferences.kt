package com.ankush.streamhub

import android.content.Context

class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("streamhub_prefs", Context.MODE_PRIVATE)

    var aiProvider: String
        get() = prefs.getString(KEY_AI_PROVIDER, PROVIDER_GEMINI) ?: PROVIDER_GEMINI
        set(value) { prefs.edit().putString(KEY_AI_PROVIDER, value).apply() }

    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ_API_KEY, "") ?: ""
        set(value) { prefs.edit().putString(KEY_GROQ_API_KEY, value).apply() }

    var ollamaApiKey: String
        get() = prefs.getString(KEY_OLLAMA_API_KEY, "") ?: ""
        set(value) { prefs.edit().putString(KEY_OLLAMA_API_KEY, value).apply() }

    var digestEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIGEST_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_DIGEST_ENABLED, value).apply() }

    companion object {
        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_GROQ   = "groq"
        const val PROVIDER_OLLAMA = "ollama"
        private const val KEY_AI_PROVIDER    = "ai_provider"
        private const val KEY_GROQ_API_KEY   = "groq_api_key"
        private const val KEY_OLLAMA_API_KEY  = "ollama_api_key"
        private const val KEY_DIGEST_ENABLED  = "digest_enabled"
    }
}
