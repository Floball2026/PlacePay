package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.TerminalConfig
import com.example.pdvmaquineta.domain.repository.TerminalConfigRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTerminalConfigUseCase @Inject constructor(
    private val terminalConfigRepository: TerminalConfigRepository
) {
    operator fun invoke(): Flow<TerminalConfig?> = terminalConfigRepository.observeConfig()
}
