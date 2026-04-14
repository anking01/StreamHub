package com.ankush.streamhub.ui.bookmarks

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import com.ankush.streamhub.StreamHubApp
import com.ankush.streamhub.data.model.ContentItem
import com.ankush.streamhub.databinding.FragmentBookmarksBinding
import com.ankush.streamhub.ui.SharedViewModel
import com.ankush.streamhub.ui.SharedViewModelFactory
import com.ankush.streamhub.ui.home.FeedAdapter
import com.ankush.streamhub.ui.stream.StreamActivity
import com.ankush.streamhub.util.*

class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by activityViewModels {
        SharedViewModelFactory(requireActivity().application as StreamHubApp)
    }

    private lateinit var adapter: FeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FeedAdapter(
            onItemClick      = { openStream(it) },
            onBookmarkClick  = {
                viewModel.toggleBookmark(it)
                requireContext().toast("Removed from bookmarks")
            },
            onShareClick     = { requireContext().shareUrl(it.sourceUrl, it.title) },
            onSummarizeClick = { viewModel.summarizeItem(it) }
        )

        binding.recyclerView.apply {
            adapter = this@BookmarksFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.btnClearAll.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear All Bookmarks")
                .setMessage("Remove all saved bookmarks?")
                .setPositiveButton("Clear") { _, _ ->
                    viewModel.clearAllBookmarks()
                    requireContext().toast("All bookmarks cleared")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewModel.bookmarks.observe(viewLifecycleOwner) { items ->
            adapter.submitContentList(items)
            adapter.updateBookmarkedIds(items.map { it.id }.toSet())
            binding.layoutEmpty.showIf(items.isEmpty())
            binding.recyclerView.showIf(items.isNotEmpty())
            binding.btnClearAll.showIf(items.isNotEmpty())
            binding.tvCount.text = "${items.size} saved"
        }
        lifecycleScope.launch {
            viewModel.summaries.collect { states -> adapter.updateSummaryStates(states) }
        }
    }

    private fun openStream(item: ContentItem) {
        startActivity(Intent(requireContext(), StreamActivity::class.java).apply {
            putExtra(StreamActivity.EXTRA_CONTENT_ITEM, item)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
