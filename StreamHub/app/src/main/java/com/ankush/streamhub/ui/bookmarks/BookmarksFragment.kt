package com.ankush.streamhub.ui.bookmarks

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import com.ankush.streamhub.StreamHubApp
import com.ankush.streamhub.data.local.CollectionEntity
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
    private var activeCollectionId: String? = null   // null = "All" chip selected
    private var currentBookmarks: LiveData<List<ContentItem>>? = null

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
            onShareClick     = { requireContext().showShareOptions(it.sourceUrl, it.title) },
            onSummarizeClick = { viewModel.summarizeItem(it) },
            onLongClick      = { item -> showMoveToCollectionDialog(item) }
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

        binding.btnNewCollection.setOnClickListener { showNewCollectionDialog() }

        viewModel.collections.observe(viewLifecycleOwner) { collections ->
            buildCollectionChips(collections)
        }

        observeBookmarks()

        lifecycleScope.launch {
            viewModel.summaries.collect { states -> adapter.updateSummaryStates(states) }
        }
    }

    private fun buildCollectionChips(collections: List<CollectionEntity>) {
        binding.chipGroupCollections.removeAllViews()

        // "All" chip
        val allChip = Chip(requireContext()).apply {
            text        = "📚 All"
            isCheckable = true
            isChecked   = activeCollectionId == null
            setOnClickListener { activeCollectionId = null; observeBookmarks() }
        }
        binding.chipGroupCollections.addView(allChip)

        // One chip per collection
        collections.forEach { col ->
            val chip = Chip(requireContext()).apply {
                text        = "${col.emoji} ${col.name}"
                isCheckable = true
                isChecked   = activeCollectionId == col.id
                setOnClickListener {
                    activeCollectionId = col.id
                    Analytics.collectionsViewed(col.name)
                    observeBookmarks()
                }
                setOnLongClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete \"${col.name}\"?")
                        .setMessage("Bookmarks in this collection will move to All.")
                        .setPositiveButton("Delete") { _, _ -> viewModel.deleteCollection(col) }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
            }
            binding.chipGroupCollections.addView(chip)
        }
    }

    private fun observeBookmarks() {
        // Remove previous observer
        currentBookmarks?.removeObservers(viewLifecycleOwner)

        val liveData = if (activeCollectionId == null) viewModel.bookmarks
                       else viewModel.getBookmarksByCollection(activeCollectionId!!)
        currentBookmarks = liveData

        liveData.observe(viewLifecycleOwner) { items ->
            adapter.submitContentList(items)
            adapter.updateBookmarkedIds(items.map { it.id }.toSet())
            binding.layoutEmpty.showIf(items.isEmpty())
            binding.recyclerView.showIf(items.isNotEmpty())
            binding.btnClearAll.showIf(items.isNotEmpty() && activeCollectionId == null)
            val label = when {
                activeCollectionId != null -> "${items.size} in collection"
                else                       -> "${items.size} saved"
            }
            binding.tvCount.text = label
        }
    }

    private fun showNewCollectionDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Collection name (e.g. Must Read)"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("📁 New Collection")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) {
                    viewModel.addCollection(name)
                    requireContext().toast("Collection \"$name\" created")
                } else {
                    requireContext().toast("Name cannot be empty")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMoveToCollectionDialog(item: ContentItem) {
        val collections = viewModel.collections.value.orEmpty()
        if (collections.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Move to Collection")
                .setMessage("No collections yet. Create one first via the + button.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val options = arrayOf("📚 Remove from collection") +
                      collections.map { "${it.emoji} ${it.name}" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Move \"${item.title.take(40)}...\"")
            .setItems(options) { _, which ->
                if (which == 0) viewModel.moveBookmarkToCollection(item.id, null)
                else            viewModel.moveBookmarkToCollection(item.id, collections[which - 1].id)
                requireContext().toast("Moved!")
            }
            .show()
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
