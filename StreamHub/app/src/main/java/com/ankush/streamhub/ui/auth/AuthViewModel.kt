package com.ankush.streamhub.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ankush.streamhub.data.remote.FirestoreRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle    : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestoreRepo = FirestoreRepository()

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    val isLoggedIn: Boolean get() = auth.currentUser != null

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email aur password dono chahiye")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun signup(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Saari fields fill karo")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password kam se kam 6 characters ka hona chahiye")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("User creation failed")
                firestoreRepo.createUserDocument(uid, name, email)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Pehle email daalo")
            return
        }
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.Error("Reset email bhej diya! Inbox check karo.")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun resetState() { _authState.value = AuthState.Idle }

    private fun friendlyError(msg: String?): String = when {
        msg == null -> "Kuch galat hua"
        msg.contains("INVALID_LOGIN_CREDENTIALS") || msg.contains("no user record") ->
            "Email ya password galat hai"
        msg.contains("EMAIL_EXISTS") || msg.contains("email address is already in use") ->
            "Yeh email pehle se registered hai"
        msg.contains("WEAK_PASSWORD") ->
            "Password thoda strong rakho (6+ characters)"
        msg.contains("INVALID_EMAIL") ->
            "Valid email address daalo"
        msg.contains("network") || msg.contains("NETWORK") ->
            "Internet connection check karo"
        else -> msg
    }
}
