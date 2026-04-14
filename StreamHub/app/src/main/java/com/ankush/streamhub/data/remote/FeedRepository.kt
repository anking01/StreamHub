package com.ankush.streamhub.data.remote

import android.util.Log
import com.ankush.streamhub.data.local.BookmarkDao
import com.ankush.streamhub.data.local.toEntity
import com.ankush.streamhub.data.local.toDomain
import com.ankush.streamhub.data.model.*
import com.ankush.streamhub.data.remote.youtube.YouTubeDataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private const val TAG = "FeedRepository"

// ─────────────────────────────────────────────────────────────────────────────
// FeedRepository – orchestrates network fetching + local DB
// ─────────────────────────────────────────────────────────────────────────────

class FeedRepository(
    private val bookmarkDao: BookmarkDao,
    private val appScope: CoroutineScope
) {
    private val parser = RssFeedParser()
    private val youtube = YouTubeDataSource()

    // In-memory feed cache (clears on app restart)
    private val _feedCache = MutableStateFlow<List<ContentItem>>(emptyList())
    val feedCache: StateFlow<List<ContentItem>> = _feedCache.asStateFlow()

    private val _youtubeError = MutableStateFlow<String?>(null)
    val youtubeError: StateFlow<String?> = _youtubeError.asStateFlow()

    // ── Feed Fetching ─────────────────────────────────────────

    suspend fun fetchAllFeeds(
        feeds: List<FeedConfig>,
        category: Category = Category.ALL
    ): Result<List<ContentItem>> = withContext(Dispatchers.IO) {
        try {
            val activeFeeds = feeds.filter { it.isActive }
                .let { list ->
                    if (category == Category.ALL) list
                    else list.filter { it.category == category }
                }

            // Fetch all feeds concurrently (limit 5 at once)
            // YouTube errors are caught separately so RSS feeds still load
            _youtubeError.value = null
            val results = activeFeeds
                .chunked(5)
                .flatMap { batch ->
                    batch.map { config ->
                        async {
                            if (config.source == FeedSource.YOUTUBE) {
                                try {
                                    youtube.fetch(config)
                                } catch (e: Exception) {
                                    Log.e(TAG, "YouTube fetch failed: ${e.message}")
                                    _youtubeError.value = e.message
                                    emptyList()
                                }
                            } else {
                                parser.parse(config)
                            }
                        }
                    }.awaitAll()
                }
                .flatten()
                .distinctBy { it.sourceUrl }
                .sortedByDescending { it.publishedAt }

            // Mark bookmarked items
            val bookmarkedIds = bookmarkDao.getAllBookmarkIds().first().toSet()
            val marked = results.map { it.copy(isBookmarked = it.id in bookmarkedIds) }

            _feedCache.value = marked
            Result.success(marked)
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllFeeds failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun searchFeed(query: String): List<ContentItem> {
        val q = query.lowercase().trim()
        return _feedCache.value.filter {
            it.title.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.sourceName.lowercase().contains(q) ||
            it.tags.any { tag -> tag.lowercase().contains(q) }
        }
    }

    // ── Bookmark Operations ───────────────────────────────────

    val bookmarks: Flow<List<ContentItem>> =
        bookmarkDao.getAllBookmarks().map { entities ->
            entities.map { it.toDomain() }
        }

    val bookmarkIds: Flow<Set<String>> =
        bookmarkDao.getAllBookmarkIds().map { it.toSet() }

    suspend fun toggleBookmark(item: ContentItem) {
        if (bookmarkDao.isBookmarked(item.id)) {
            bookmarkDao.deleteById(item.id)
        } else {
            bookmarkDao.insert(item.toEntity())
        }
    }

    suspend fun isBookmarked(id: String) = bookmarkDao.isBookmarked(id)

    suspend fun clearAllBookmarks() = bookmarkDao.deleteAll()
}
