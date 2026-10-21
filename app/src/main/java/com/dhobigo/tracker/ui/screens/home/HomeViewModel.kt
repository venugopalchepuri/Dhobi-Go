package com.dhobigo.tracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dhobigo.tracker.data.auth.AuthRepository
import com.dhobigo.tracker.data.local.entity.UserEntity
import com.dhobigo.tracker.data.local.model.DashboardStats
import com.dhobigo.tracker.data.local.model.LaundryRecordWithItems
import com.dhobigo.tracker.data.repository.LaundryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val repository: LaundryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> = authRepository.currentUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val dashboardStats: StateFlow<DashboardStats> = repository.observeDashboardStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardStats()
        )

    val recentRecords: StateFlow<List<LaundryRecordWithItems>> = repository.observeRecentRecords(5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    class Factory(
        private val repository: LaundryRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository, authRepository) as T
        }
    }
}
