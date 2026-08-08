package com.example.pdvmaquineta.domain.usecase

import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.domain.repository.UserRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<List<User>> = userRepository.observeAll()
}
