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

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    @Query("SELECT * FROM employees WHERE isActive = 1 ORDER BY lastName, firstName ASC")
    fun observeAllActive(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees ORDER BY lastName, firstName ASC")
    fun observeAll(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :employeeId")
    fun observeById(employeeId: String): Flow<EmployeeEntity?>

    @Query("SELECT * FROM employees WHERE id = :employeeId")
    suspend fun getById(employeeId: String): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE employeeCode = :code AND isActive = 1 LIMIT 1")
    suspend fun getByCode(code: String): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE isBiometricEnrolled = 1 AND isActive = 1")
    suspend fun getAllWithBiometrics(): List<EmployeeEntity>

    @Upsert
    suspend fun upsert(employee: EmployeeEntity)

    @Query("UPDATE employees SET isActive = 0 WHERE id = :employeeId")
    suspend fun deactivate(employeeId: String)

    @Query("DELETE FROM employees WHERE id = :employeeId")
    suspend fun deleteById(employeeId: String): Int
}
