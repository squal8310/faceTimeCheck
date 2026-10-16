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

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.android.architecture.blueprints.todoapp.attendance.OvalShape
import com.example.android.architecture.blueprints.todoapp.biometrics.FaceDetectorHelper
import com.example.android.architecture.blueprints.todoapp.biometrics.TensorFlowFaceEmbedder
import timber.log.Timber
import java.util.concurrent.Executors

enum class EnrollmentStep {
    FRONTAL,
    LEFT,
    RIGHT,
    COMPLETED
}

@Composable
fun MultiAngleFaceEnrollmentDialog(
    faceDetectorHelper: FaceDetectorHelper,
    faceEmbedder: TensorFlowFaceEmbedder,
    onEnrollmentComplete: (compositeEmbedding: List<Float>, previewBitmap: Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStep by remember { mutableStateOf(EnrollmentStep.FRONTAL) }
    val capturedEmbeddings = remember { mutableStateListOf<FloatArray>() }
    var lastCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enrolamiento Facial 3D",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                // Banner de Instrucciones
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (currentStep) {
                            EnrollmentStep.COMPLETED -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val instructionText = when (currentStep) {
                            EnrollmentStep.FRONTAL -> "1. Mira de FRENTE a la cámara"
                            EnrollmentStep.LEFT -> "2. Gira levemente la cabeza a la IZQUIERDA ⬅️"
                            EnrollmentStep.RIGHT -> "3. Gira levemente la cabeza a la DERECHA ➡️"
                            EnrollmentStep.COMPLETED -> "¡Geometría 3D Capturada con Éxito! 🎉"
                        }
                        Text(
                            text = instructionText,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (currentStep == EnrollmentStep.COMPLETED) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Indicadores de Ángulos (3 Puntos/Anillos)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StepBadge(label = "1. Frente", isDone = currentStep.ordinal > 0)
                    StepBadge(label = "2. Izquierda", isDone = currentStep.ordinal > 1)
                    StepBadge(label = "3. Derecha", isDone = currentStep.ordinal > 2)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cámara en Óvalo
                Box(
                    modifier = Modifier
                        .size(240.dp, 300.dp)
                        .clip(OvalShape())
                        .border(
                            4.dp,
                            if (currentStep == EnrollmentStep.COMPLETED) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            OvalShape()
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    if (currentStep == EnrollmentStep.COMPLETED) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }

                                    faceDetectorHelper.processImageProxy(
                                        imageProxy = imageProxy,
                                        onFaceDetected = { croppedBitmap, face ->
                                            val yaw = face.headEulerAngleY // Giro horizontal

                                            when (currentStep) {
                                                EnrollmentStep.FRONTAL -> {
                                                    if (yaw in -12.0f..12.0f) {
                                                        val embed = faceEmbedder.getEmbedding(croppedBitmap)
                                                        capturedEmbeddings.add(embed)
                                                        lastCapturedBitmap = croppedBitmap
                                                        Timber.tag("FaceBiometrics").i("Ángulo 1 (FRONTAL) capturado. Yaw = %.1f°", yaw)
                                                        currentStep = EnrollmentStep.LEFT
                                                    }
                                                }
                                                EnrollmentStep.LEFT -> {
                                                    if (yaw > 12.0f) { // Mirando hacia la izquierda desde la perspectiva frontal
                                                        val embed = faceEmbedder.getEmbedding(croppedBitmap)
                                                        capturedEmbeddings.add(embed)
                                                        Timber.tag("FaceBiometrics").i("Ángulo 2 (IZQUIERDA) capturado. Yaw = %.1f°", yaw)
                                                        currentStep = EnrollmentStep.RIGHT
                                                    }
                                                }
                                                EnrollmentStep.RIGHT -> {
                                                    if (yaw < -12.0f) { // Mirando hacia la derecha desde la perspectiva frontal
                                                        val embed = faceEmbedder.getEmbedding(croppedBitmap)
                                                        capturedEmbeddings.add(embed)
                                                        Timber.tag("FaceBiometrics").i("Ángulo 3 (DERECHA) capturado. Yaw = %.1f°", yaw)
                                                        currentStep = EnrollmentStep.COMPLETED

                                                        val composite = calculateCompositeEmbedding(capturedEmbeddings)
                                                        val previewBmp = lastCapturedBitmap ?: croppedBitmap
                                                        onEnrollmentComplete(composite, previewBmp)
                                                    }
                                                }
                                                EnrollmentStep.COMPLETED -> {}
                                            }
                                        },
                                        onNoFace = {},
                                        onError = { e ->
                                            Log.e("MultiAngleEnroll", "Error en detección", e)
                                        }
                                    )
                                }

                                val cameraSelector = try {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } catch (e: Exception) {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (exc: Exception) {
                                    Log.e("MultiAngleEnroll", "Error vinculación cámara", exc)
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (currentStep == EnrollmentStep.COMPLETED) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Text(" Listo - Guardar Enrolamiento 3D")
                    }
                } else {
                    Text(
                        text = "Realiza los giros indicados para registrar tu rostro en 3D.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StepBadge(label: String, isDone: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isDone) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        if (isDone) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun calculateCompositeEmbedding(embeddings: List<FloatArray>): List<Float> {
    if (embeddings.isEmpty()) return emptyList()
    val vectorSize = embeddings[0].size
    val result = FloatArray(vectorSize)

    for (i in 0 until vectorSize) {
        var sum = 0f
        for (embed in embeddings) {
            sum += embed[i]
        }
        result[i] = sum / embeddings.size
    }
    return result.toList()
}
