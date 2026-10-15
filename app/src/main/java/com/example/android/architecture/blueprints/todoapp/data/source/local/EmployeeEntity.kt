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
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val employeeCode: String,
    val firstName: String,
    val lastName: String,
    val position: String,
    val hourlyRate: Double,
    val overtimeHourlyRate: Double,
    val dailyHoursLimit: Double = 8.0,
    val photoPath: String? = null,
    val faceEmbedding: List<Float>? = null,
    val isBiometricEnrolled: Boolean = false,
    val isActive: Boolean = true
) {
    val fullName: String
        get() = "$firstName $lastName".trim()
}
