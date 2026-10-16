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

import android.content.Context
import android.os.Environment
import com.example.android.architecture.blueprints.todoapp.data.source.local.AttendanceDao
import com.example.android.architecture.blueprints.todoapp.data.source.local.AttendanceRecordEntity
import com.example.android.architecture.blueprints.todoapp.data.source.local.AttendanceType
import com.example.android.architecture.blueprints.todoapp.data.source.local.EmployeeDao
import com.example.android.architecture.blueprints.todoapp.data.source.local.EmployeeEntity
import com.example.android.architecture.blueprints.todoapp.data.source.local.VerificationMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val employeeDao: EmployeeDao,
    private val attendanceDao: AttendanceDao
) {

    private fun getBackupFile(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        return File(downloadsDir, BACKUP_FILE_NAME)
    }

    suspend fun exportBackupToJson(): Pair<Boolean, String> {
        return try {
            val employees = employeeDao.getAllEmployees()
            val attendanceRecords = attendanceDao.getByEmployeeBetween("", 0L, Long.MAX_VALUE)

            val rootJson = JSONObject()

            // Exportar Empleados
            val employeesArray = JSONArray()
            for (emp in employees) {
                val empJson = JSONObject().apply {
                    put("id", emp.id)
                    put("employeeCode", emp.employeeCode)
                    put("firstName", emp.firstName)
                    put("lastName", emp.lastName)
                    put("position", emp.position)
                    put("hourlyRate", emp.hourlyRate)
                    put("overtimeHourlyRate", emp.overtimeHourlyRate)
                    put("dailyHoursLimit", emp.dailyHoursLimit)
                    put("photoPath", emp.photoPath ?: "")
                    put("isBiometricEnrolled", emp.isBiometricEnrolled)
                    put("isActive", emp.isActive)

                    val embedArray = JSONArray()
                    emp.faceEmbedding?.forEach { floatVal -> embedArray.put(floatVal.toDouble()) }
                    put("faceEmbedding", embedArray)
                }
                employeesArray.put(empJson)
            }
            rootJson.put("employees", employeesArray)

            // Exportar Registros de Asistencia
            val attendanceArray = JSONArray()
            for (record in attendanceRecords) {
                val recJson = JSONObject().apply {
                    put("id", record.id)
                    put("employeeId", record.employeeId)
                    put("timestamp", record.timestamp)
                    put("type", record.type.name)
                    put("verificationMethod", record.verificationMethod.name)
                    put("capturedPhotoPath", record.capturedPhotoPath ?: "")
                    put("notes", record.notes ?: "")
                }
                attendanceArray.put(recJson)
            }
            rootJson.put("attendanceRecords", attendanceArray)

            val file = getBackupFile()
            file.writeText(rootJson.toString(2))

            Timber.tag("FaceBiometrics").i("Respaldo exportado exitosamente a: %s", file.absolutePath)
            Pair(true, "Respaldo guardado en Downloads/${file.name}")
        } catch (e: Exception) {
            Timber.tag("FaceBiometrics").e(e, "Error al exportar respaldo JSON")
            Pair(false, "Error al exportar: ${e.localizedMessage}")
        }
    }

    suspend fun importBackupFromJson(): Pair<Boolean, String> {
        return try {
            val file = getBackupFile()
            if (!file.exists()) {
                return Pair(false, "No se encontró el archivo ${BACKUP_FILE_NAME} en carpeta Downloads.")
            }

            val content = file.readText()
            val rootJson = JSONObject(content)

            // Importar Empleados
            if (rootJson.has("employees")) {
                val employeesArray = rootJson.getJSONArray("employees")
                for (i in 0 until employeesArray.length()) {
                    val empJson = employeesArray.getJSONObject(i)

                    val embedList = mutableListOf<Float>()
                    if (empJson.has("faceEmbedding")) {
                        val embedArray = empJson.getJSONArray("faceEmbedding")
                        for (j in 0 until embedArray.length()) {
                            embedList.add(embedArray.getDouble(j).toFloat())
                        }
                    }

                    val emp = EmployeeEntity(
                        id = empJson.getString("id"),
                        employeeCode = empJson.getString("employeeCode"),
                        firstName = empJson.getString("firstName"),
                        lastName = empJson.getString("lastName"),
                        position = empJson.getString("position"),
                        hourlyRate = empJson.getDouble("hourlyRate"),
                        overtimeHourlyRate = empJson.getDouble("overtimeHourlyRate"),
                        dailyHoursLimit = empJson.optDouble("dailyHoursLimit", 8.0),
                        photoPath = empJson.optString("photoPath", "").ifEmpty { null },
                        faceEmbedding = if (embedList.isNotEmpty()) embedList else null,
                        isBiometricEnrolled = empJson.optBoolean("isBiometricEnrolled", false),
                        isActive = empJson.optBoolean("isActive", true)
                    )
                    employeeDao.upsert(emp)
                }
            }

            // Importar Registros de Asistencia
            if (rootJson.has("attendanceRecords")) {
                val attendanceArray = rootJson.getJSONArray("attendanceRecords")
                for (i in 0 until attendanceArray.length()) {
                    val recJson = attendanceArray.getJSONObject(i)
                    val record = AttendanceRecordEntity(
                        id = recJson.getString("id"),
                        employeeId = recJson.getString("employeeId"),
                        timestamp = recJson.getLong("timestamp"),
                        type = AttendanceType.valueOf(recJson.getString("type")),
                        verificationMethod = VerificationMethod.valueOf(recJson.getString("verificationMethod")),
                        capturedPhotoPath = recJson.optString("capturedPhotoPath", "").ifEmpty { null },
                        notes = recJson.optString("notes", "").ifEmpty { null }
                    )
                    attendanceDao.upsert(record)
                }
            }

            Timber.tag("FaceBiometrics").i("Respaldo importado exitosamente desde: %s", file.absolutePath)
            Pair(true, "Respaldo restaurado exitosamente desde Downloads/${file.name}")
        } catch (e: Exception) {
            Timber.tag("FaceBiometrics").e(e, "Error al importar respaldo JSON")
            Pair(false, "Error al importar: ${e.localizedMessage}")
        }
    }

    companion object {
        private const val BACKUP_FILE_NAME = "POSJetFire_Backup.json"
    }
}
