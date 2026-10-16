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

package com.example.android.architecture.blueprints.todoapp.data.source.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class AttendanceType {
    CHECK_IN,   // 1. Entrada a laborar
    MEAL_OUT,   // 2. Salida a comer
    MEAL_IN,    // 3. Regreso de comer
    CHECK_OUT   // 4. Salida definitiva
}

enum class VerificationMethod {
    FINGERPRINT,
    CAMERA,
    MANUAL
}

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["employeeId"])]
)
data class AttendanceRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val employeeId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: AttendanceType,
    val verificationMethod: VerificationMethod,
    val capturedPhotoPath: String? = null,
    val notes: String? = null
)
