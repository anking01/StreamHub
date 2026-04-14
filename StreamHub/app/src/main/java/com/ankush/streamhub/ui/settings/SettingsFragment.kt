package com.ankush.streamhub.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.ankush.streamhub.AppPreferences
import com.ankush.streamhub.R
import com.ankush.streamhub.StreamHubApp
import com.ankush.streamhub.data.model.*
import com.ankush.streamhub.data.remote.FirestoreRepository
import com.ankush.streamhub.databinding.FragmentSettingsBinding
import com.ankush.streamhub.ui.SharedViewModel
import com.ankush.streamhub.ui.SharedViewModelFactory
import com.ankush.streamhub.ui.auth.AuthActivity
import com.ankush.streamhub.util.*
import kotlinx.coroutines.launch
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

    private val prefs by lazy { (requireActivity().application as StreamHubApp).preferences }
    private val firestoreRepo by lazy { FirestoreRepository() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAccountSection()
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

        setupAiProviderSettings()

        binding.tvVersion.text = "StreamHub v1.0.0 • Built with ❤️ in Kotlin"
    }

    private fun setupAccountSection() {
        // Check if current user is admin → show Admin Panel button
        lifecycleScope.launch {
            val isAdmin = firestoreRepo.isCurrentUserAdmin()
            binding.btnAdminPanel.showIf(isAdmin)
        }

        binding.btnAdminPanel.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_admin)
        }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Logout karna chahte ho?")
                .setPositiveButton("Haan") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(requireContext(), AuthActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    requireActivity().finish()
                }
                .setNegativeButton("Nahi", null)
                .show()
        }
    }

    private fun setupAiProviderSettings() {
        // Set current provider selection
        when (prefs.aiProvider) {
            AppPreferences.PROVIDER_GROQ   -> binding.rgAiProvider.check(R.id.rb_groq)
            AppPreferences.PROVIDER_OLLAMA -> binding.rgAiProvider.check(R.id.rb_ollama)
            else                           -> binding.rgAiProvider.check(R.id.rb_gemini)
        }

        updateKeyFieldVisibility(prefs.aiProvider)

        if (prefs.groqApiKey.isNotBlank())   binding.etGroqKey.setText(prefs.groqApiKey)
        if (prefs.ollamaApiKey.isNotBlank()) binding.etOllamaKey.setText(prefs.ollamaApiKey)

        binding.rgAiProvider.setOnCheckedChangeListener { _, checkedId ->
            val provider = when (checkedId) {
                R.id.rb_groq   -> AppPreferences.PROVIDER_GROQ
                R.id.rb_ollama -> AppPreferences.PROVIDER_OLLAMA
                else           -> AppPreferences.PROVIDER_GEMINI
            }
            prefs.aiProvider = provider
            updateKeyFieldVisibility(provider)
            requireContext().toast(when (provider) {
                AppPreferences.PROVIDER_GROQ   -> "⚡ Groq selected"
                AppPreferences.PROVIDER_OLLAMA -> "🦙 Ollama selected"
                else                           -> "✨ Gemini selected"
            })
        }

        binding.btnSaveGroqKey.setOnClickListener {
            val key = binding.etGroqKey.text?.toString()?.trim().orEmpty()
            if (key.isBlank()) { requireContext().toast("API key cannot be empty"); return@setOnClickListener }
            prefs.groqApiKey = key
            prefs.aiProvider = AppPreferences.PROVIDER_GROQ
            binding.rgAiProvider.check(R.id.rb_groq)
            requireContext().toast("✓ Groq API key saved!")
        }

        binding.btnSaveOllamaKey.setOnClickListener {
            val key = binding.etOllamaKey.text?.toString()?.trim().orEmpty()
            if (key.isBlank()) { requireContext().toast("API key cannot be empty"); return@setOnClickListener }
            prefs.ollamaApiKey = key
            prefs.aiProvider = AppPreferences.PROVIDER_OLLAMA
            binding.rgAiProvider.check(R.id.rb_ollama)
            requireContext().toast("✓ Ollama API key saved!")
        }
    }

    private fun updateKeyFieldVisibility(provider: String) {
        binding.tilGroqKey.showIf(provider == AppPreferences.PROVIDER_GROQ)
        binding.btnSaveGroqKey.showIf(provider == AppPreferences.PROVIDER_GROQ)
        binding.tilOllamaKey.showIf(provider == AppPreferences.PROVIDER_OLLAMA)
        binding.btnSaveOllamaKey.showIf(provider == AppPreferences.PROVIDER_OLLAMA)
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
