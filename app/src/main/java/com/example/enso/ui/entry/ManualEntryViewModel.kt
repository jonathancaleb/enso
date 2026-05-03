package com.example.enso.ui.entry

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.enso.data.local.entity.Provider
import com.example.enso.data.local.entity.TransactionEntity
import com.example.enso.data.local.entity.TransactionType
import com.example.enso.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EntryFormState(
    val amount: String = "",
    val description: String = "",
    val type: String = TransactionType.TRANSFER_OUT,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

class ManualEntryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppModule.provideRepository(application)

    private val _state = MutableStateFlow(EntryFormState())
    val state: StateFlow<EntryFormState> = _state.asStateFlow()

    fun updateAmount(value: String) {
        _state.value = _state.value.copy(amount = value, error = null)
    }

    fun updateDescription(value: String) {
        _state.value = _state.value.copy(description = value, error = null)
    }

    fun updateType(type: String) {
        _state.value = _state.value.copy(type = type)
    }

    fun save() {
        val current = _state.value
        val amount = current.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _state.value = current.copy(error = "Enter a valid amount")
            return
        }
        if (current.description.isBlank()) {
            _state.value = current.copy(error = "Enter a description")
            return
        }

        viewModelScope.launch {
            _state.value = current.copy(isSaving = true)
            repository.addManualTransaction(
                TransactionEntity(
                    provider = Provider.MANUAL,
                    type = current.type,
                    amount = amount,
                    description = current.description.trim(),
                    date = System.currentTimeMillis()
                )
            )
            _state.value = EntryFormState(saved = true)
        }
    }
}
