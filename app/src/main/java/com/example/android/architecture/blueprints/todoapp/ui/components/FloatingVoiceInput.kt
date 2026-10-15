
package com.example.android.architecture.blueprints.todoapp.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun FloatingVoiceInput() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.getOrNull(0)
            if (!spokenText.isNullOrEmpty()) {
                Log.d("VoiceInput", "Transcribed text: $spokenText")
                // Aquí es donde manejarías el texto transcrito en el futuro
            }
        }
    }

    FloatingActionButton(
        onClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...")
            }
            try {
                launcher.launch(intent)
            } catch (e: Exception) {
                Log.e("VoiceInput", "Error launching voice recognizer", e)
            }
        }
    ) {
        Icon(Icons.Filled.Mic, contentDescription = "Entrada de voz")
    }
}
