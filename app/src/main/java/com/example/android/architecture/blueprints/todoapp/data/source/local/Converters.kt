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

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromFloatList(list: List<Float>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    fun toFloatList(data: String?): List<Float>? {
        if (data.isNullOrEmpty()) return null
        return try {
            data.split(",").map { it.trim().toFloat() }
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromAttendanceType(type: AttendanceType): String = type.name

    @TypeConverter
    fun toAttendanceType(value: String): AttendanceType = try {
        AttendanceType.valueOf(value)
    } catch (e: Exception) {
        AttendanceType.CHECK_IN
    }

    @TypeConverter
    fun fromVerificationMethod(method: VerificationMethod): String = method.name

    @TypeConverter
    fun toVerificationMethod(value: String): VerificationMethod = try {
        VerificationMethod.valueOf(value)
    } catch (e: Exception) {
        VerificationMethod.MANUAL
    }
}
