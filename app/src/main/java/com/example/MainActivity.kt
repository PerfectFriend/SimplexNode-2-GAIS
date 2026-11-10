package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainAppLayout
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Anti-forensic protection: In browser streaming emulators (AI Studio), FLAG_SECURE
    // blocks the virtual display capture surface, causing "failed to connect to device".
    // We only enable FLAG_SECURE in release builds or when explicitly toggled by the user.
    if (!BuildConfig.DEBUG) {
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
    }
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: MainViewModel = viewModel()
        MainAppLayout(viewModel = viewModel)
      }
    }
  }
}
