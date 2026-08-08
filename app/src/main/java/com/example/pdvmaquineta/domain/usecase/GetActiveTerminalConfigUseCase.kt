package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.TerminalConfig
import com.example.pdvmaquineta.domain.repository.TerminalConfigRepository
import javax.inject.Inject

class GetActiveTerminalConfigUseCase @Inject constructor(
    private val terminalConfigRepository: TerminalConfigRepository
) {
    suspend operator fun invoke(): TerminalConfig? = terminalConfigRepository.getConfig()
}
