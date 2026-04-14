package com.ankush.streamhub.ui

import androidx.lifecycle.*
import com.ankush.streamhub.StreamHubApp
import com.ankush.streamhub.ai.SummaryState
import com.ankush.streamhub.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val SOURCE_EMOJI = mapOf(
    FeedSource.YOUTUBE  to "📺",
    FeedSource.NEWS     to "📰",
    FeedSource.RSS      to "📡",
    FeedSource.PODCAST  to "🎙️",
    FeedSource.TWITCH   to "🟣",
    FeedSource.CUSTOM   to "🔗"
)

// ─────────────────────────────────────────────────────────────────────────────
// Shared ViewModel – owned by MainActivity, shared across all fragments
// ─────────────────────────────────────────────────────────────────────────────

class SharedViewModel(private val app: StreamHubApp) : AndroidViewModel(app) {

    private val repository = app.repository

    // ── Feed state ────────────────────────────────────────────

    private val _feedItems   = MutableLiveData<List<ContentItem>>(emptyList())
    val feedItems: LiveData<List<ContentItem>> = _feedItems

    // Always holds ALL-category items — used by Discover so it's not affected by Home's category filter
    private val _allFeedItems = MutableLiveData<List<ContentItem>>(emptyList())
    val allFeedItems: LiveData<List<ContentItem>> = _allFeedItems

    private val _isLoading   = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error       = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _activeCategory = MutableLiveData(Category.ALL)
    val activeCategory: LiveData<Category> = _activeCategory

    // ── YouTube error (shown as Snackbar in HomeFragment) ────
    val youtubeError: StateFlow<String?> = repository.youtubeError

    // ── Bookmarks (Flow from Room) ────────────────────────────
    val bookmarks: LiveData<List<ContentItem>> = repository.bookmarks.asLiveData()
    val bookmarkIds: StateFlow<Set<String>>    = repository.bookmarkIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ── AI Summary state ──────────────────────────────────────
    private val _summaries = MutableStateFlow<Map<String, SummaryState>>(emptyMap())
    val summaries: StateFlow<Map<String, SummaryState>> = _summaries.asStateFlow()

    // ── Search state ──────────────────────────────────────────
    private val _searchResults = MutableLiveData<List<FeedListItem>>(emptyList())
    val searchResults: LiveData<List<FeedListItem>> = _searchResults

    // ── Feed sources (in-memory, user can toggle/add) ─────────
    private val _feedSources = MutableLiveData(DefaultFeeds.list.toMutableList())
    val feedSources: LiveData<MutableList<FeedConfig>> = _feedSources

    // ─────────────────────────────────────────────────────────

    init {
        loadFeed()
    }

    fun loadFeed(category: Category = _activeCategory.value ?: Category.ALL) {
        _isLoading.value = true
        _error.value = null
        _activeCategory.value = category

        viewModelScope.launch {
            val sources = _feedSources.value ?: DefaultFeeds.list
            val result = repository.fetchAllFeeds(sources, category)
            result
                .onSuccess { items ->
                    _feedItems.value = items
                    if (category == Category.ALL) _allFeedItems.value = items
                    _isLoading.value = false
                }
                .onFailure { err ->
                    _error.value = err.message ?: "Failed to load feed"
                    _isLoading.value = false
                }
        }
    }

    fun setCategory(category: Category) {
        if (_activeCategory.value == category) return
        loadFeed(category)
    }

    fun refreshFeed() = loadFeed(_activeCategory.value ?: Category.ALL)

    fun search(query: String) {
        viewModelScope.launch {
            val flat = repository.searchFeed(query)
            // Group by source, add section headers
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

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun summarizeItem(item: ContentItem) {
        val current = _summaries.value[item.id]
        if (current is SummaryState.Loading || current is SummaryState.Success) return
        _summaries.value = _summaries.value + (item.id to SummaryState.Loading)
        viewModelScope.launch {
            val result = app.summarizationService.summarize(item.title, item.description)
            _summaries.value = _summaries.value + (item.id to result)
        }
    }

    fun toggleBookmark(item: ContentItem) {
        viewModelScope.launch {
            repository.toggleBookmark(item)
        }
    }

    fun clearAllBookmarks() {
        viewModelScope.launch {
            repository.clearAllBookmarks()
        }
    }

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
