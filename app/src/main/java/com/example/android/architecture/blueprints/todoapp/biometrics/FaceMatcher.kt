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

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class FaceMatcher @Inject constructor() {

    fun calculateSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }

        if (norm1 == 0f || norm2 == 0f) return 0f
        return (dotProduct / (sqrt(norm1.toDouble()) * sqrt(norm2.toDouble()))).toFloat()
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
        const val DEFAULT_THRESHOLD = 0.80f
    }
}
