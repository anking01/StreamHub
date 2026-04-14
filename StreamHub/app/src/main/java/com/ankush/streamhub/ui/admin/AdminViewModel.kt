package com.ankush.streamhub.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankush.streamhub.data.remote.AppUser
import com.ankush.streamhub.data.remote.FirestoreRepository
import kotlinx.coroutines.launch

sealed class AdminState {
    object Idle    : AdminState()
    object Loading : AdminState()
    data class Success(val message: String) : AdminState()
    data class Error(val message: String)   : AdminState()
}

class AdminViewModel : ViewModel() {

    private val repo = FirestoreRepository()

    private val _users = MutableLiveData<List<AppUser>>(emptyList())
    val users: LiveData<List<AppUser>> = _users

    private val _state = MutableLiveData<AdminState>(AdminState.Idle)
    val state: LiveData<AdminState> = _state

    fun loadUsers() {
        _state.value = AdminState.Loading
        viewModelScope.launch {
            try {
                _users.value = repo.getAllUsers()
                _state.value = AdminState.Idle
            } catch (e: Exception) {
                _state.value = AdminState.Error("Users load nahi hue: ${e.message}")
            }
        }
    }

    fun toggleAdmin(user: AppUser) {
        viewModelScope.launch {
            try {
                if (user.role == "admin") repo.removeAdmin(user.uid)
                else repo.makeAdmin(user.uid)
                loadUsers()
                _state.value = AdminState.Success(
                    if (user.role == "admin") "${user.name} ko user bana diya"
                    else "${user.name} ko admin bana diya"
                )
            } catch (e: Exception) {
                _state.value = AdminState.Error("Role change nahi hua: ${e.message}")
            }
        }
    }

    fun sendNotification(title: String, body: String) {
        if (title.isBlank() || body.isBlank()) {
            _state.value = AdminState.Error("Title aur message dono chahiye")
            return
        }
        _state.value = AdminState.Loading
        viewModelScope.launch {
            try {
                repo.queueNotification(title, body)
                _state.value = AdminState.Success("Notification queue mein add ho gaya!")
            } catch (e: Exception) {
                _state.value = AdminState.Error("Notification queue nahi hua: ${e.message}")
            }
        }
    }
}
