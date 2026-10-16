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

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TensorFlowFaceEmbedder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private val modelInputSize = 112
    private val embeddingSize = 128

    init {
        try {
            val modelFile = FileUtil.loadMappedFile(context, MODEL_FILE_NAME)
            interpreter = Interpreter(modelFile)
            Timber.tag("FaceBiometrics").i("Modelo TFLite '%s' cargado exitosamente desde assets.", MODEL_FILE_NAME)
        } catch (e: Exception) {
            interpreter = null
            Timber.tag("FaceBiometrics").w("No se encontró el modelo TFLite '%s' en assets. Se usará el extractor algorítmico secundario.", MODEL_FILE_NAME)
        }
    }

    fun getEmbedding(faceBitmap: Bitmap): FloatArray {
        val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, modelInputSize, modelInputSize, true)
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)
        val outputArray = Array(1) { FloatArray(embeddingSize) }

        val activeInterpreter = interpreter
        if (activeInterpreter != null) {
            activeInterpreter.run(inputBuffer, outputArray)
            val embedding = outputArray[0]
            Timber.tag("FaceBiometrics").d("Inferencia TFLite ejecutada. Vector extraído (%d flotantes). Muestra: [%.3f, %.3f, %.3f]", embedding.size, embedding[0], embedding[1], embedding[2])
            return embedding
        } else {
            Timber.tag("FaceBiometrics").d("Ejecutando extractor alternativo de vectores (%dx%d px).", faceBitmap.width, faceBitmap.height)
            return generatePseudoEmbedding(resizedBitmap)
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * modelInputSize * modelInputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(modelInputSize * modelInputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixelIndex = 0
        for (i in 0 until modelInputSize) {
            for (j in 0 until modelInputSize) {
                val value = intValues[pixelIndex++]
                byteBuffer.putFloat(((value shr 16 and 0xFF) - 127.5f) / 128.0f)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - 127.5f) / 128.0f)
                byteBuffer.putFloat(((value and 0xFF) - 127.5f) / 128.0f)
            }
        }
        return byteBuffer
    }

    private fun generatePseudoEmbedding(bitmap: Bitmap): FloatArray {
        val embedding = FloatArray(embeddingSize)
        val width = bitmap.width
        val height = bitmap.height
        val step = (width * height) / embeddingSize

        for (i in 0 until embeddingSize) {
            val pixel = bitmap.getPixel((i * step) % width, ((i * step) / width) % height)
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            embedding[i] = (r * 0.299f + g * 0.587f + b * 0.114f)
        }
        return embedding
    }

    companion object {
        private const val MODEL_FILE_NAME = "mobilefacenet.tflite"
    }
}
