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
import java.util.Calendar

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
        if (pf.provider != null) repository.getTypeTotalsByProvider(start, end, pf.provider.name)
        else repository.getTypeTotals(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalSpent: StateFlow<Double> = filterState.flatMapLatest { (p, pf) ->
        val start = getBounds(p).first
        if (pf.provider != null) repository.getTotalSpentByProvider(start, pf.provider.name)
        else repository.getTotalSpentSince(start)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalReceived: StateFlow<Double> = filterState.flatMapLatest { (p, pf) ->
        val start = getBounds(p).first
        if (pf.provider != null) repository.getTotalReceivedByProvider(start, pf.provider.name)
        else repository.getTotalReceivedSince(start)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setPeriod(p: TimePeriod) { _period.value = p }
    fun setProviderFilter(f: AnalyticsProviderFilter) { _providerFilter.value = f }

    private fun getBounds(p: TimePeriod): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return when (p) {
            TimePeriod.TODAY -> DateUtils.getDayStart(0) to now
            TimePeriod.WEEK -> DateUtils.getWeekBounds(0)
            TimePeriod.MONTH -> DateUtils.getMonthStart(0) to now
            TimePeriod.LAST_MONTH -> {
                val start = DateUtils.getMonthStart(1)
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                start to (cal.timeInMillis - 1)
            }
            TimePeriod.THREE_MONTHS -> DateUtils.getMonthStart(2) to now
        }
    }
}
