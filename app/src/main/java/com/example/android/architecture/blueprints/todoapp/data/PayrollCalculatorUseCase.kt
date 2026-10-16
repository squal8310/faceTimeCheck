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

package com.example.android.architecture.blueprints.todoapp.data

import com.example.android.architecture.blueprints.todoapp.data.source.local.AttendanceRecordEntity
import com.example.android.architecture.blueprints.todoapp.data.source.local.AttendanceType
import com.example.android.architecture.blueprints.todoapp.data.source.local.EmployeeEntity
import javax.inject.Inject
import javax.inject.Singleton

data class EmployeePayrollSummary(
    val employee: EmployeeEntity,
    val periodStart: Long,
    val periodEnd: Long,
    val regularHoursWorked: Double,
    val overtimeHoursWorked: Double,
    val totalHoursWorked: Double,
    val regularPay: Double,
    val overtimePay: Double,
    val totalGrossPay: Double
)

@Singleton
class PayrollCalculatorUseCase @Inject constructor() {

    fun calculatePayrollForEmployee(
        employee: EmployeeEntity,
        records: List<AttendanceRecordEntity>,
        startTime: Long,
        endTime: Long
    ): EmployeePayrollSummary {
        val sortedRecords = records.sortedBy { it.timestamp }
        var regularHours = 0.0
        var overtimeHours = 0.0

        var pendingCheckIn: AttendanceRecordEntity? = null

        for (record in sortedRecords) {
            when (record.type) {
                AttendanceType.CHECK_IN, AttendanceType.MEAL_IN -> {
                    pendingCheckIn = record
                }
                AttendanceType.MEAL_OUT, AttendanceType.CHECK_OUT -> {
                    if (pendingCheckIn != null) {
                        val durationMillis = record.timestamp - pendingCheckIn.timestamp
                        val hoursWorked = (durationMillis / (1000.0 * 60.0 * 60.0)).coerceAtLeast(0.0)

                        val limit = employee.dailyHoursLimit
                        if (regularHours + hoursWorked <= limit) {
                            regularHours += hoursWorked
                        } else {
                            val remainingReg = (limit - regularHours).coerceAtLeast(0.0)
                            regularHours += remainingReg
                            overtimeHours += (hoursWorked - remainingReg)
                        }
                        pendingCheckIn = null
                    }
                }
            }
        }

        val regPay = regularHours * employee.hourlyRate
        val otPay = overtimeHours * employee.overtimeHourlyRate
        val totalPay = regPay + otPay

        return EmployeePayrollSummary(
            employee = employee,
            periodStart = startTime,
            periodEnd = endTime,
            regularHoursWorked = regularHours,
            overtimeHoursWorked = overtimeHours,
            totalHoursWorked = regularHours + overtimeHours,
            regularPay = regPay,
            overtimePay = otPay,
            totalGrossPay = totalPay
        )
    }
}
