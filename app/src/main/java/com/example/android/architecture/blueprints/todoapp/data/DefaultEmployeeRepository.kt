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

import com.example.android.architecture.blueprints.todoapp.data.source.local.EmployeeDao
import com.example.android.architecture.blueprints.todoapp.data.source.local.EmployeeEntity
import com.example.android.architecture.blueprints.todoapp.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultEmployeeRepository @Inject constructor(
    private val employeeDao: EmployeeDao,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : EmployeeRepository {

    override fun getEmployeesStream(): Flow<List<EmployeeEntity>> {
        return employeeDao.observeAllActive()
    }

    override fun getEmployeeStream(employeeId: String): Flow<EmployeeEntity?> {
        return employeeDao.observeById(employeeId)
    }

    override suspend fun getEmployee(employeeId: String): EmployeeEntity? {
        return withContext(dispatcher) {
            employeeDao.getById(employeeId)
        }
    }

    override suspend fun getEmployeeByCode(code: String): EmployeeEntity? {
        return withContext(dispatcher) {
            employeeDao.getByCode(code)
        }
    }

    override suspend fun getEmployeesWithBiometrics(): List<EmployeeEntity> {
        return withContext(dispatcher) {
            employeeDao.getAllWithBiometrics()
        }
    }

    override suspend fun saveEmployee(employee: EmployeeEntity) {
        withContext(dispatcher) {
            employeeDao.upsert(employee)
        }
    }

    override suspend fun saveBiometricFaceTemplate(
        employeeId: String,
        embedding: List<Float>,
        photoPath: String?
    ) {
        withContext(dispatcher) {
            val employee = employeeDao.getById(employeeId) ?: return@withContext
            val updated = employee.copy(
                faceEmbedding = embedding,
                photoPath = photoPath ?: employee.photoPath,
                isBiometricEnrolled = true
            )
            employeeDao.upsert(updated)
        }
    }

    override suspend fun deactivateEmployee(employeeId: String) {
        withContext(dispatcher) {
            employeeDao.deactivate(employeeId)
        }
    }

    override suspend fun deleteEmployee(employeeId: String) {
        withContext(dispatcher) {
            employeeDao.deleteById(employeeId)
        }
    }
}
