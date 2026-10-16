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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class AttendanceClockUiState(
    val selectedEmployee: EmployeeEntity? = null,
    val employees: List<EmployeeEntity> = emptyList(),
    val attendanceType: AttendanceType = AttendanceType.CHECK_IN,
    val isFaceDetected: Boolean = false,
    val statusMessage: String = "Colócate dentro del óvalo para marcar automáticamente tu registro",
    val isSuccess: Boolean = false,
    val isAlreadyRegisteredToday: Boolean = false,
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

    private var lastAutoScanTime = 0L
    private val SCAN_COOLDOWN_MS = 3000L
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

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
                    "Colócate dentro del óvalo para marcar automáticamente tu registro"
            )
        }
    }

    fun setAttendanceType(type: AttendanceType) {
        _uiState.update {
            it.copy(
                attendanceType = type,
                isSuccess = false,
                isAlreadyRegisteredToday = false,
                statusMessage = "Colócate dentro del óvalo para marcar automáticamente tu registro"
            )
        }
    }

    fun onLiveFaceDetected(faceBitmap: Bitmap) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAutoScanTime < SCAN_COOLDOWN_MS) return
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Identificando rostro...") }

            val currentEmbedding = faceEmbedder.getEmbedding(faceBitmap)
            val enrolledEmployees = employeeRepository.getEmployeesWithBiometrics()

            var matchedEmployee: EmployeeEntity? = _uiState.value.selectedEmployee
            var matchPercentage = 100

            if (matchedEmployee == null) {
                val bestMatchResult = faceMatcher.findBestMatch(currentEmbedding, enrolledEmployees)
                if (bestMatchResult != null) {
                    matchedEmployee = bestMatchResult.employee
                    matchPercentage = (bestMatchResult.similarityScore * 100).toInt().coerceIn(0, 100)
                }
            }

            if (matchedEmployee != null) {
                lastAutoScanTime = System.currentTimeMillis()

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = calendar.timeInMillis

                val recordsToday = attendanceRepository.getAttendanceBetweenForEmployee(
                    matchedEmployee.id,
                    startOfDay,
                    currentTime
                ).sortedBy { it.timestamp }

                val targetType = _uiState.value.attendanceType
                val existingTodayRecord = recordsToday.find { it.type == targetType }

                if (existingTodayRecord != null) {
                    val formattedTime = timeFormat.format(existingTodayRecord.timestamp)
                    val actionName = getActionLabel(targetType)
                    Timber.tag("FaceBiometrics").i("REGISTRO EXISTENTE: El empleado '%s' ya registró %s hoy a las %s.", matchedEmployee.fullName, actionName, formattedTime)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            isAlreadyRegisteredToday = true,
                            statusMessage = "El día de hoy ya se registró $actionName a las $formattedTime: ${matchedEmployee.fullName}"
                        )
                    }
                } else {
                    val actionName = getActionLabel(targetType)
                    Timber.tag("FaceBiometrics").i("NUEVO REGISTRO: Guardando %s para '%s' (%d%% coincidencia).", actionName, matchedEmployee.fullName, matchPercentage)
                    attendanceRepository.recordAttendance(
                        employeeId = matchedEmployee.id,
                        type = targetType,
                        verificationMethod = VerificationMethod.CAMERA
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            isAlreadyRegisteredToday = false,
                            statusMessage = "¡$actionName Registrada Exitosamente para ${matchedEmployee.fullName}!"
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        isAlreadyRegisteredToday = false,
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

        viewModelScope.launch {
            val targetType = _uiState.value.attendanceType
            attendanceRepository.recordAttendance(
                employeeId = employee.id,
                type = targetType,
                verificationMethod = VerificationMethod.MANUAL
            )
            val actionName = getActionLabel(targetType)
            _uiState.update {
                it.copy(
                    isSuccess = true,
                    isAlreadyRegisteredToday = false,
                    statusMessage = "¡$actionName Registrada Exitosamente para ${employee.fullName}!"
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

        biometricAuthManager.promptBiometric(
            activity = activity,
            onSuccess = {
                viewModelScope.launch {
                    val targetType = _uiState.value.attendanceType
                    attendanceRepository.recordAttendance(
                        employeeId = employee.id,
                        type = targetType,
                        verificationMethod = VerificationMethod.FINGERPRINT
                    )
                    val actionName = getActionLabel(targetType)
                    _uiState.update {
                        it.copy(
                            isSuccess = true,
                            isAlreadyRegisteredToday = false,
                            statusMessage = "¡$actionName por Huella Registrada para ${employee.fullName}!"
                        )
                    }
                }
            },
            onError = { err ->
                _uiState.update { it.copy(statusMessage = "Error de huella: $err") }
            }
        )
    }

    private fun getActionLabel(type: AttendanceType): String {
        return when (type) {
            AttendanceType.CHECK_IN -> "Entrada a Laborar"
            AttendanceType.MEAL_OUT -> "Salida a Comer"
            AttendanceType.MEAL_IN -> "Regreso de Comer"
            AttendanceType.CHECK_OUT -> "Salida Definitiva"
        }
    }
}
