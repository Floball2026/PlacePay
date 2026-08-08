package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.domain.model.ThemeTone
import com.example.pdvmaquineta.domain.model.next
import com.example.pdvmaquineta.domain.repository.UserRepository
import com.example.pdvmaquineta.domain.session.SessionManager
import javax.inject.Inject

// Preferência visual, não ação sensível — sem auditoria. Atualiza a sessão em
// memória na hora (refreshActiveUser) pra o tema mudar sem precisar de novo
// login, mesmo mecanismo já usado pela troca de PIN.
class UpdateThemeToneUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): ThemeTone? {
        val actor = (sessionManager.state.value as? SessionState.Active)?.user ?: return null
        val next = actor.themeTone.next()
        userRepository.setThemeTone(actor.id, next)
        sessionManager.refreshActiveUser(actor.copy(themeTone = next))
        return next
    }
}
