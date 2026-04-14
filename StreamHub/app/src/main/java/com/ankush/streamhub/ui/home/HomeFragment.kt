package com.ankush.streamhub.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.ankush.streamhub.R
import com.ankush.streamhub.StreamHubApp
import com.ankush.streamhub.data.model.Category
import com.ankush.streamhub.data.model.ContentItem
import com.ankush.streamhub.data.model.FeedListItem
import com.ankush.streamhub.data.model.FeedSource
import com.ankush.streamhub.databinding.FragmentHomeBinding
import com.ankush.streamhub.ui.SharedViewModel
import com.ankush.streamhub.ui.SharedViewModelFactory
import com.ankush.streamhub.ui.stream.StreamActivity
import com.ankush.streamhub.util.*
import kotlinx.coroutines.launch

// Language → BBC source name mapping for Regional sub-filter
private val REGIONAL_LANGUAGES = linkedMapOf(
    "All"      to null,
    "हिंदी"   to "BBC Hindi",
    "தமிழ்"   to "BBC Tamil",
    "తెలుగు"  to "BBC Telugu",
    "বাংলা"   to "BBC Bengali",
    "मराठी"   to "BBC Marathi",
    "ગુજરાતી" to "BBC Gujarati"
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by activityViewModels {
        SharedViewModelFactory(requireActivity().application as StreamHubApp)
    }

    private lateinit var feedAdapter: FeedAdapter
    private var isSearchMode = false
    private var currentSearchQuery = ""
    private var selectedRegionalSource: String? = null  // null = show all regional

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCategoryChips()
        setupRegionalChips()
        setupSwipeRefresh()
        setupSearch()
        observeData()
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(
            onItemClick      = { openStream(it) },
            onBookmarkClick  = { viewModel.toggleBookmark(it) },
            onShareClick     = { requireContext().showShareOptions(it.sourceUrl, it.title) },
            onSummarizeClick = { viewModel.summarizeItem(it) }
        )
        binding.recyclerView.apply {
            adapter = feedAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupCategoryChips() {
        Category.entries.forEachIndexed { i, cat ->
            Chip(requireContext()).apply {
                text = "${cat.emoji} ${cat.label}"
                isCheckable = true
                isChecked = i == 0
                setOnClickListener {
                    selectedRegionalSource = null
                    binding.scrollRegionalFilter.showIf(cat == Category.REGIONAL)
                    // Reset regional "All" chip
                    (binding.chipGroupRegional.getChildAt(0) as? Chip)?.isChecked = true
                    viewModel.setCategory(cat)
                }
            }.also { binding.chipGroupCategory.addView(it) }
        }
    }

    private fun setupRegionalChips() {
        REGIONAL_LANGUAGES.entries.forEachIndexed { i, (label, sourceName) ->
            Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = i == 0
                setOnClickListener {
                    selectedRegionalSource = sourceName
                    submitFilteredFeed(viewModel.feedItems.value.orEmpty())
                }
            }.also { binding.chipGroupRegional.addView(it) }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(R.color.primary)
            setOnRefreshListener { viewModel.refreshFeed() }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                if (query.isBlank()) exitSearch() else performSearch(query)
            }
        })
    }

    private fun observeData() {
        viewModel.feedItems.observe(viewLifecycleOwner) { items ->
            if (!isSearchMode) submitFilteredFeed(items)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
            binding.shimmerLayout.showIf(loading && feedAdapter.itemCount == 0)
            if (loading) binding.shimmerLayout.startShimmer() else binding.shimmerLayout.stopShimmer()
        }
        viewModel.error.observe(viewLifecycleOwner) { it?.let { msg -> requireContext().toast(msg, true) } }
        viewModel.activeCategory.observe(viewLifecycleOwner) { active ->
            for (i in 0 until binding.chipGroupCategory.childCount)
                (binding.chipGroupCategory.getChildAt(i) as? Chip)?.isChecked = Category.entries[i] == active
            binding.scrollRegionalFilter.showIf(active == Category.REGIONAL)
        }
        lifecycleScope.launch { viewModel.bookmarkIds.collect { feedAdapter.updateBookmarkedIds(it) } }
        lifecycleScope.launch { viewModel.summaries.collect { feedAdapter.updateSummaryStates(it) } }
        lifecycleScope.launch {
            viewModel.youtubeError.collect { error ->
                if (!error.isNullOrBlank())
                    Snackbar.make(binding.root, "YouTube: $error", Snackbar.LENGTH_LONG).show()
            }
        }
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (isSearchMode) {
                feedAdapter.submitList(results)
                val hasItems = results.any { it is FeedListItem.Item }
                binding.layoutEmpty.showIf(!hasItems)
                if (!hasItems) binding.tvEmptyText.text = "No results for \"$currentSearchQuery\""
            }
        }
    }

    private fun submitFilteredFeed(items: List<ContentItem>) {
        val filtered = if (viewModel.activeCategory.value == Category.REGIONAL && selectedRegionalSource != null)
            items.filter { it.sourceName == selectedRegionalSource }
        else
            items
        feedAdapter.submitContentList(filtered)
    }

    private fun performSearch(query: String) {
        isSearchMode = true
        currentSearchQuery = query
        viewModel.search(query)
    }

    private fun exitSearch() {
        isSearchMode = false
        viewModel.clearSearch()
        submitFilteredFeed(viewModel.feedItems.value.orEmpty())
    }

    private fun openStream(item: ContentItem) {
        if (item.source == FeedSource.YOUTUBE) {
            val youtubeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.contentUrl)).apply {
                setPackage("com.google.android.youtube")
            }
            if (youtubeIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(youtubeIntent)
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.contentUrl)))
            }
        } else {
            startActivity(Intent(requireContext(), StreamActivity::class.java)
                .putExtra(StreamActivity.EXTRA_CONTENT_ITEM, item))
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
