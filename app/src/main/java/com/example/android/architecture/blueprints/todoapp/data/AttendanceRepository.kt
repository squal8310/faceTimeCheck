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
import com.example.android.architecture.blueprints.todoapp.data.source.local.VerificationMethod
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    fun getAttendanceForEmployee(employeeId: String): Flow<List<AttendanceRecordEntity>>
    fun getAttendanceBetween(startTime: Long, endTime: Long): Flow<List<AttendanceRecordEntity>>
    suspend fun getAttendanceBetweenForEmployee(employeeId: String, startTime: Long, endTime: Long): List<AttendanceRecordEntity>
    suspend fun recordAttendance(
        employeeId: String,
        type: AttendanceType,
        verificationMethod: VerificationMethod,
        photoPath: String? = null,
        notes: String? = null
    ): AttendanceRecordEntity
    suspend fun getLastRecordForEmployee(employeeId: String): AttendanceRecordEntity?
    suspend fun clearAllAttendanceRecords()
}
