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

package com.example.android.architecture.blueprints.todoapp.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android.architecture.blueprints.todoapp.data.source.local.AttendanceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceClockScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AttendanceClockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checador de Asistencia") },
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú")
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Filas de Modo de Registro (4 Eventos del Día)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.setAttendanceType(AttendanceType.CHECK_IN) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.attendanceType == AttendanceType.CHECK_IN)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Text("1. Entrada", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { viewModel.setAttendanceType(AttendanceType.MEAL_OUT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.attendanceType == AttendanceType.MEAL_OUT)
                                MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Text("2. A Comer", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.setAttendanceType(AttendanceType.MEAL_IN) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.attendanceType == AttendanceType.MEAL_IN)
                                MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Text("3. Regreso Comer", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { viewModel.setAttendanceType(AttendanceType.CHECK_OUT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.attendanceType == AttendanceType.CHECK_OUT)
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Text("4. Salida Día", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cámara en Vivo Continuamente Abierta en un Óvalo
            LiveOvalCameraPreview(
                faceDetectorHelper = viewModel.faceDetectorHelper,
                isSuccess = uiState.isSuccess,
                isAlreadyRegisteredToday = uiState.isAlreadyRegisteredToday,
                onFaceDetected = { faceBitmap ->
                    viewModel.onLiveFaceDetected(faceBitmap)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de Estado
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        uiState.isSuccess -> MaterialTheme.colorScheme.primaryContainer
                        uiState.isAlreadyRegisteredToday -> Color(0xFFFFECB3)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = uiState.statusMessage,
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            uiState.isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
                            uiState.isAlreadyRegisteredToday -> Color(0xFFBF360C)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Opciones Alternativas / Huella / Manual
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { isDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.selectedEmployee?.fullName ?: "Selección Manual de Empleado (Opcional)",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    DropdownMenuItem(
                        text = { Text("Auto-Reconocimiento por Cámara") },
                        onClick = {
                            viewModel.selectEmployee(null)
                            isDropdownExpanded = false
                        }
                    )
                    uiState.employees.forEach { employee ->
                        DropdownMenuItem(
                            text = { Text("${employee.fullName} (${employee.employeeCode})") },
                            onClick = {
                                viewModel.selectEmployee(employee)
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        viewModel.authenticateWithFingerprint(activity)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Text(" 👆 Marcar con Huella Dactilar")
            }

            if (uiState.selectedEmployee != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = viewModel::recordManualAttendance,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Text(" ✅ Confirmar Marcaje Manual para ${uiState.selectedEmployee?.firstName}")
                }
            }
        }
    }
}
