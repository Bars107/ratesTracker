package com.bars.exchange.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.bars.exchange.tracker.ui.main.MainAppScreen // Changed
import com.bars.exchange.tracker.ui.theme.TrackerApplicationTheme
import dagger.hilt.android.AndroidEntryPoint // Added for Hilt

@AndroidEntryPoint // Added for Hilt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrackerApplicationTheme {
                // Scaffold removed from here, it's now in MainAppScreen
                MainAppScreen(modifier = Modifier.fillMaxSize()) // Changed
            }
        }
    }
}

// Greeting and GreetingPreview composables can be removed or kept for other previews.
// For clarity, I'll remove them from this direct modification of MainActivity.