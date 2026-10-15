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
import javax.inject.Inject

data class AttendanceClockUiState(
    val selectedEmployee: EmployeeEntity? = null,
    val employees: List<EmployeeEntity> = emptyList(),
    val attendanceType: AttendanceType = AttendanceType.CHECK_IN,
    val isFaceDetected: Boolean = false,
    val statusMessage: String = "Selecciona un empleado o escanea tu rostro / huella",
    val isSuccess: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class AttendanceClockViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val attendanceRepository: AttendanceRepository,
    private val faceEmbedder: TensorFlowFaceEmbedder,
    private val faceMatcher: FaceMatcher,
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

    fun selectEmployee(employee: EmployeeEntity) {
        _uiState.update {
            it.copy(
                selectedEmployee = employee,
                statusMessage = "Empleado seleccionado: ${employee.fullName}"
            )
        }
    }

    fun setAttendanceType(type: AttendanceType) {
        _uiState.update { it.copy(attendanceType = type) }
    }

    fun onFaceCaptured(faceBitmap: Bitmap) {
        val state = _uiState.value
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentEmbedding = faceEmbedder.getEmbedding(faceBitmap)

            val enrolledEmployees = employeeRepository.getEmployeesWithBiometrics()
            var matchedEmployee: EmployeeEntity? = state.selectedEmployee

            if (matchedEmployee == null) {
                for (emp in enrolledEmployees) {
                    val savedEmbed = emp.faceEmbedding?.toFloatArray() ?: continue
                    if (faceMatcher.isMatch(savedEmbed, currentEmbedding)) {
                        matchedEmployee = emp
                        break
                    }
                }
            }

            if (matchedEmployee != null) {
                attendanceRepository.recordAttendance(
                    employeeId = matchedEmployee.id,
                    type = state.attendanceType,
                    verificationMethod = VerificationMethod.CAMERA
                )
                val typeLabel = if (state.attendanceType == AttendanceType.CHECK_IN) "ENTRADA" else "SALIDA"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        statusMessage = "¡$typeLabel Registrada Exitosamente para ${matchedEmployee.fullName}!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        statusMessage = "Rostro no reconocido. Selecciona tu usuario o reintenta."
                    )
                }
            }
        }
    }

    fun authenticateWithFingerprint(activity: FragmentActivity) {
        val employee = _uiState.value.selectedEmployee
        if (employee == null) {
            _uiState.update { it.copy(statusMessage = "Por favor selecciona un empleado primero para huella.") }
            return
        }

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
