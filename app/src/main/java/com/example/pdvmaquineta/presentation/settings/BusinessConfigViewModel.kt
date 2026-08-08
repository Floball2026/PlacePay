package com.example.pdvmaquineta.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pdvmaquineta.data.sync.BusinessConfigStore
import com.example.pdvmaquineta.domain.model.BusinessConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BusinessConfigViewModel @Inject constructor(
    private val store: BusinessConfigStore
) : ViewModel() {

    var config by mutableStateOf(store.get())
        private set

    var savedMessage by mutableStateOf<String?>(null)
        private set

    fun onChange(newConfig: BusinessConfig) {
        config = newConfig
        savedMessage = null
    }

    fun save() {
        store.save(config)
        config = store.get()
        savedMessage = "Configurações salvas"
    }
}
