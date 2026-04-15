package com.ankush.streamhub.ui

import androidx.lifecycle.*
import com.ankush.streamhub.StreamHubApp
import com.ankush.streamhub.ai.SummaryState
import com.ankush.streamhub.data.local.CollectionEntity
import com.ankush.streamhub.data.local.WatchHistoryEntity
import com.ankush.streamhub.data.model.*
import com.ankush.streamhub.util.Analytics
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

private val SOURCE_EMOJI = mapOf(
    FeedSource.YOUTUBE to "📺",
    FeedSource.NEWS    to "📰",
    FeedSource.RSS     to "📡",
    FeedSource.PODCAST to "🎙️",
    FeedSource.TWITCH  to "🟣",
    FeedSource.CUSTOM  to "🔗"
)

// Words to skip when extracting trending topics
private val STOP_WORDS = setOf(
    "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of",
    "with", "by", "from", "is", "are", "was", "were", "be", "been", "have", "has",
    "had", "do", "does", "did", "will", "would", "could", "should", "may", "might",
    "that", "this", "it", "he", "she", "they", "we", "you", "its", "as", "up",
    "out", "not", "no", "so", "if", "about", "than", "then", "there", "when",
    "who", "what", "which", "also", "after", "before", "over", "india", "indian",
    "new", "says", "said", "say", "one", "two", "year", "years", "day", "days",
    "time", "first", "last", "next", "back", "more", "some", "just", "now", "most",
    "other", "into", "here", "how", "their", "our", "your", "its", "been", "very",
    "also", "only", "such", "even", "much", "many", "know", "than", "them", "make",
    "like", "can't", "don't", "won't", "isn't", "aren't", "wasn't", "weren't"
)

// ─────────────────────────────────────────────────────────────────────────────
// Shared ViewModel – owned by MainActivity, shared across all fragments
// ─────────────────────────────────────────────────────────────────────────────

class SharedViewModel(private val app: StreamHubApp) : AndroidViewModel(app) {

    private val repository = app.repository

    // ── Feed state ────────────────────────────────────────────

    private val _feedItems    = MutableLiveData<List<ContentItem>>(emptyList())
    val feedItems: LiveData<List<ContentItem>> = _feedItems

    // All-category items — used by Discover and trending topics
    private val _allFeedItems = MutableLiveData<List<ContentItem>>(emptyList())
    val allFeedItems: LiveData<List<ContentItem>> = _allFeedItems

    private val _isLoading    = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error        = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _activeCategory = MutableLiveData(Category.ALL)
    val activeCategory: LiveData<Category> = _activeCategory

    // ── YouTube error ─────────────────────────────────────────
    val youtubeError: StateFlow<String?> = repository.youtubeError

    // ── Bookmarks ─────────────────────────────────────────────
    val bookmarks: LiveData<List<ContentItem>> = repository.bookmarks.asLiveData()
    val bookmarkIds: StateFlow<Set<String>>    = repository.bookmarkIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ── AI Summary ────────────────────────────────────────────
    private val _summaries = MutableStateFlow<Map<String, SummaryState>>(emptyMap())
    val summaries: StateFlow<Map<String, SummaryState>> = _summaries.asStateFlow()


    // ── Search ────────────────────────────────────────────────
    private val _searchResults = MutableLiveData<List<FeedListItem>>(emptyList())
    val searchResults: LiveData<List<FeedListItem>> = _searchResults

    // ── Trending Topics ───────────────────────────────────────
    private val _trendingTopics = MutableLiveData<List<String>>(emptyList())
    val trendingTopics: LiveData<List<String>> = _trendingTopics

    // ── Watch History ─────────────────────────────────────────
    val watchHistory: LiveData<List<WatchHistoryEntity>> = repository.watchHistory.asLiveData()

    // ── Collections ───────────────────────────────────────────
    val collections: LiveData<List<CollectionEntity>> = repository.collections.asLiveData()

    // ── Feed sources ──────────────────────────────────────────
    private val _feedSources = MutableLiveData(DefaultFeeds.list.toMutableList())
    val feedSources: LiveData<MutableList<FeedConfig>> = _feedSources

    // ─────────────────────────────────────────────────────────

    init {
        loadFeed()
    }

    fun loadFeed(category: Category = _activeCategory.value ?: Category.ALL, forceRefresh: Boolean = false) {
        _activeCategory.value = category
        _error.value = null

        viewModelScope.launch {
            val sources = _feedSources.value ?: DefaultFeeds.list

            if (forceRefresh) {
                // Manual refresh: keep existing data visible, show spinner until new data arrives
                _isLoading.value = true
            } else {
                // Initial load: show cached data instantly, spinner only if no cache
                val cached = repository.getCachedItems(category)
                if (cached.isNotEmpty()) {
                    _feedItems.value = cached
                    if (category == Category.ALL) {
                        _allFeedItems.value = cached
                        updateTrendingTopics(cached)
                    }
                } else {
                    _isLoading.value = true
                }
            }

            // Fetch fresh data
            val result = repository.fetchAllFeeds(sources, category)
            result
                .onSuccess { items ->
                    _feedItems.value = items
                    if (category == Category.ALL) {
                        _allFeedItems.value = items
                        updateTrendingTopics(items)
                    }
                }
                .onFailure { err ->
                    if (_feedItems.value.isNullOrEmpty()) {
                        _error.value = err.message ?: "Failed to load feed"
                    }
                }
            _isLoading.value = false
        }
    }

    fun setCategory(category: Category) {
        if (_activeCategory.value == category) return
        Analytics.categorySelected(category.name)
        loadFeed(category)
    }

    fun refreshFeed() = loadFeed(_activeCategory.value ?: Category.ALL, forceRefresh = true)

    fun search(query: String) {
        Analytics.searchPerformed(query)
        viewModelScope.launch {
            val flat = repository.searchFeed(query)
            val grouped = flat
                .groupBy { it.source }
                .flatMap { (source, items) ->
                    val emoji = SOURCE_EMOJI[source] ?: "📌"
                    listOf(FeedListItem.Header(source.label, emoji, items.size)) +
                    items.map { FeedListItem.Item(it) }
                }
            _searchResults.value = grouped
        }
    }

    fun clearSearch() { _searchResults.value = emptyList() }

    fun summarizeItem(item: ContentItem) {
        val current = _summaries.value[item.id]
        if (current is SummaryState.Loading || current is SummaryState.Success) return
        Analytics.aiSummarize(app.preferences.aiProvider)
        _summaries.value = _summaries.value + (item.id to SummaryState.Loading)
        viewModelScope.launch {
            val result = app.summarizationService.summarize(item.title, item.description)
            _summaries.value = _summaries.value + (item.id to result)
        }
    }

    fun toggleBookmark(item: ContentItem) {
        viewModelScope.launch {
            val wasBookmarked = bookmarkIds.value.contains(item.id)
            repository.toggleBookmark(item)
            if (wasBookmarked) Analytics.bookmarkRemoved(item.id)
            else               Analytics.bookmarkAdded(item.id)
        }
    }

    fun clearAllBookmarks() { viewModelScope.launch { repository.clearAllBookmarks() } }

    // ── Watch History ─────────────────────────────────────────

    fun logWatchHistory(item: ContentItem) {
        viewModelScope.launch { repository.logWatchHistory(item) }
    }

    fun clearWatchHistory() {
        viewModelScope.launch { repository.clearWatchHistory() }
    }

    // ── Collections ───────────────────────────────────────────

    fun addCollection(name: String, emoji: String = "📁") {
        viewModelScope.launch {
            repository.addCollection(CollectionEntity(UUID.randomUUID().toString(), name, emoji))
        }
    }

    fun deleteCollection(collection: CollectionEntity) {
        viewModelScope.launch { repository.deleteCollection(collection) }
    }

    fun moveBookmarkToCollection(bookmarkId: String, collectionId: String?) {
        viewModelScope.launch { repository.moveBookmarkToCollection(bookmarkId, collectionId) }
    }

    fun getBookmarksByCollection(collectionId: String) =
        repository.getBookmarksByCollection(collectionId).asLiveData()

    fun getUncategorizedBookmarks() =
        repository.getUncategorizedBookmarks().asLiveData()

    // ── Trending Topics ───────────────────────────────────────

    private fun updateTrendingTopics(items: List<ContentItem>) {
        if (items.isEmpty()) return
        val wordCount = mutableMapOf<String, Int>()
        items.forEach { item ->
            (item.title + " " + item.description)
                .lowercase()
                .split(Regex("[^a-zA-Z]+"))
                .filter { it.length > 3 && it !in STOP_WORDS }
                .forEach { word -> wordCount[word] = (wordCount[word] ?: 0) + 1 }
        }
        _trendingTopics.value = wordCount.entries
            .sortedByDescending { it.value }
            .take(15)
            .map { it.key.replaceFirstChar { c -> c.uppercase() } }
    }

    // ── Feed sources ──────────────────────────────────────────

    fun toggleFeedSource(id: String) {
        val current = _feedSources.value ?: return
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(isActive = !current[index].isActive)
            _feedSources.value = current
        }
    }

    fun addFeedSource(config: FeedConfig) {
        val current = _feedSources.value ?: mutableListOf()
        current.add(config)
        _feedSources.value = current
    }

    fun removeFeedSource(id: String) {
        val current = _feedSources.value ?: return
        _feedSources.value = current.filter { it.id != id }.toMutableList()
    }
}

class SharedViewModelFactory(private val app: StreamHubApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SharedViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
