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

package com.example.android.architecture.blueprints.todoapp.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.architecture.blueprints.todoapp.data.EmployeeRepository
import com.example.android.architecture.blueprints.todoapp.data.JsonBackupManager
import com.example.android.architecture.blueprints.todoapp.data.SecurityRepository
import com.example.android.architecture.blueprints.todoapp.data.source.local.EmployeeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmployeesUiState(
    val isLoading: Boolean = false,
    val employees: List<EmployeeEntity> = emptyList(),
    val userMessage: String? = null
)

@HiltViewModel
class EmployeesViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val jsonBackupManager: JsonBackupManager,
    val securityRepository: SecurityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeesUiState())
    val uiState: StateFlow<EmployeesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            employeeRepository.getEmployeesStream().collect { list ->
                _uiState.update { it.copy(employees = list) }
            }
        }
    }

    fun changeAdminPin(newPin: String) {
        if (newPin.isBlank() || newPin.length < 4) {
            _uiState.update { it.copy(userMessage = "El PIN debe contener al menos 4 dígitos.") }
            return
        }
        securityRepository.setAdminPin(newPin.trim())
        _uiState.update { it.copy(userMessage = "PIN de Administrador actualizado exitosamente.") }
    }

    fun exportBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (_, message) = jsonBackupManager.exportBackupToJson()
            _uiState.update { it.copy(isLoading = false, userMessage = message) }
        }
    }

    fun importBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (_, message) = jsonBackupManager.importBackupFromJson()
            _uiState.update { it.copy(isLoading = false, userMessage = message) }
        }
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun deactivateEmployee(employeeId: String) {
        viewModelScope.launch {
            employeeRepository.deactivateEmployee(employeeId)
        }
    }
}
