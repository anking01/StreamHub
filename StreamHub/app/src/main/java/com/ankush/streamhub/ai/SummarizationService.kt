package com.ankush.streamhub.ai

import com.ankush.streamhub.AppPreferences

class SummarizationService(private val prefs: AppPreferences) {

    private val gemini by lazy { GeminiSummarizer() }

    // Cache Groq instance — recreate only when API key changes
    private var groqSummarizer: GroqSummarizer? = null
    private var cachedGroqKey: String = ""

    // Cache Ollama instance — recreate only when API key changes
    private var ollamaSummarizer: OllamaSummarizer? = null
    private var cachedOllamaKey: String = ""

    private fun getGroq(): GroqSummarizer {
        val key = prefs.groqApiKey
        if (groqSummarizer == null || cachedGroqKey != key) {
            groqSummarizer = GroqSummarizer(key)
            cachedGroqKey = key
        }
        return groqSummarizer!!
    }

    private fun getOllama(): OllamaSummarizer {
        val key = prefs.ollamaApiKey
        if (ollamaSummarizer == null || cachedOllamaKey != key) {
            ollamaSummarizer = OllamaSummarizer(key)
            cachedOllamaKey = key
        }
        return ollamaSummarizer!!
    }

    suspend fun summarize(title: String, description: String): SummaryState {
        return when (prefs.aiProvider) {
            AppPreferences.PROVIDER_GROQ   -> getGroq().summarize(title, description)
            AppPreferences.PROVIDER_OLLAMA -> getOllama().summarize(title, description)
            else                           -> gemini.summarize(title, description)
        }
    }

    val currentProvider: String get() = prefs.aiProvider
}
