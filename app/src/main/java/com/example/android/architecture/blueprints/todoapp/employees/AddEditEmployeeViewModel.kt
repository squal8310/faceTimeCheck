/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.employees

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.architecture.blueprints.todoapp.biometrics.FaceDetectorHelper
import com.example.android.architecture.blueprints.todoapp.biometrics.TensorFlowFaceEmbedder
import com.example.android.architecture.blueprints.todoapp.data.EmployeeRepository
import com.example.android.architecture.blueprints.todoapp.data.source.local.EmployeeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class AddEditEmployeeUiState(
    val employeeCode: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val position: String = "",
    val hourlyRate: String = "100.0",
    val overtimeHourlyRate: String = "150.0",
    val dailyHoursLimit: String = "8.0",
    val capturedBitmap: Bitmap? = null,
    val faceEmbedding: List<Float>? = null,
    val isBiometricEnrolled: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditEmployeeViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    val faceEmbedder: TensorFlowFaceEmbedder,
    val faceDetectorHelper: FaceDetectorHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val employeeId: String? = savedStateHandle["employeeId"]

    private val _uiState = MutableStateFlow(AddEditEmployeeUiState())
    val uiState: StateFlow<AddEditEmployeeUiState> = _uiState.asStateFlow()

    init {
        if (employeeId != null) {
            viewModelScope.launch {
                val emp = employeeRepository.getEmployee(employeeId)
                if (emp != null) {
                    _uiState.update {
                        it.copy(
                            employeeCode = emp.employeeCode,
                            firstName = emp.firstName,
                            lastName = emp.lastName,
                            position = emp.position,
                            hourlyRate = emp.hourlyRate.toString(),
                            overtimeHourlyRate = emp.overtimeHourlyRate.toString(),
                            dailyHoursLimit = emp.dailyHoursLimit.toString(),
                            faceEmbedding = emp.faceEmbedding,
                            isBiometricEnrolled = emp.isBiometricEnrolled
                        )
                    }
                }
            }
        }
    }

    fun onEmployeeCodeChanged(value: String) { _uiState.update { it.copy(employeeCode = value) } }
    fun onFirstNameChanged(value: String) { _uiState.update { it.copy(firstName = value) } }
    fun onLastNameChanged(value: String) { _uiState.update { it.copy(lastName = value) } }
    fun onPositionChanged(value: String) { _uiState.update { it.copy(position = value) } }
    fun onHourlyRateChanged(value: String) { _uiState.update { it.copy(hourlyRate = value) } }
    fun onOvertimeRateChanged(value: String) { _uiState.update { it.copy(overtimeHourlyRate = value) } }
    fun onDailyHoursLimitChanged(value: String) { _uiState.update { it.copy(dailyHoursLimit = value) } }

    fun processFacePhoto(faceBitmap: Bitmap) {
        viewModelScope.launch {
            Timber.tag("FaceBiometrics").d("=== CAPTURANDO Y PROCESANDO NUEVO ROSTRO DE ENROLAMIENTO ===")
            Timber.tag("FaceBiometrics").d("Dimensiones foto original: %dx%d px", faceBitmap.width, faceBitmap.height)
            val embeddingArray = faceEmbedder.getEmbedding(faceBitmap)
            Timber.tag("FaceBiometrics").i("Firma biométrica creada exitosamente (%d flotantes). Vector listo para guardar.", embeddingArray.size)
            _uiState.update {
                it.copy(
                    capturedBitmap = faceBitmap,
                    faceEmbedding = embeddingArray.toList(),
                    isBiometricEnrolled = true
                )
            }
        }
    }

    fun saveCompositeFaceEmbedding(embeddingList: List<Float>, previewBitmap: Bitmap) {
        Timber.tag("FaceBiometrics").i("=== ENROLAMIENTO 3D MULTI-ÁNGULO COMPLETADO ===")
        Timber.tag("FaceBiometrics").i("Vector compuesto generado con %d valores flotantes.", embeddingList.size)
        _uiState.update {
            it.copy(
                capturedBitmap = previewBitmap,
                faceEmbedding = embeddingList,
                isBiometricEnrolled = true
            )
        }
    }

    fun saveEmployee() {
        val state = _uiState.value
        if (state.employeeCode.isBlank() || state.firstName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Código y Nombre son obligatorios") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val emp = EmployeeEntity(
                id = employeeId ?: UUID.randomUUID().toString(),
                employeeCode = state.employeeCode.trim(),
                firstName = state.firstName.trim(),
                lastName = state.lastName.trim(),
                position = state.position.trim(),
                hourlyRate = state.hourlyRate.toDoubleOrNull() ?: 100.0,
                overtimeHourlyRate = state.overtimeHourlyRate.toDoubleOrNull() ?: 150.0,
                dailyHoursLimit = state.dailyHoursLimit.toDoubleOrNull() ?: 8.0,
                faceEmbedding = state.faceEmbedding,
                isBiometricEnrolled = state.isBiometricEnrolled
            )
            Timber.tag("FaceBiometrics").i("GUARDANDO EMPLEADO EN BASE DE DATOS: '%s' | Código: %s | Enrolado Biométrico: %b | Tamaño Embedding: %d",
                emp.fullName, emp.employeeCode, emp.isBiometricEnrolled, emp.faceEmbedding?.size ?: 0)
            employeeRepository.saveEmployee(emp)
            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}
