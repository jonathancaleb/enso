package com.example.enso.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.enso.data.local.entity.Transaction
import com.example.enso.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SyncStatus(val count: Int)

class TransactionListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppModule.provideRepository(application)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus?>(null)
    val syncStatus: StateFlow<SyncStatus?> = _syncStatus.asStateFlow()

    val transactions = repository.getAllTransactions()

    fun syncSms() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = null
            try {
                val count = repository.syncFromSms()
                _syncStatus.value = SyncStatus(count)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun dismissSyncStatus() {
        _syncStatus.value = null
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction.id)
        }
    }
}
