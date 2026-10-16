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

package com.example.android.architecture.blueprints.todoapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ExecutiveNavyPrimary = Color(0xFF1E3A8A)       // Azul Marino Ejecutivo
val ExecutiveBlueSecondary = Color(0xFF2563EB)     // Azul Vívido Secundario
val ExecutiveDarkNavy = Color(0xFF0F172A)          // Azul Oscuro Profundo
val ExecutiveContainerLight = Color(0xFFDBEAFE)    // Azul Claro para Tarjetas Resaltadas

@Composable
fun TodoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = ExecutiveNavyPrimary,
            onPrimary = Color.White,
            primaryContainer = ExecutiveContainerLight,
            onPrimaryContainer = ExecutiveNavyPrimary,
            secondary = ExecutiveBlueSecondary,
            onSecondary = Color.White,
            tertiary = Color(0xFF3B82F6),
            background = Color(0xFFF8FAFC),
            onBackground = ExecutiveDarkNavy,
            surface = Color.White,
            onSurface = ExecutiveDarkNavy,
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF334155)
        )
    ) {
        content()
    }
}
