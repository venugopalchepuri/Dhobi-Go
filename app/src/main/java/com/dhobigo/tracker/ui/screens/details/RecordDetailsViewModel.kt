package com.dhobigo.tracker.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dhobigo.tracker.data.local.model.LaundryRecordWithItems
import com.dhobigo.tracker.data.repository.LaundryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class RecordDetailsEvent {
    data object RecordDeleted : RecordDetailsEvent()
    data class ShowToast(val message: String) : RecordDetailsEvent()
}

class RecordDetailsViewModel(
    private val repository: LaundryRepository,
    val recordId: String
) : ViewModel() {

    val recordWithItems: StateFlow<LaundryRecordWithItems?> = repository.observeRecordById(recordId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _events = MutableSharedFlow<RecordDetailsEvent>()
    val events: SharedFlow<RecordDetailsEvent> = _events.asSharedFlow()

    fun updateCollection(returnedQuantitiesByItemId: Map<Long, Int>, forceCollected: Boolean = false) {
        viewModelScope.launch {
            try {
                repository.updateCollectionProgress(recordId, returnedQuantitiesByItemId, forceCollected)
                _events.emit(RecordDetailsEvent.ShowToast("Collection status updated"))
            } catch (e: Exception) {
                _events.emit(RecordDetailsEvent.ShowToast("Error updating status: ${e.message}"))
            }
        }
    }

    fun markAllReturned() {
        viewModelScope.launch {
            try {
                repository.markAllItemsReturned(recordId)
                _events.emit(RecordDetailsEvent.ShowToast("All items marked as collected"))
            } catch (e: Exception) {
                _events.emit(RecordDetailsEvent.ShowToast("Error: ${e.message}"))
            }
        }
    }

    fun deleteRecord() {
        viewModelScope.launch {
            try {
                repository.deleteRecord(recordId)
                _events.emit(RecordDetailsEvent.RecordDeleted)
            } catch (e: Exception) {
                _events.emit(RecordDetailsEvent.ShowToast("Failed to delete record: ${e.message}"))
            }
        }
    }

    class Factory(
        private val repository: LaundryRepository,
        private val recordId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecordDetailsViewModel(repository, recordId) as T
        }
    }
}
