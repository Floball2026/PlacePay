package com.example.pdvmaquineta.domain.model

sealed class CredentialResult {
    data class Success(val user: User) : CredentialResult()
    data object InvalidCredentials : CredentialResult()
    data object UserInactive : CredentialResult()
}
