package com.example.pdvmaquineta.domain.model

sealed class SessionState {
    data object LoggedOut : SessionState()
    data class Locked(val user: User) : SessionState()
    data class Active(val user: User) : SessionState()
}
