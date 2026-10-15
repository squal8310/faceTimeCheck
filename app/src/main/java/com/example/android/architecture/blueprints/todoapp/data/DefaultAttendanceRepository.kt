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

import com.example.android.architecture.blueprints.todoapp.data.source.local.AttendanceDao
import com.example.android.architecture.blueprints.todoapp.data.source.local.AttendanceRecordEntity
import com.example.android.architecture.blueprints.todoapp.data.source.local.AttendanceType
import com.example.android.architecture.blueprints.todoapp.data.source.local.VerificationMethod
import com.example.android.architecture.blueprints.todoapp.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAttendanceRepository @Inject constructor(
    private val attendanceDao: AttendanceDao,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : AttendanceRepository {

    override fun getAttendanceForEmployee(employeeId: String): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.observeByEmployeeId(employeeId)
    }

    override fun getAttendanceBetween(startTime: Long, endTime: Long): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.observeBetweenTimestamps(startTime, endTime)
    }

    override suspend fun getAttendanceBetweenForEmployee(
        employeeId: String,
        startTime: Long,
        endTime: Long
    ): List<AttendanceRecordEntity> {
        return withContext(dispatcher) {
            attendanceDao.getByEmployeeBetween(employeeId, startTime, endTime)
        }
    }

    override suspend fun recordAttendance(
        employeeId: String,
        type: AttendanceType,
        verificationMethod: VerificationMethod,
        photoPath: String?,
        notes: String?
    ): AttendanceRecordEntity {
        return withContext(dispatcher) {
            val record = AttendanceRecordEntity(
                id = UUID.randomUUID().toString(),
                employeeId = employeeId,
                timestamp = System.currentTimeMillis(),
                type = type,
                verificationMethod = verificationMethod,
                capturedPhotoPath = photoPath,
                notes = notes
            )
            attendanceDao.upsert(record)
            record
        }
    }

    override suspend fun getLastRecordForEmployee(employeeId: String): AttendanceRecordEntity? {
        return withContext(dispatcher) {
            attendanceDao.getLastRecordForEmployee(employeeId)
        }
    }
}
