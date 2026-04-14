package com.ankush.streamhub.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.ankush.streamhub.data.model.ContentItem

// ─────────────────────────────────────────────────────────────────────────────
// Analytics — thin wrapper around Firebase Analytics for key app events
// ─────────────────────────────────────────────────────────────────────────────

object Analytics {

    private var fa: FirebaseAnalytics? = null

    fun init(context: Context) {
        fa = FirebaseAnalytics.getInstance(context)
    }

    private fun log(event: String, vararg params: Pair<String, String>) {
        val bundle = Bundle().apply { params.forEach { (k, v) -> putString(k, v) } }
        fa?.logEvent(event, bundle)
    }

    // Content
    fun contentOpen(item: ContentItem) = log(
        "content_open",
        "source"   to item.sourceName,
        "type"     to item.type.name,
        "category" to item.category.name
    )

    // Bookmarks
    fun bookmarkAdded(itemId: String)   = log("bookmark_add",    "id" to itemId)
    fun bookmarkRemoved(itemId: String) = log("bookmark_remove", "id" to itemId)

    // Search
    fun searchPerformed(query: String) = log("search", "query" to query.take(100))

    // AI
    fun aiSummarize(provider: String) = log("ai_summarize", "provider" to provider)

    // Navigation
    fun categorySelected(category: String)    = log("category_select",  "category" to category)
    fun trendingTopicClicked(topic: String)   = log("trending_topic",    "topic"    to topic)
    fun collectionsViewed(collectionName: String) = log("collection_view", "name" to collectionName)

    // Share
    fun shareClicked(method: String) = log("share", "method" to method)
}
