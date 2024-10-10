package com.paruyr.fluencytask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.paruyr.fluencytask.presentation.ui.BluetoothScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluetoothApp()
        }
    }
}

@Composable
fun BluetoothApp() {
    MaterialTheme {
        BluetoothScreen()
    }
}