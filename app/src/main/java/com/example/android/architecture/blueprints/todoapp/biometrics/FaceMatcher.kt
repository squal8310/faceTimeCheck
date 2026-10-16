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

package com.example.android.architecture.blueprints.todoapp.biometrics

import com.example.android.architecture.blueprints.todoapp.data.source.local.EmployeeEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

data class FaceMatchResult(
    val employee: EmployeeEntity,
    val similarityScore: Float
)

@Singleton
class FaceMatcher @Inject constructor() {

    fun calculateSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0f

        val norm1 = normalizeL2(v1)
        val norm2 = normalizeL2(v2)

        var dotProduct = 0f
        for (i in norm1.indices) {
            dotProduct += norm1[i] * norm2[i]
        }

        return dotProduct.coerceIn(-1.0f, 1.0f)
    }

    private fun normalizeL2(vector: FloatArray): FloatArray {
        var sumSquare = 0f
        for (v in vector) {
            sumSquare += v * v
        }
        val norm = sqrt(sumSquare.toDouble()).toFloat()
        if (norm == 0f) return vector

        val result = FloatArray(vector.size)
        for (i in vector.indices) {
            result[i] = vector[i] / norm
        }
        return result
    }

    /**
     * Busca en TODOS los empleados enrolados para encontrar a la persona con el
     * puntaje de similitud MÁS ALTO e imprime logs detallados en Logcat (tag: FaceBiometrics).
     */
    fun findBestMatch(
        currentEmbedding: FloatArray,
        enrolledEmployees: List<EmployeeEntity>,
        threshold: Float = DEFAULT_THRESHOLD
    ): FaceMatchResult? {
        Timber.tag("FaceBiometrics").d("--- INICIANDO BÚSQUEDA DE MEJOR COINCIDENCIA ---")
        Timber.tag("FaceBiometrics").d("Total de empleados enrolados encontrados en DB: %d", enrolledEmployees.size)

        if (enrolledEmployees.isEmpty()) {
            Timber.tag("FaceBiometrics").w("No hay ningún empleado con biometría registrada en la base de datos.")
            return null
        }

        var bestEmployee: EmployeeEntity? = null
        var maxScore = -1.0f

        for (emp in enrolledEmployees) {
            val savedEmbed = emp.faceEmbedding?.toFloatArray()
            if (savedEmbed == null) {
                Timber.tag("FaceBiometrics").w("Empleado '%s' (Code: %s) marcado con biometría pero sin embedding guardado.", emp.fullName, emp.employeeCode)
                continue
            }
            val score = calculateSimilarity(savedEmbed, currentEmbedding)
            Timber.tag("FaceBiometrics").d("Evaluando Empleado: '%s' (Code: %s) -> Similitud: %.2f%% (Score: %.4f)", emp.fullName, emp.employeeCode, score * 100, score)

            if (score > maxScore) {
                maxScore = score
                bestEmployee = emp
            }
        }

        return if (bestEmployee != null) {
            Timber.tag("FaceBiometrics").i("Candidato con mayor puntaje: '%s' con score %.4f (%.2f%%). Umbral requerido: %.2f", bestEmployee.fullName, maxScore, maxScore * 100, threshold)
            if (maxScore >= threshold) {
                Timber.tag("FaceBiometrics").i("SUCCESS: ¡Coincidencia ACEPTADA para '%s'!", bestEmployee.fullName)
                FaceMatchResult(bestEmployee, maxScore)
            } else {
                Timber.tag("FaceBiometrics").w("REJECTED: Puntaje %.4f por debajo del umbral mínimo de %.2f", maxScore, threshold)
                null
            }
        } else {
            Timber.tag("FaceBiometrics").w("REJECTED: No se encontró ningún candidato.")
            null
        }
    }

    fun isMatch(
        v1: FloatArray,
        v2: FloatArray,
        threshold: Float = DEFAULT_THRESHOLD
    ): Boolean {
        val similarity = calculateSimilarity(v1, v2)
        return similarity >= threshold
    }

    companion object {
        const val DEFAULT_THRESHOLD = 0.70f // 70% de coincidencia biómétrica
    }
}
