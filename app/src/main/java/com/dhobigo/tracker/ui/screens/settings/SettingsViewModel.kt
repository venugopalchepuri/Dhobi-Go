package com.dhobigo.tracker.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dhobigo.tracker.DhobiApplication
import com.dhobigo.tracker.data.auth.AuthRepository
import com.dhobigo.tracker.data.backup.BackupResult
import com.dhobigo.tracker.data.backup.RestoreResult
import com.dhobigo.tracker.data.local.entity.ClothingCategoryEntity
import com.dhobigo.tracker.data.local.entity.UserEntity
import com.dhobigo.tracker.data.repository.LaundryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val defaultDhobiName: String = "",
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val currentUser: UserEntity? = null,
    val isDeletingAccount: Boolean = false
)

sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
    data object AccountDeleted : SettingsEvent()
    data object LoggedOut : SettingsEvent()
}

class SettingsViewModel(
    private val repository: LaundryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(defaultDhobiName = repository.getDefaultDhobiName())
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<ClothingCategoryEntity>> = repository.observeAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUserFlow.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun updateDefaultDhobiName(name: String) {
        repository.setDefaultDhobiName(name)
        _uiState.update { it.copy(defaultDhobiName = name) }
    }

    fun updateProfile(
        fullName: String,
        phone: String?,
        college: String?,
        enrollmentNumber: String?,
        course: String?,
        academicYear: String?,
        batch: String?
    ) {
        viewModelScope.launch {
            val result = authRepository.updateProfile(
                fullName = fullName,
                phone = phone,
                college = college,
                enrollmentNumber = enrollmentNumber,
                course = course,
                academicYear = academicYear,
                batch = batch
            )
            when (result) {
                is com.dhobigo.tracker.data.auth.AuthResult.Success -> {
                    _events.emit(SettingsEvent.ShowToast("Profile updated successfully"))
                }
                is com.dhobigo.tracker.data.auth.AuthResult.Error -> {
                    _events.emit(SettingsEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        viewModelScope.launch {
            _events.emit(SettingsEvent.LoggedOut)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }
            val deleted = authRepository.deleteAccount()
            _uiState.update { it.copy(isDeletingAccount = false) }
            if (deleted) {
                _events.emit(SettingsEvent.ShowToast("Account and data permanently deleted"))
                _events.emit(SettingsEvent.AccountDeleted)
            } else {
                _events.emit(SettingsEvent.ShowToast("Failed to delete account. Please try again."))
            }
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.addCustomCategory(name)
            _events.emit(SettingsEvent.ShowToast("Category '$name' added"))
        }
    }

    fun toggleCategoryActive(category: ClothingCategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category.copy(isActive = !category.isActive))
        }
    }

    fun deleteCustomCategory(category: ClothingCategoryEntity) {
        if (category.isDefault) return
        viewModelScope.launch {
            repository.deleteCategory(category)
            _events.emit(SettingsEvent.ShowToast("Category '${category.name}' removed"))
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }
            val context = DhobiApplication.instance
            try {
                context.contentResolver.openOutputStream(uri)?.use { outStream ->
                    when (val result = repository.createBackup(outStream)) {
                        is BackupResult.Success -> {
                            _events.emit(
                                SettingsEvent.ShowToast("Backup created: ${result.recordCount} slips, ${result.imageCount} chit photos")
                            )
                        }
                        is BackupResult.Error -> {
                            _events.emit(SettingsEvent.ShowToast(result.message))
                        }
                    }
                } ?: run {
                    _events.emit(SettingsEvent.ShowToast("Cannot open output file for backup"))
                }
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowToast("Backup failed: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isBackingUp = false) }
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }
            val context = DhobiApplication.instance
            try {
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    when (val result = repository.restoreBackup(inStream)) {
                        is RestoreResult.Success -> {
                            _events.emit(
                                SettingsEvent.ShowToast("Restore completed: ${result.recordsRestored} slips restored")
                            )
                        }
                        is RestoreResult.Error -> {
                            _events.emit(SettingsEvent.ShowToast(result.message))
                        }
                    }
                } ?: run {
                    _events.emit(SettingsEvent.ShowToast("Cannot open selected backup file"))
                }
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowToast("Restore failed: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isRestoring = false) }
            }
        }
    }

    class Factory(
        private val repository: LaundryRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, authRepository) as T
        }
    }
}
