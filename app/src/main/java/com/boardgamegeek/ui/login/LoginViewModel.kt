package com.boardgamegeek.ui.login

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.model.AuthToken
import com.boardgamegeek.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private var authenticationJob: Job? = null

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _authenticationResult = MutableStateFlow<AuthToken?>(null)
    val authenticationResult: StateFlow<AuthToken?> = _authenticationResult.asStateFlow()

    fun login(username: String?, password: String?) {
        _isAuthenticating.value = true
        authenticationJob = viewModelScope.launch(Dispatchers.IO) {
            _authenticationResult.value = authRepository.authenticate(username.orEmpty(), password.orEmpty(), "Dialog")
            _isAuthenticating.value = false
        }
    }

    fun clearAuthenticationResult() {
        _authenticationResult.value = null
    }

    fun cancel() {
        authenticationJob?.cancel()
        _isAuthenticating.value = false
    }
}
