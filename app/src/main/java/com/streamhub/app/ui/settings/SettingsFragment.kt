package com.streamhub.app.ui.settings

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.streamhub.app.R
import com.streamhub.app.StreamHubApp
import com.streamhub.app.data.model.*
import com.streamhub.app.databinding.FragmentSettingsBinding
import com.streamhub.app.ui.SharedViewModel
import com.streamhub.app.ui.SharedViewModelFactory
import com.streamhub.app.util.*
import java.util.UUID

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by activityViewModels {
        SharedViewModelFactory(requireActivity().application as StreamHubApp)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddFeed.setOnClickListener { showAddFeedDialog() }

        binding.btnClearBookmarks.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear All Bookmarks")
                .setMessage("This will remove all saved bookmarks.")
                .setPositiveButton("Clear") { _, _ ->
                    viewModel.clearAllBookmarks()
                    requireContext().toast("Bookmarks cleared")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewModel.feedSources.observe(viewLifecycleOwner) { sources ->
            renderFeedList(sources)
        }

        binding.tvVersion.text = "StreamHub v1.0.0 • Built with ❤️ in Kotlin"
    }

    private fun renderFeedList(sources: List<FeedConfig>) {
        binding.layoutFeeds.removeAllViews()

        sources.forEach { config ->
            val row = layoutInflater.inflate(
                R.layout.item_feed_source, binding.layoutFeeds, false
            )

            row.findViewById<android.widget.TextView>(R.id.tv_feed_name).text = config.name
            row.findViewById<android.widget.TextView>(R.id.tv_feed_url).text = config.url

            val sw = row.findViewById<SwitchMaterial>(R.id.switch_feed)
            sw.isChecked = config.isActive
            sw.setOnCheckedChangeListener { _, _ ->
                viewModel.toggleFeedSource(config.id)
            }

            val deleteBtn = row.findViewById<android.widget.ImageButton>(R.id.btn_delete_feed)
            deleteBtn.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Remove Feed")
                    .setMessage("Remove \"${config.name}\"?")
                    .setPositiveButton("Remove") { _, _ ->
                        viewModel.removeFeedSource(config.id)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            binding.layoutFeeds.addView(row)
        }
    }

    private fun showAddFeedDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_feed, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_feed_name)
        val etUrl  = dialogView.findViewById<TextInputEditText>(R.id.et_feed_url)

        AlertDialog.Builder(requireContext())
            .setTitle("➕ Add Feed Source")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text?.toString()?.trim().orEmpty()
                val url  = etUrl.text?.toString()?.trim().orEmpty()

                if (name.isBlank() || url.isBlank()) {
                    requireContext().toast("Name and URL are required")
                    return@setPositiveButton
                }
                if (!url.startsWith("http")) {
                    requireContext().toast("Please enter a valid URL starting with http(s)://")
                    return@setPositiveButton
                }

                viewModel.addFeedSource(
                    FeedConfig(
                        id       = UUID.randomUUID().toString(),
                        name     = name,
                        url      = url,
                        source   = FeedSource.RSS,
                        category = Category.ALL
                    )
                )
                requireContext().toast("✓ Feed \"$name\" added!")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
