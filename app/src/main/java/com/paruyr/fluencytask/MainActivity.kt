package com.paruyr.fluencytask

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.paruyr.fluencytask.presentation.ui.BluetoothScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluetoothApp()
        }
            val requiredPermissions =
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION // Needed for discovery
                )
             ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE), 1);
    }

}

@Composable
fun BluetoothApp() {
    MaterialTheme {
        BluetoothScreen()
    }
}