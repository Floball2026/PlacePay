package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.repository.UserRepository
import javax.inject.Inject

// Usado pela tela de login/desbloqueio pra decidir qual teclado mostrar
// (numérico de PIN ou campo de senha) a partir do username digitado.
class CheckHasPinUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(username: String): Boolean = userRepository.hasPinSet(username)
}
