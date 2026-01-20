package com.celdy.groufr.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableLiveData<LoginState>(LoginState.Idle)
    val state: LiveData<LoginState> = _state

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("Email and password are required.")
            return
        }
        _state.value = LoginState.Loading
        viewModelScope.launch {
            try {
                authRepository.login(email.trim(), password)
                _state.value = LoginState.Success
            } catch (exception: Exception) {
                _state.value = LoginState.Error("Login failed. Check your credentials.")
            }
        }
    }
}

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
