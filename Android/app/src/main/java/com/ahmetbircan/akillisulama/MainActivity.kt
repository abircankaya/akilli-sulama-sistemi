package com.ahmetbircan.akillisulama

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ahmetbircan.akillisulama.ui.AkilliSulamaApp
import com.ahmetbircan.akillisulama.ui.theme.AkilliSulamaTheme

class MainActivity : ComponentActivity() {

    // Samsung hover bug workaround
    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        return try {
            super.dispatchGenericMotionEvent(ev)
        } catch (e: IllegalStateException) {
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Samsung ACTION_HOVER_EXIT bug için global handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is IllegalStateException &&
                throwable.message?.contains("ACTION_HOVER_EXIT") == true) {
                // Bu hatayı yoksay - Samsung Compose bug'ı
                return@setDefaultUncaughtExceptionHandler
            }
            // Diğer hataları normal şekilde işle
            defaultHandler?.uncaughtException(thread, throwable)
        }

        setContent {
            AkilliSulamaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AkilliSulamaApp()
                }
            }
        }
    }
}
