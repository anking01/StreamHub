package com.streamhub.app.ui

import androidx.lifecycle.*
import com.streamhub.app.StreamHubApp
import com.streamhub.app.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Shared ViewModel – owned by MainActivity, shared across all fragments
// ─────────────────────────────────────────────────────────────────────────────

class SharedViewModel(private val app: StreamHubApp) : AndroidViewModel(app) {

    private val repository = app.repository

    // ── Feed state ────────────────────────────────────────────

    private val _feedItems   = MutableLiveData<List<ContentItem>>(emptyList())
    val feedItems: LiveData<List<ContentItem>> = _feedItems

    private val _isLoading   = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error       = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _activeCategory = MutableLiveData(Category.ALL)
    val activeCategory: LiveData<Category> = _activeCategory

    // ── Bookmarks (Flow from Room) ────────────────────────────
    val bookmarks: LiveData<List<ContentItem>> = repository.bookmarks.asLiveData()
    val bookmarkIds: StateFlow<Set<String>>    = repository.bookmarkIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ── Search state ──────────────────────────────────────────
    private val _searchResults = MutableLiveData<List<ContentItem>>(emptyList())
    val searchResults: LiveData<List<ContentItem>> = _searchResults

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
            _searchResults.value = repository.searchFeed(query)
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
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
