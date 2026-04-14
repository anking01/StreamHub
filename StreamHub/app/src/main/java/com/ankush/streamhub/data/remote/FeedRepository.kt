package com.ankush.streamhub.data.remote

import android.util.Log
import com.ankush.streamhub.data.local.*
import com.ankush.streamhub.data.model.*
import com.ankush.streamhub.data.remote.youtube.YouTubeDataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

private const val TAG = "FeedRepository"

// ─────────────────────────────────────────────────────────────────────────────
// FeedRepository – orchestrates network fetching + local DB
// ─────────────────────────────────────────────────────────────────────────────

class FeedRepository(
    private val bookmarkDao: BookmarkDao,
    private val cachedFeedDao: CachedFeedDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val collectionDao: CollectionDao,
    private val appScope: CoroutineScope
) {
    private val parser  = RssFeedParser()
    private val youtube = YouTubeDataSource()

    // In-memory feed cache (clears on app restart)
    private val _feedCache    = MutableStateFlow<List<ContentItem>>(emptyList())
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

            val bookmarkedIds = bookmarkDao.getAllBookmarkIds().first().toSet()
            val marked = results.map { it.copy(isBookmarked = it.id in bookmarkedIds) }

            _feedCache.value = marked

            // Persist to local cache for offline use (only ALL-category fetch)
            if (category == Category.ALL && marked.isNotEmpty()) {
                cachedFeedDao.clearAll()
                cachedFeedDao.insertAll(marked.map { it.toCachedEntity() })
            }

            Result.success(marked)
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllFeeds network failed, loading from cache: ${e.message}", e)
            // Fallback: load from Room cache
            val cached = if (category == Category.ALL) cachedFeedDao.getAll()
                         else cachedFeedDao.getByCategory(category.name)
            val bookmarkedIds = bookmarkDao.getAllBookmarkIds().first().toSet()
            val marked = cached.map { it.toDomain().copy(isBookmarked = it.id in bookmarkedIds) }
            if (marked.isNotEmpty()) {
                _feedCache.value = marked
                Result.success(marked)
            } else {
                Result.failure(e)
            }
        }
    }

    // Used by FeedSyncWorker for background-only fetch + cache (no UI state update)
    suspend fun fetchAndCacheInBackground(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = fetchAllFeeds(DefaultFeeds.list, Category.ALL)
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Background cache failed: ${e.message}")
            false
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
        bookmarkDao.getAllBookmarks().map { entities -> entities.map { it.toDomain() } }

    val bookmarkIds: Flow<Set<String>> =
        bookmarkDao.getAllBookmarkIds().map { it.toSet() }

    suspend fun toggleBookmark(item: ContentItem) {
        if (bookmarkDao.isBookmarked(item.id)) bookmarkDao.deleteById(item.id)
        else bookmarkDao.insert(item.toEntity())
    }

    suspend fun isBookmarked(id: String) = bookmarkDao.isBookmarked(id)

    suspend fun clearAllBookmarks() = bookmarkDao.deleteAll()

    // ── Watch History ─────────────────────────────────────────

    val watchHistory: Flow<List<WatchHistoryEntity>> = watchHistoryDao.getHistory()

    suspend fun logWatchHistory(item: ContentItem) {
        watchHistoryDao.insert(
            WatchHistoryEntity(
                id           = UUID.randomUUID().toString(),
                contentId    = item.id,
                title        = item.title,
                sourceName   = item.sourceName,
                source       = item.source.name,
                contentUrl   = item.contentUrl,
                thumbnailUrl = item.thumbnailUrl,
                type         = item.type.name
            )
        )
    }

    suspend fun clearWatchHistory() = watchHistoryDao.clearAll()

    // ── Offline Cache ─────────────────────────────────────────

    suspend fun getTopCachedItems(limit: Int = 5): List<CachedFeedEntity> =
        cachedFeedDao.getTop(limit)

    // ── Collections ───────────────────────────────────────────

    val collections: Flow<List<CollectionEntity>> = collectionDao.getAll()

    fun getBookmarksByCollection(collectionId: String): Flow<List<ContentItem>> =
        bookmarkDao.getBookmarksByCollection(collectionId).map { it.map { e -> e.toDomain() } }

    fun getUncategorizedBookmarks(): Flow<List<ContentItem>> =
        bookmarkDao.getUncategorizedBookmarks().map { it.map { e -> e.toDomain() } }

    suspend fun addCollection(collection: CollectionEntity) = collectionDao.insert(collection)

    suspend fun deleteCollection(collection: CollectionEntity) = collectionDao.delete(collection)

    suspend fun moveBookmarkToCollection(bookmarkId: String, collectionId: String?) =
        bookmarkDao.moveToCollection(bookmarkId, collectionId)
}
