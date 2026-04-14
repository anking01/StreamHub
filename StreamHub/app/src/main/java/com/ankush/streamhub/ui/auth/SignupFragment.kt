package com.ankush.streamhub.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ankush.streamhub.R
import com.ankush.streamhub.databinding.FragmentSignupBinding
import com.ankush.streamhub.ui.MainActivity
import com.ankush.streamhub.util.showIf

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSignup.setOnClickListener {
            val name     = binding.etName.text?.toString().orEmpty().trim()
            val email    = binding.etEmail.text?.toString().orEmpty().trim()
            val password = binding.etPassword.text?.toString().orEmpty()
            viewModel.signup(name, email, password)
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_login)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            binding.progress.showIf(state is AuthState.Loading)
            binding.btnSignup.isEnabled = state !is AuthState.Loading

            when (state) {
                is AuthState.Success -> goToMain()
                is AuthState.Error   -> showError(state.message)
                else                 -> hideError()
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(requireContext(), MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        requireActivity().finish()
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
