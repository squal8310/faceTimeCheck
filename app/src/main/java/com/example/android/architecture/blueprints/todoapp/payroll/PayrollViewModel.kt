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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class PayrollUiState(
    val isLoading: Boolean = false,
    val payrollSummaries: List<EmployeePayrollSummary> = emptyList(),
    val totalPayrollCost: Double = 0.0,
    val periodLabel: String = "Esta Semana"
)

@HiltViewModel
class PayrollViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val attendanceRepository: AttendanceRepository,
    private val payrollCalculator: PayrollCalculatorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayrollUiState())
    val uiState: StateFlow<PayrollUiState> = _uiState.asStateFlow()

    init {
        loadWeeklyPayroll()
    }

    fun loadWeeklyPayroll() {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis

        calculatePayrollForPeriod(startTime, endTime, "Últimos 7 días")
    }

    fun loadBiweeklyPayroll() {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -15)
        val startTime = calendar.timeInMillis

        calculatePayrollForPeriod(startTime, endTime, "Últimos 15 días (Quincena)")
    }

    private fun calculatePayrollForPeriod(startTime: Long, endTime: Long, label: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, periodLabel = label) }

            val employees = employeeRepository.getEmployeesWithBiometrics().ifEmpty {
                val list = mutableListOf<com.example.android.architecture.blueprints.todoapp.data.source.local.EmployeeEntity>()
                employeeRepository.getEmployeesStream().collect { list.addAll(it) }
                list
            }

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
                    payrollSummaries = summaries,
                    totalPayrollCost = grandTotal
                )
            }
        }
    }
}
