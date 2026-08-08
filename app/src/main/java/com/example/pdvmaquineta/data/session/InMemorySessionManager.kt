package com.example.pdvmaquineta.data.session

import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class InMemorySessionManager @Inject constructor() : SessionManager {
    private val _state = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override fun start(user: User) {
        _state.value = SessionState.Active(user)
    }

    override fun lock() {
        val current = _state.value
        if (current is SessionState.Active) {
            _state.value = SessionState.Locked(current.user)
        }
    }

    override fun unlock() {
        val current = _state.value
        if (current is SessionState.Locked) {
            _state.value = SessionState.Active(current.user)
        }
    }

    override fun clear() {
        _state.value = SessionState.LoggedOut
    }

    override fun refreshActiveUser(user: User) {
        val current = _state.value
        if (current is SessionState.Active) {
            _state.value = SessionState.Active(user)
        }
    }
}
