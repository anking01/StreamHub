package com.ankush.streamhub.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ankush.streamhub.data.remote.AppUser
import com.ankush.streamhub.databinding.FragmentAdminBinding
import com.ankush.streamhub.util.showIf
import com.ankush.streamhub.util.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        userAdapter = UserAdapter { user -> confirmRoleToggle(user) }
        binding.rvUsers.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.btnSendNotification.setOnClickListener {
            val title = binding.etNotifTitle.text?.toString().orEmpty().trim()
            val body  = binding.etNotifBody.text?.toString().orEmpty().trim()
            viewModel.sendNotification(title, body)
        }

        viewModel.users.observe(viewLifecycleOwner) { users ->
            userAdapter.submitList(users)
            binding.tvUserCount.text  = users.size.toString()
            binding.tvAdminCount.text = users.count { it.role == "admin" }.toString()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progress.showIf(state is AdminState.Loading)
            when (state) {
                is AdminState.Success -> {
                    requireContext().toast(state.message)
                    binding.etNotifTitle.text?.clear()
                    binding.etNotifBody.text?.clear()
                }
                is AdminState.Error -> requireContext().toast(state.message, true)
                else -> {}
            }
        }

        viewModel.loadUsers()
    }

    private fun confirmRoleToggle(user: AppUser) {
        val newRole = if (user.role == "admin") "user" else "admin"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Role Change")
            .setMessage("${user.name} ko $newRole banana chahte ho?")
            .setPositiveButton("Haan") { _, _ -> viewModel.toggleAdmin(user) }
            .setNegativeButton("Nahi", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
