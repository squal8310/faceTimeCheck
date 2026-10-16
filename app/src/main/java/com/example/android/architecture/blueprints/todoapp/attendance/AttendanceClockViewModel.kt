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

package com.example.android.architecture.blueprints.todoapp.attendance

import android.graphics.Bitmap
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.architecture.blueprints.todoapp.biometrics.BiometricAuthManager
import com.example.android.architecture.blueprints.todoapp.biometrics.FaceDetectorHelper
import com.example.android.architecture.blueprints.todoapp.biometrics.FaceMatcher
import com.example.android.architecture.blueprints.todoapp.biometrics.TensorFlowFaceEmbedder
import com.example.android.architecture.blueprints.todoapp.data.AttendanceRepository
import com.example.android.architecture.blueprints.todoapp.data.EmployeeRepository
import com.example.android.architecture.blueprints.todoapp.data.source.local.AttendanceType
import com.example.android.architecture.blueprints.todoapp.data.source.local.EmployeeEntity
import com.example.android.architecture.blueprints.todoapp.data.source.local.VerificationMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AttendanceClockUiState(
    val selectedEmployee: EmployeeEntity? = null,
    val employees: List<EmployeeEntity> = emptyList(),
    val attendanceType: AttendanceType = AttendanceType.CHECK_IN,
    val isFaceDetected: Boolean = false,
    val statusMessage: String = "Colócate dentro del óvalo para marcar automáticamente tu entrada / salida",
    val isSuccess: Boolean = false,
    val isLocked: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class AttendanceClockViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val attendanceRepository: AttendanceRepository,
    private val faceEmbedder: TensorFlowFaceEmbedder,
    private val faceMatcher: FaceMatcher,
    val faceDetectorHelper: FaceDetectorHelper,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceClockUiState())
    val uiState: StateFlow<AttendanceClockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            employeeRepository.getEmployeesStream().collect { list ->
                _uiState.update { it.copy(employees = list) }
            }
        }
    }

    fun selectEmployee(employee: EmployeeEntity?) {
        _uiState.update {
            it.copy(
                selectedEmployee = employee,
                statusMessage = if (employee != null)
                    "Empleado seleccionado: ${employee.fullName}"
                else
                    "Colócate dentro del óvalo para marcar automáticamente tu entrada / salida"
            )
        }
    }

    fun setAttendanceType(type: AttendanceType) {
        _uiState.update {
            it.copy(
                attendanceType = type,
                isSuccess = false,
                isLocked = false,
                statusMessage = "Colócate dentro del óvalo para marcar automáticamente tu entrada / salida"
            )
        }
    }

    fun resetClockState() {
        _uiState.update {
            it.copy(
                isSuccess = false,
                isLocked = false,
                selectedEmployee = null,
                statusMessage = "Colócate dentro del óvalo para marcar automáticamente tu entrada / salida"
            )
        }
    }

    fun onLiveFaceDetected(faceBitmap: Bitmap) {
        val currentState = _uiState.value
        if (currentState.isLocked || currentState.isLoading) return

        viewModelScope.launch {
            Timber.tag("FaceBiometrics").d("=== NUEVO FRAME DETECTADO EN CÁMARA (Dimensiones: %dx%d px) ===", faceBitmap.width, faceBitmap.height)
            _uiState.update { it.copy(isLoading = true, statusMessage = "Identificando rostro...") }

            val currentEmbedding = faceEmbedder.getEmbedding(faceBitmap)
            val enrolledEmployees = employeeRepository.getEmployeesWithBiometrics()
            Timber.tag("FaceBiometrics").d("Empleados totales con biometría guardada en DB: %d", enrolledEmployees.size)

            var matchedEmployee: EmployeeEntity? = currentState.selectedEmployee
            var matchPercentage = 100

            if (matchedEmployee == null) {
                val bestMatchResult = faceMatcher.findBestMatch(currentEmbedding, enrolledEmployees)
                if (bestMatchResult != null) {
                    matchedEmployee = bestMatchResult.employee
                    matchPercentage = (bestMatchResult.similarityScore * 100).toInt().coerceIn(0, 100)
                }
            }

            if (matchedEmployee != null) {
                Timber.tag("FaceBiometrics").i(">>> REGISTRO COMPLETADO: Empleado '%s' (%d%% coincidencia)", matchedEmployee.fullName, matchPercentage)
                attendanceRepository.recordAttendance(
                    employeeId = matchedEmployee.id,
                    type = _uiState.value.attendanceType,
                    verificationMethod = VerificationMethod.CAMERA
                )
                val typeLabel = if (_uiState.value.attendanceType == AttendanceType.CHECK_IN) "ENTRADA" else "SALIDA"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        isLocked = true,
                        statusMessage = "¡$typeLabel Registrada para ${matchedEmployee.fullName} (Coincidencia $matchPercentage%)!"
                    )
                }
            } else {
                Timber.tag("FaceBiometrics").w(">>> NINGÚN EMPLEADO COINCIDE con el rostro detectado.")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        statusMessage = "Rostro detectado sin coincidencia suficiente. Reintenta o selecciona tu usuario."
                    )
                }
            }
        }
    }

    fun recordManualAttendance() {
        val employee = _uiState.value.selectedEmployee
        if (employee == null) {
            _uiState.update { it.copy(statusMessage = "Por favor selecciona un empleado de la lista primero.") }
            return
        }

        if (_uiState.value.isLocked) return

        viewModelScope.launch {
            attendanceRepository.recordAttendance(
                employeeId = employee.id,
                type = _uiState.value.attendanceType,
                verificationMethod = VerificationMethod.MANUAL
            )
            val typeLabel = if (_uiState.value.attendanceType == AttendanceType.CHECK_IN) "ENTRADA" else "SALIDA"
            _uiState.update {
                it.copy(
                    isSuccess = true,
                    isLocked = true,
                    statusMessage = "¡$typeLabel Registrada Exitosamente para ${employee.fullName}!"
                )
            }
        }
    }

    fun authenticateWithFingerprint(activity: FragmentActivity) {
        val employee = _uiState.value.selectedEmployee
        if (employee == null) {
            _uiState.update { it.copy(statusMessage = "Por favor selecciona un empleado primero para validar la huella.") }
            return
        }

        if (_uiState.value.isLocked) return

        biometricAuthManager.promptBiometric(
            activity = activity,
            onSuccess = {
                viewModelScope.launch {
                    attendanceRepository.recordAttendance(
                        employeeId = employee.id,
                        type = _uiState.value.attendanceType,
                        verificationMethod = VerificationMethod.FINGERPRINT
                    )
                    val typeLabel = if (_uiState.value.attendanceType == AttendanceType.CHECK_IN) "ENTRADA" else "SALIDA"
                    _uiState.update {
                        it.copy(
                            isSuccess = true,
                            isLocked = true,
                            statusMessage = "¡$typeLabel por Huella Registrada para ${employee.fullName}!"
                        )
                    }
                }
            },
            onError = { err ->
                _uiState.update { it.copy(statusMessage = "Error de huella: $err") }
            }
        )
    }
}
