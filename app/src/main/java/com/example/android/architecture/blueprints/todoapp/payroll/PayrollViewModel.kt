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

package com.example.android.architecture.blueprints.todoapp.payroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.architecture.blueprints.todoapp.data.AttendanceRepository
import com.example.android.architecture.blueprints.todoapp.data.EmployeePayrollSummary
import com.example.android.architecture.blueprints.todoapp.data.EmployeeRepository
import com.example.android.architecture.blueprints.todoapp.data.PayrollCalculatorUseCase
import com.example.android.architecture.blueprints.todoapp.data.source.local.AttendanceType
import com.example.android.architecture.blueprints.todoapp.data.source.local.EmployeeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

enum class PayrollViewMode {
    TODAY,
    WEEKLY,
    BIWEEKLY
}

data class DailyEmployeeWorkStatus(
    val employee: EmployeeEntity,
    val isWorkingNow: Boolean,
    val checkInTimeFormatted: String?,
    val mealOutTimeFormatted: String?,
    val mealInTimeFormatted: String?,
    val checkOutTimeFormatted: String?,
    val elapsedTimeFormatted: String,
    val hoursWorkedToday: Double
)

data class PayrollUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val viewMode: PayrollViewMode = PayrollViewMode.TODAY,
    val dailyStatuses: List<DailyEmployeeWorkStatus> = emptyList(),
    val payrollSummaries: List<EmployeePayrollSummary> = emptyList(),
    val totalPayrollCost: Double = 0.0,
    val activeEmployeesWorkingNow: Int = 0,
    val periodLabel: String = "Estado de Asistencia Hoy"
)

@HiltViewModel
class PayrollViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val attendanceRepository: AttendanceRepository,
    private val payrollCalculator: PayrollCalculatorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayrollUiState())
    val uiState: StateFlow<PayrollUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    init {
        loadTodayWorkStatus()

        // Auto-observar reactivamente la base de datos de marcajes para actualizar en tiempo real
        viewModelScope.launch {
            attendanceRepository.getAttendanceBetween(0L, Long.MAX_VALUE).collect {
                refreshCurrentView()
            }
        }
    }

    fun refreshCurrentView() {
        when (_uiState.value.viewMode) {
            PayrollViewMode.TODAY -> loadTodayWorkStatus()
            PayrollViewMode.WEEKLY -> loadWeeklyPayroll()
            PayrollViewMode.BIWEEKLY -> loadBiweeklyPayroll()
        }
    }

    fun clearAllAttendanceRecords() {
        viewModelScope.launch {
            attendanceRepository.clearAllAttendanceRecords()
            refreshCurrentView()
        }
    }

    fun loadTodayWorkStatus() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isRefreshing = true,
                    viewMode = PayrollViewMode.TODAY,
                    periodLabel = "Asistencia y Horas Trabajadas Hoy"
                )
            }

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val now = System.currentTimeMillis()

            val employees = getAllEmployees()
            val statuses = mutableListOf<DailyEmployeeWorkStatus>()
            var workingCount = 0

            for (emp in employees) {
                val recordsToday = attendanceRepository.getAttendanceBetweenForEmployee(emp.id, startOfDay, now)
                    .sortedBy { it.timestamp }

                val checkInRecord = recordsToday.find { it.type == AttendanceType.CHECK_IN }
                val mealOutRecord = recordsToday.find { it.type == AttendanceType.MEAL_OUT }
                val mealInRecord = recordsToday.find { it.type == AttendanceType.MEAL_IN }
                val checkOutRecord = recordsToday.find { it.type == AttendanceType.CHECK_OUT }

                val checkInFormatted = checkInRecord?.let { timeFormat.format(it.timestamp) }
                val mealOutFormatted = mealOutRecord?.let { timeFormat.format(it.timestamp) }
                val mealInFormatted = mealInRecord?.let { timeFormat.format(it.timestamp) }
                val checkOutFormatted = checkOutRecord?.let { timeFormat.format(it.timestamp) }

                val lastRecord = recordsToday.lastOrNull()
                var isWorkingNow = false
                var elapsedTimeStr = "Sin entrada hoy"
                var hoursToday = 0.0

                if (lastRecord != null) {
                    when (lastRecord.type) {
                        AttendanceType.CHECK_IN -> {
                            isWorkingNow = true
                            workingCount++
                            val diffMs = now - lastRecord.timestamp
                            val hrs = diffMs / (1000 * 60 * 60)
                            val mins = (diffMs / (1000 * 60)) % 60
                            elapsedTimeStr = "Laborando (${hrs}h ${mins}m en este turno)"
                            hoursToday = diffMs / (1000.0 * 60.0 * 60.0)
                        }
                        AttendanceType.MEAL_OUT -> {
                            isWorkingNow = false
                            elapsedTimeStr = "En horario de comida 🍲"
                        }
                        AttendanceType.MEAL_IN -> {
                            isWorkingNow = true
                            workingCount++
                            val diffMs = now - lastRecord.timestamp
                            val hrs = diffMs / (1000 * 60 * 60)
                            val mins = (diffMs / (1000 * 60)) % 60
                            elapsedTimeStr = "Laborando tras comida (${hrs}h ${mins}m del 2do turno)"
                        }
                        AttendanceType.CHECK_OUT -> {
                            isWorkingNow = false
                            elapsedTimeStr = "Jornada laboral completada"
                        }
                    }
                }

                statuses.add(
                    DailyEmployeeWorkStatus(
                        employee = emp,
                        isWorkingNow = isWorkingNow,
                        checkInTimeFormatted = checkInFormatted,
                        mealOutTimeFormatted = mealOutFormatted,
                        mealInTimeFormatted = mealInFormatted,
                        checkOutTimeFormatted = checkOutFormatted,
                        elapsedTimeFormatted = elapsedTimeStr,
                        hoursWorkedToday = hoursToday
                    )
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    dailyStatuses = statuses,
                    activeEmployeesWorkingNow = workingCount
                )
            }
        }
    }

    fun loadWeeklyPayroll() {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis

        calculatePayrollForPeriod(startTime, endTime, "Últimos 7 días", PayrollViewMode.WEEKLY)
    }

    fun loadBiweeklyPayroll() {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -15)
        val startTime = calendar.timeInMillis

        calculatePayrollForPeriod(startTime, endTime, "Últimos 15 días (Quincena)", PayrollViewMode.BIWEEKLY)
    }

    private fun calculatePayrollForPeriod(startTime: Long, endTime: Long, label: String, mode: PayrollViewMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isRefreshing = true, viewMode = mode, periodLabel = label) }

            val employees = getAllEmployees()
            val summaries = mutableListOf<EmployeePayrollSummary>()
            var grandTotal = 0.0

            for (emp in employees) {
                val records = attendanceRepository.getAttendanceBetweenForEmployee(emp.id, startTime, endTime)
                val summary = payrollCalculator.calculatePayrollForEmployee(emp, records, startTime, endTime)
                summaries.add(summary)
                grandTotal += summary.totalGrossPay
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    payrollSummaries = summaries,
                    totalPayrollCost = grandTotal
                )
            }
        }
    }

    private suspend fun getAllEmployees(): List<EmployeeEntity> {
        val enrolled = employeeRepository.getEmployeesWithBiometrics()
        if (enrolled.isNotEmpty()) return enrolled

        val list = mutableListOf<EmployeeEntity>()
        employeeRepository.getEmployeesStream().collect { list.addAll(it) }
        return list
    }
}
