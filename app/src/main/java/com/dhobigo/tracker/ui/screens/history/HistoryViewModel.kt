package com.dhobigo.tracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dhobigo.tracker.data.local.model.LaundryRecordWithItems
import com.dhobigo.tracker.data.local.model.LaundryStatus
import com.dhobigo.tracker.data.repository.LaundryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.Calendar

enum class DateFilterOption(val displayName: String) {
    ALL_TIME("All Time"),
    LAST_7_DAYS("Last 7 Days"),
    THIS_MONTH("This Month")
}

data class HistoryFilterState(
    val searchQuery: String = "",
    val statusFilter: LaundryStatus? = null,
    val dateFilter: DateFilterOption = DateFilterOption.ALL_TIME
)

class HistoryViewModel(private val repository: LaundryRepository) : ViewModel() {

    private val _filterState = MutableStateFlow(HistoryFilterState())
    val filterState: StateFlow<HistoryFilterState> = _filterState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val records: StateFlow<List<LaundryRecordWithItems>> = _filterState
        .flatMapLatest { filter ->
            val fromDate: Long? = when (filter.dateFilter) {
                DateFilterOption.ALL_TIME -> null
                DateFilterOption.LAST_7_DAYS -> {
                    Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
                }
                DateFilterOption.THIS_MONTH -> {
                    Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                }
            }

            repository.observeFilteredRecords(
                query = filter.searchQuery,
                status = filter.statusFilter?.name,
                fromDate = fromDate,
                toDate = null
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun onStatusFilterSelected(status: LaundryStatus?) {
        _filterState.update { it.copy(statusFilter = status) }
    }

    fun onDateFilterSelected(dateOption: DateFilterOption) {
        _filterState.update { it.copy(dateFilter = dateOption) }
    }

    fun resetFilters() {
        _filterState.update {
            HistoryFilterState()
        }
    }

    class Factory(private val repository: LaundryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
