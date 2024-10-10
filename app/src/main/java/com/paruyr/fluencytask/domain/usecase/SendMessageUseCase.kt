package com.paruyr.fluencytask.domain.usecase

import com.paruyr.fluencytask.data.BluetoothRepository
import com.paruyr.fluencytask.domain.model.FluencyMessage

class SendMessageUseCase(private val repository: BluetoothRepository) {
    suspend operator fun invoke(message: FluencyMessage) {
        // Add any additional logic or processing here if needed
        repository.sendMessage(message)  // Delegate to the repository
    }
}