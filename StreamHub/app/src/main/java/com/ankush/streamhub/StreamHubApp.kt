package com.ankush.streamhub

import android.app.Application
import com.ankush.streamhub.ai.SummarizationService
import com.ankush.streamhub.data.local.AppDatabase
import com.ankush.streamhub.data.remote.FeedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class StreamHubApp : Application() {

    // App-scope coroutine scope (lives as long as app)
    val applicationScope = CoroutineScope(SupervisorJob())

    // Lazy-init database — created only when first accessed
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    // Lazy-init repository
    val repository: FeedRepository by lazy {
        FeedRepository(database.bookmarkDao(), applicationScope)
    }

    // User preferences (AI provider, Groq key, etc.)
    val preferences: AppPreferences by lazy { AppPreferences(this) }

    // AI summarization service — delegates to Gemini or Groq based on prefs
    val summarizationService: SummarizationService by lazy { SummarizationService(preferences) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: StreamHubApp
            private set
    }
}
