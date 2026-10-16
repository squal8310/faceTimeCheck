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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android.architecture.blueprints.todoapp.data.EmployeePayrollSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PayrollViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Borrar Historial de Registros") },
            text = { Text("¿Estás seguro de borrar todos los marcajes de asistencia acumulados? Esta acción es solo para pruebas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllAttendanceRecords()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Borrar Registros", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cálculo de Nómina y Asistencia") },
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Borrar Registros (Pruebas)",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Fila de Filtros (Hoy en Vivo / Semanal / Quincenal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = viewModel::loadTodayWorkStatus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.viewMode == PayrollViewMode.TODAY)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("⏱️ Hoy")
                }

                Button(
                    onClick = viewModel::loadWeeklyPayroll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.viewMode == PayrollViewMode.WEEKLY)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("📅 Semanal")
                }

                Button(
                    onClick = viewModel::loadBiweeklyPayroll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.viewMode == PayrollViewMode.BIWEEKLY)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("🗓️ Quincenal")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.viewMode == PayrollViewMode.TODAY) {
                // Vista de Asistencia e Registro de Horas Hoy
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = uiState.periodLabel,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Empleados Laborando Ahora: ${uiState.activeEmployeesWorkingNow}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón de acceso directo para limpiar historial de registros durante pruebas
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(" 🗑️ Borrar Historial de Marcajes (Pruebas)")
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.dailyStatuses.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay empleados registrados.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.dailyStatuses) { status ->
                            DailyWorkStatusCard(status = status)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            } else {
                // Vista de Nómina Semanal / Quincenal
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Periodo: ${uiState.periodLabel}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Costo Total de Nómina: $${String.format("%.2f", uiState.totalPayrollCost)}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(" 🗑️ Borrar Historial de Marcajes (Pruebas)")
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.payrollSummaries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay registros de marcajes para este periodo.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.payrollSummaries) { summary ->
                            PayrollItemCard(summary = summary)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyWorkStatusCard(status: DailyEmployeeWorkStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.employee.fullName,
                    style = MaterialTheme.typography.titleMedium
                )
                if (status.isWorkingNow) {
                    Text(
                        text = "🟢 TRABAJANDO",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF2E7D32)
                    )
                } else if (status.checkOutTimeFormatted != null) {
                    Text(
                        text = "🔴 SALIDA REGISTRADA",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "⚪ SIN ENTRADA HOY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Puesto: ${status.employee.position} | Código: ${status.employee.employeeCode}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (status.checkInTimeFormatted != null) {
                Text(
                    text = "Hora de Entrada hoy: ${status.checkInTimeFormatted}" +
                            if (status.checkOutTimeFormatted != null) " | Salida: ${status.checkOutTimeFormatted}" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Estatus hoy: ${status.elapsedTimeFormatted}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (status.isWorkingNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PayrollItemCard(summary: EmployeePayrollSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = summary.employee.fullName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Puesto: ${summary.employee.position} | Código: ${summary.employee.employeeCode}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Horas Norm.: ${String.format("%.1f", summary.regularHoursWorked)}h ($${String.format("%.2f", summary.regularPay)})",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Extras: ${String.format("%.1f", summary.overtimeHoursWorked)}h ($${String.format("%.2f", summary.overtimePay)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pago Total: $${String.format("%.2f", summary.totalGrossPay)}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
