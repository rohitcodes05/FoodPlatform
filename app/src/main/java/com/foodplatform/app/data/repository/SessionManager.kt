package com.foodplatform.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SessionState {
    UNKNOWN,
    AUTHENTICATED,
    UNAUTHENTICATED
}

object SessionManager {
    private val _sessionState = MutableStateFlow(SessionState.UNKNOWN)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun setAuthenticated() {
        _sessionState.value = SessionState.AUTHENTICATED
    }

    fun setUnauthenticated() {
        _sessionState.value = SessionState.UNAUTHENTICATED
    }
}
