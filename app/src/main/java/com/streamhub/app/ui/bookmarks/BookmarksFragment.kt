package com.streamhub.app.ui.bookmarks

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.streamhub.app.StreamHubApp
import com.streamhub.app.data.model.ContentItem
import com.streamhub.app.databinding.FragmentBookmarksBinding
import com.streamhub.app.ui.SharedViewModel
import com.streamhub.app.ui.SharedViewModelFactory
import com.streamhub.app.ui.home.FeedAdapter
import com.streamhub.app.ui.stream.StreamActivity
import com.streamhub.app.util.*

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
            onItemClick = { openStream(it) },
            onBookmarkClick = {
                viewModel.toggleBookmark(it)
                requireContext().toast("Removed from bookmarks")
            },
            onShareClick = { requireContext().shareUrl(it.sourceUrl, it.title) }
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
            adapter.submitList(items)
            // All items in bookmarks screen are bookmarked by definition
            adapter.updateBookmarkedIds(items.map { it.id }.toSet())
            binding.layoutEmpty.showIf(items.isEmpty())
            binding.recyclerView.showIf(items.isNotEmpty())
            binding.btnClearAll.showIf(items.isNotEmpty())
            binding.tvCount.text = "${items.size} saved"
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
