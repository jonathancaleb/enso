package com.example.enso.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.enso.data.local.entity.Provider
import com.example.enso.data.local.entity.Transaction
import com.example.enso.di.AppModule
import com.example.enso.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProviderFilter(val label: String, val provider: Provider?) {
    ALL("All", null),
    MTN("MTN", Provider.MTN),
    AIRTEL("Airtel", Provider.AIRTEL)
}

data class SyncResult(val count: Int, val shown: Boolean = true)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppModule.provideRepository(application)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult: StateFlow<SyncResult?> = _syncResult.asStateFlow()

    private val _providerFilter = MutableStateFlow(ProviderFilter.ALL)
    val providerFilter: StateFlow<ProviderFilter> = _providerFilter.asStateFlow()

    private val todayStart = DateUtils.getDayStart(0)
    private val weekStart = DateUtils.getWeekBounds(0).first
    private val monthStart = DateUtils.getMonthStart(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val spentToday: StateFlow<Double> = _providerFilter.flatMapLatest { f ->
        if (f.provider != null) repository.getTotalSpentByProvider(todayStart, f.provider.name)
        else repository.getTotalSpentSince(todayStart)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val spentThisWeek: StateFlow<Double> = _providerFilter.flatMapLatest { f ->
        if (f.provider != null) repository.getTotalSpentByProvider(weekStart, f.provider.name)
        else repository.getTotalSpentSince(weekStart)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val spentThisMonth: StateFlow<Double> = _providerFilter.flatMapLatest { f ->
        if (f.provider != null) repository.getTotalSpentByProvider(monthStart, f.provider.name)
        else repository.getTotalSpentSince(monthStart)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val receivedThisMonth: StateFlow<Double> = _providerFilter.flatMapLatest { f ->
        if (f.provider != null) repository.getTotalReceivedByProvider(monthStart, f.provider.name)
        else repository.getTotalReceivedSince(monthStart)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val recentTransactions: StateFlow<List<Transaction>> = repository.getRecentTransactions(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionCount: StateFlow<Int> = repository.getTransactionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setProviderFilter(f: ProviderFilter) {
        _providerFilter.value = f
    }

    fun syncSms() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncResult.value = null
            try {
                val count = repository.syncFromSms()
                _syncResult.value = SyncResult(count)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun dismissSyncResult() {
        _syncResult.value = null
    }
}
