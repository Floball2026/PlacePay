package com.example.pdvmaquineta.domain.session

import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.model.User
import kotlinx.coroutines.flow.StateFlow

// Estado de sessão vive só em memória (singleton de app) por enquanto:
// persistir sessão entre reinícios de processo é preocupação de Fase 9,
// não de Fase 1.
interface SessionManager {
    val state: StateFlow<SessionState>

    fun start(user: User)
    fun lock()
    fun unlock()
    fun clear()

    // Atualiza o usuário da sessão ativa sem exigir novo login — usado depois
    // que o próprio usuário logado muda seu PIN (ex: troca forçada), já que o
    // snapshot de User guardado em memória não reflete sozinho mudanças no
    // banco.
    fun refreshActiveUser(user: User)
}
