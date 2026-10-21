package com.dhobigo.tracker.ui.screens.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dhobigo.tracker.data.local.entity.ClothingCategoryEntity
import com.dhobigo.tracker.data.local.entity.LaundryItemEntity
import com.dhobigo.tracker.data.local.entity.LaundryRecordEntity
import com.dhobigo.tracker.data.ocr.ExtractedChitData
import com.dhobigo.tracker.data.ocr.TextRecognitionManager
import com.dhobigo.tracker.data.repository.LaundryRepository
import com.dhobigo.tracker.data.storage.ImageStorageManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class AddLaundryUiState(
    val recordId: String = "",
    val isEditMode: Boolean = false,
    val dateGivenMillis: Long = System.currentTimeMillis(),
    val slipNumber: String = "",
    val dhobiName: String = "",
    val amountText: String = "",
    val expectedReturnDateMillis: Long? = null,
    val notes: String = "",
    val chitImagePath: String? = null,
    val pendingImageUri: Uri? = null,
    val quantities: Map<String, Int> = emptyMap(),
    val categories: List<ClothingCategoryEntity> = emptyList(),
    val isSaving: Boolean = false,
    val isScanningOcr: Boolean = false,
    val errorMessage: String? = null,
    val duplicateSlipWarning: String? = null,
    val hasUnsavedChanges: Boolean = false
) {
    val totalItems: Int
        get() = quantities.values.sum()
}

sealed class AddLaundryEvent {
    data object SaveSuccess : AddLaundryEvent()
    data class NavigateToOcrConfirmation(val extractedData: ExtractedChitData, val imageFile: File) : AddLaundryEvent()
    data class ShowToast(val message: String) : AddLaundryEvent()
}

class AddLaundryViewModel(
    private val repository: LaundryRepository,
    private val imageStorageManager: ImageStorageManager,
    private val textRecognitionManager: TextRecognitionManager,
    private val existingRecordId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddLaundryUiState())
    val uiState: StateFlow<AddLaundryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddLaundryEvent>()
    val events: SharedFlow<AddLaundryEvent> = _events.asSharedFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val activeCategories = repository.observeActiveCategories().first()
            val defaultDhobi = repository.getDefaultDhobiName()

            if (!existingRecordId.isNullOrBlank()) {
                val existing = repository.getRecordById(existingRecordId)
                if (existing != null) {
                    val record = existing.record
                    val quantityMap = existing.items.associate { it.categoryName to it.quantityGiven }
                    _uiState.update {
                        it.copy(
                            recordId = record.id,
                            isEditMode = true,
                            dateGivenMillis = record.dateGivenMillis,
                            slipNumber = record.slipNumber ?: "",
                            dhobiName = record.dhobiName ?: defaultDhobi,
                            amountText = record.amount?.toString() ?: "",
                            expectedReturnDateMillis = record.expectedReturnDateMillis,
                            notes = record.notes ?: "",
                            chitImagePath = record.chitImagePath,
                            quantities = quantityMap,
                            categories = activeCategories
                        )
                    }
                    return@launch
                }
            }

            val nextId = repository.generateNextRecordId()
            _uiState.update {
                it.copy(
                    recordId = nextId,
                    isEditMode = false,
                    dhobiName = defaultDhobi,
                    categories = activeCategories
                )
            }
        }
    }

    fun onDateGivenChanged(millis: Long) {
        _uiState.update { it.copy(dateGivenMillis = millis, hasUnsavedChanges = true) }
    }

    fun onSlipNumberChanged(value: String) {
        _uiState.update { it.copy(slipNumber = value, duplicateSlipWarning = null, hasUnsavedChanges = true) }
        checkDuplicateSlip(value)
    }

    private fun checkDuplicateSlip(slipNumber: String) {
        val trimmed = slipNumber.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val existing = repository.findRecordBySlipNumber(trimmed)
            if (existing != null && existing.id != _uiState.value.recordId) {
                _uiState.update {
                    it.copy(duplicateSlipWarning = "Notice: Slip #$trimmed was already used in record ${existing.id}.")
                }
            }
        }
    }

    fun onDhobiNameChanged(value: String) {
        _uiState.update { it.copy(dhobiName = value, hasUnsavedChanges = true) }
    }

    fun onAmountChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
            _uiState.update { it.copy(amountText = value, hasUnsavedChanges = true) }
        }
    }

    fun onExpectedReturnDateChanged(millis: Long?) {
        _uiState.update { it.copy(expectedReturnDateMillis = millis, hasUnsavedChanges = true) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value, hasUnsavedChanges = true) }
    }

    fun onQuantityChanged(categoryName: String, quantity: Int) {
        _uiState.update { state ->
            val updatedMap = state.quantities.toMutableMap()
            if (quantity <= 0) {
                updatedMap.remove(categoryName)
            } else {
                updatedMap[categoryName] = quantity
            }
            state.copy(quantities = updatedMap, hasUnsavedChanges = true)
        }
    }

    fun addCustomCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addCustomCategory(trimmed)
            val updated = repository.observeActiveCategories().first()
            _uiState.update { it.copy(categories = updated) }
        }
    }

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                val userId = repository.getRequiredUserId()
                val relativePath = imageStorageManager.saveImageFromUri(uri, userId)
                _uiState.update {
                    it.copy(
                        chitImagePath = relativePath,
                        hasUnsavedChanges = true
                    )
                }
            } catch (e: Exception) {
                _events.emit(AddLaundryEvent.ShowToast("Failed to save image: ${e.message}"))
            }
        }
    }

    fun removeChitImage() {
        val path = _uiState.value.chitImagePath
        viewModelScope.launch {
            if (!path.isNullOrBlank()) {
                imageStorageManager.deleteImage(path)
            }
            _uiState.update { it.copy(chitImagePath = null, hasUnsavedChanges = true) }
        }
    }

    fun createTempCameraUri(): Uri {
        return imageStorageManager.createTempCameraUri()
    }

    fun scanChitWithOcr() {
        val relativePath = _uiState.value.chitImagePath
        if (relativePath.isNullOrBlank()) {
            viewModelScope.launch {
                _events.emit(AddLaundryEvent.ShowToast("Please attach or capture a chit photo first"))
            }
            return
        }

        val file = imageStorageManager.getFile(relativePath)
        if (file == null || !file.exists()) {
            viewModelScope.launch {
                _events.emit(AddLaundryEvent.ShowToast("Chit image file not found on disk"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isScanningOcr = true) }
            val knownNames = _uiState.value.categories.map { it.name }
            val extracted = textRecognitionManager.recognizeChit(file, knownNames)
            _uiState.update { it.copy(isScanningOcr = false) }
            _events.emit(AddLaundryEvent.NavigateToOcrConfirmation(extracted, file))
        }
    }

    fun applyOcrResults(
        slipNumber: String?,
        dateMillis: Long?,
        confirmedQuantities: Map<String, Int>
    ) {
        _uiState.update { state ->
            val updatedQuantities = state.quantities.toMutableMap()
            confirmedQuantities.forEach { (cat, count) ->
                if (count > 0) {
                    updatedQuantities[cat] = count
                }
            }

            state.copy(
                slipNumber = if (slipNumber.isNullOrBlank()) state.slipNumber else slipNumber,
                dateGivenMillis = dateMillis ?: state.dateGivenMillis,
                quantities = updatedQuantities,
                hasUnsavedChanges = true
            )
        }
    }

    fun saveRecord() {
        val state = _uiState.value
        if (state.totalItems <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter quantity for at least one clothing item.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val userId = repository.getRequiredUserId()
                val recordEntity = LaundryRecordEntity(
                    id = state.recordId,
                    userId = userId,
                    slipNumber = state.slipNumber.trim().ifEmpty { null },
                    dateGivenMillis = state.dateGivenMillis,
                    dhobiName = state.dhobiName.trim().ifEmpty { null },
                    totalItems = state.totalItems,
                    amount = state.amountText.toDoubleOrNull(),
                    expectedReturnDateMillis = state.expectedReturnDateMillis,
                    notes = state.notes.trim().ifEmpty { null },
                    chitImagePath = state.chitImagePath
                )

                val items = state.quantities.filter { it.value > 0 }.map { (catName, qty) ->
                    LaundryItemEntity(
                        laundryRecordId = state.recordId,
                        categoryName = catName,
                        quantityGiven = qty,
                        quantityReturned = 0
                    )
                }

                if (state.isEditMode) {
                    repository.updateRecord(recordEntity, items)
                } else {
                    repository.saveNewRecord(recordEntity, items)
                }

                _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                _events.emit(AddLaundryEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Error saving record: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    class Factory(
        private val repository: LaundryRepository,
        private val imageStorageManager: ImageStorageManager,
        private val textRecognitionManager: TextRecognitionManager,
        private val existingRecordId: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddLaundryViewModel(
                repository,
                imageStorageManager,
                textRecognitionManager,
                existingRecordId
            ) as T
        }
    }
}
