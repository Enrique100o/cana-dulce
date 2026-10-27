package com.canadulce.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canadulce.app.ui.CanaDulceApp
import com.canadulce.app.ui.theme.CañaDulceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CañaDulceTheme {
                CanaDulceApp(viewModel())
            }
        }
    }
}
