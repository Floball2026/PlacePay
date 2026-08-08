package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.CashSession
import com.example.pdvmaquineta.domain.repository.CashSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveOpenCashSessionUseCase @Inject constructor(
    private val cashSessionRepository: CashSessionRepository
) {
    operator fun invoke(): Flow<CashSession?> = cashSessionRepository.observeOpenSession()
}
