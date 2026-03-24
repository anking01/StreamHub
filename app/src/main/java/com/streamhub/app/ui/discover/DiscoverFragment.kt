package com.streamhub.app.ui.discover

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.streamhub.app.StreamHubApp
import com.streamhub.app.data.model.Category
import com.streamhub.app.data.model.ContentItem
import com.streamhub.app.databinding.FragmentDiscoverBinding
import com.streamhub.app.ui.SharedViewModel
import com.streamhub.app.ui.SharedViewModelFactory
import com.streamhub.app.ui.home.FeedAdapter
import com.streamhub.app.ui.stream.StreamActivity
import com.streamhub.app.util.*
import kotlinx.coroutines.launch

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by activityViewModels {
        SharedViewModelFactory(requireActivity().application as StreamHubApp)
    }

    private lateinit var adapter: FeedAdapter
    private var selectedCategory: Category = Category.VIDEOS

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FeedAdapter(
            onItemClick     = { openStream(it) },
            onBookmarkClick = { viewModel.toggleBookmark(it) },
            onShareClick    = { requireContext().shareUrl(it.sourceUrl, it.title) }
        )
        binding.recyclerView.apply {
            adapter = this@DiscoverFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        Category.entries.drop(1).forEachIndexed { i, category ->
            Chip(requireContext()).apply {
                text = "${category.emoji} ${category.label}"
                isCheckable = true
                isChecked = i == 0
                setOnClickListener { selectedCategory = category; filterAndSubmit() }
            }.also { binding.chipGroupDiscover.addView(it) }
        }

        viewModel.feedItems.observe(viewLifecycleOwner) { filterAndSubmit() }
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.progressBar.showIf(it) }

        lifecycleScope.launch {
            viewModel.bookmarkIds.collect { ids -> adapter.updateBookmarkedIds(ids) }
        }
    }

    private fun filterAndSubmit() {
        val filtered = viewModel.feedItems.value.orEmpty().filter { it.category == selectedCategory }
        adapter.submitList(filtered)
        binding.tvEmpty.showIf(filtered.isEmpty() && viewModel.isLoading.value == false)
    }

    private fun openStream(item: ContentItem) {
        startActivity(Intent(requireContext(), StreamActivity::class.java)
            .putExtra(StreamActivity.EXTRA_CONTENT_ITEM, item))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
