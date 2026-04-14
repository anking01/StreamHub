package com.ankush.streamhub.ui.discover

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.ankush.streamhub.StreamHubApp
import com.ankush.streamhub.data.model.Category
import com.ankush.streamhub.data.model.ContentItem
import com.ankush.streamhub.databinding.FragmentDiscoverBinding
import com.ankush.streamhub.ui.SharedViewModel
import com.ankush.streamhub.ui.SharedViewModelFactory
import com.ankush.streamhub.ui.home.FeedAdapter
import com.ankush.streamhub.ui.stream.StreamActivity
import com.ankush.streamhub.util.*
import kotlinx.coroutines.launch

private val REGIONAL_LANGUAGES = linkedMapOf(
    "All"      to null,
    "हिंदी"   to "BBC Hindi",
    "தமிழ்"   to "BBC Tamil",
    "తెలుగు"  to "BBC Telugu",
    "বাংলা"   to "BBC Bengali",
    "मराठी"   to "BBC Marathi",
    "ગુજરાતી" to "BBC Gujarati"
)

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by activityViewModels {
        SharedViewModelFactory(requireActivity().application as StreamHubApp)
    }

    private lateinit var adapter: FeedAdapter
    private var selectedCategory: Category = Category.VIDEOS
    private var trendingFilter: String? = null
    private var selectedRegionalSource: String? = null  // null = show all regional

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FeedAdapter(
            onItemClick      = { openStream(it) },
            onBookmarkClick  = { viewModel.toggleBookmark(it) },
            onShareClick     = { requireContext().showShareOptions(it.sourceUrl, it.title) },
            onSummarizeClick = { viewModel.summarizeItem(it) }
        )
        binding.recyclerView.apply {
            adapter = this@DiscoverFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Category chips (skip ALL)
        Category.entries.drop(1).forEachIndexed { i, category ->
            Chip(requireContext()).apply {
                text = "${category.emoji} ${category.label}"
                isCheckable = true
                isChecked   = i == 0
                setOnClickListener {
                    trendingFilter = null
                    selectedRegionalSource = null
                    selectedCategory = category
                    binding.scrollRegionalFilter.showIf(category == Category.REGIONAL)
                    (binding.chipGroupRegional.getChildAt(0) as? Chip)?.isChecked = true
                    filterAndSubmit()
                }
            }.also { binding.chipGroupDiscover.addView(it) }
        }

        // Regional language sub-chips
        REGIONAL_LANGUAGES.entries.forEachIndexed { i, (label, sourceName) ->
            Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked   = i == 0
                setOnClickListener {
                    selectedRegionalSource = sourceName
                    trendingFilter = null
                    filterAndSubmit()
                }
            }.also { binding.chipGroupRegional.addView(it) }
        }

        // Trending topic chips
        viewModel.trendingTopics.observe(viewLifecycleOwner) { topics ->
            buildTrendingChips(topics)
            binding.layoutTrending.showIf(topics.isNotEmpty())
        }

        viewModel.allFeedItems.observe(viewLifecycleOwner) { filterAndSubmit() }
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.progressBar.showIf(it) }

        lifecycleScope.launch {
            viewModel.bookmarkIds.collect { ids -> adapter.updateBookmarkedIds(ids) }
        }
        lifecycleScope.launch {
            viewModel.summaries.collect { states -> adapter.updateSummaryStates(states) }
        }
    }

    private fun buildTrendingChips(topics: List<String>) {
        binding.chipGroupTrending.removeAllViews()
        topics.forEach { topic ->
            Chip(requireContext()).apply {
                text = "#$topic"
                isCheckable = true
                setOnClickListener {
                    val wasActive = isChecked
                    binding.chipGroupTrending.clearCheck()
                    if (!wasActive) {
                        isChecked = true
                        trendingFilter = topic
                        Analytics.trendingTopicClicked(topic)
                    } else {
                        trendingFilter = null
                    }
                    filterAndSubmit()
                }
            }.also { binding.chipGroupTrending.addView(it) }
        }
    }

    private fun filterAndSubmit() {
        val all = viewModel.allFeedItems.value.orEmpty()
        val filtered = when {
            trendingFilter != null -> {
                val kw = trendingFilter!!.lowercase()
                all.filter { it.title.lowercase().contains(kw) || it.description.lowercase().contains(kw) }
            }
            selectedCategory == Category.REGIONAL && selectedRegionalSource != null -> {
                all.filter { it.category == Category.REGIONAL && it.sourceName == selectedRegionalSource }
            }
            else -> all.filter { it.category == selectedCategory }
        }
        adapter.submitContentList(filtered)
        binding.tvEmpty.showIf(filtered.isEmpty() && viewModel.isLoading.value == false)
    }

    private fun openStream(item: ContentItem) {
        startActivity(Intent(requireContext(), StreamActivity::class.java)
            .putExtra(StreamActivity.EXTRA_CONTENT_ITEM, item))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
