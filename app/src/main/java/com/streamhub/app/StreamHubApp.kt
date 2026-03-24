package com.streamhub.app

import android.app.Application
import com.streamhub.app.data.local.AppDatabase
import com.streamhub.app.data.remote.FeedRepository
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

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: StreamHubApp
            private set
    }
}
