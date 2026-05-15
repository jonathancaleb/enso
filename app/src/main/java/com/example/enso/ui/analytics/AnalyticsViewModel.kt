package com.example.enso.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.enso.data.local.TypeTotal
import com.example.enso.data.local.entity.Provider
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

enum class TimePeriod(val label: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THREE_MONTHS("3 Months")
}

enum class AnalyticsProviderFilter(val label: String, val provider: Provider?) {
    ALL("All", null),
    MTN("MTN", Provider.MTN),
    AIRTEL("Airtel", Provider.AIRTEL)
}

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppModule.provideRepository(application)

    private val _period = MutableStateFlow(TimePeriod.MONTH)
    val period: StateFlow<TimePeriod> = _period.asStateFlow()

    private val _providerFilter = MutableStateFlow(AnalyticsProviderFilter.ALL)
    val providerFilter: StateFlow<AnalyticsProviderFilter> = _providerFilter.asStateFlow()

    private val filterState = combine(_period, _providerFilter) { p, pf -> Pair(p, pf) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val breakdown: StateFlow<List<TypeTotal>> = filterState.flatMapLatest { (p, pf) ->
        val (start, end) = getBounds(p)
        if (pf.provider != null) repository.getTypeTotalsByProvider(start, end, pf.provider)
        else repository.getTypeTotals(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalSpent: StateFlow<Double> = filterState.flatMapLatest { (p, pf) ->
        val (start, end) = getBounds(p)
        if (pf.provider != null) repository.getTotalSpentByProvider(start, end, pf.provider)
        else repository.getTotalSpent(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalReceived: StateFlow<Double> = filterState.flatMapLatest { (p, pf) ->
        val (start, end) = getBounds(p)
        if (pf.provider != null) repository.getTotalReceivedByProvider(start, end, pf.provider)
        else repository.getTotalReceived(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setPeriod(p: TimePeriod) { _period.value = p }
    fun setProviderFilter(f: AnalyticsProviderFilter) { _providerFilter.value = f }

    private fun getBounds(p: TimePeriod): Pair<Long, Long> {
        return when (p) {
            TimePeriod.TODAY -> DateUtils.getDayStart(0) to DateUtils.getDayStart(-1)
            TimePeriod.WEEK -> DateUtils.getWeekBounds(0)
            TimePeriod.MONTH -> DateUtils.getMonthStart(0) to DateUtils.getMonthStart(-1)
            TimePeriod.LAST_MONTH -> DateUtils.getMonthStart(1) to DateUtils.getMonthStart(0)
            TimePeriod.THREE_MONTHS -> DateUtils.getMonthStart(2) to DateUtils.getMonthStart(-1)
        }
    }
}
