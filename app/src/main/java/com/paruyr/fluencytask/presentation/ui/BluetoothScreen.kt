package com.paruyr.fluencytask.presentation.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.paruyr.fluencytask.presentation.viewmodel.BluetoothViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@SuppressLint("MissingPermission")
@Composable
fun BluetoothScreen(
    viewModel: BluetoothViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {

    val messages = viewModel.messages
    var messageInput by remember { mutableStateOf("") }
    val needsPermissions by viewModel.needsPermissions.collectAsState()
    val isConnected by remember { derivedStateOf { viewModel.isConnected } }
    val discoveredDevices = viewModel.discoveredDevices
    val context = LocalContext.current

    // LazyListState to control scrolling
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Permission launcher for requesting Bluetooth permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.onPermissionsGranted()
        }
    }

    // Track whether the permission request has already been triggered
    var permissionRequestTriggered by remember { mutableStateOf(false) }

    // Check permissions and start discovery if allowed
    val startDiscovery = {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        // If permissions are already granted, start discovery
        if (!needsPermissions) {
            viewModel.startBluetoothDiscovery()
        } else {
            // Launch permission request
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        // Show connection status
        Text(
            text = if (isConnected) "Connected to device" else "Disconnected",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Conditionally show buttons and discovered devices when disconnected
        if (!isConnected) {
            // Row for horizontal button arrangement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Button to make the device discoverable
                Button(onClick = { viewModel.makeDeviceDiscoverable(context) }) {
                    Text("Discoverable")
                }

                // Button to start discovery
                Button(onClick = { startDiscovery() }) {
                    Text("Start Scan")
                }

                // Button to stop discovery
                Button(onClick = { viewModel.stopDiscovery() }) {
                    Text("Stop Scan")
                }

            }


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                // Button to create group 1
                Button(onClick = { viewModel.createGroup() }) {
                    Text("Create Group")
                }

                // Button to join group 1
                Button(onClick = { viewModel.joinGroup() }) {
                    Text("Join Group")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display discovered devices
            Text("Discovered Devices:")
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(discoveredDevices.size) { index ->
                    val device = discoveredDevices[index]
                    Text(
                        text = "Device: ${device.name ?: device.address}",
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { viewModel.connectToDevice(device) } // Click to connect
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display messages in a LazyColumn (chat area) only when connected
        if (isConnected) {
            LazyColumn(
                state = listState, // Use list state for scrolling
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(), // Ensure chat takes more space when connected
                verticalArrangement = Arrangement.Bottom
            ) {
                items(messages) { message ->
                    Text(
                        text = if (message.isSent) "Sent: ${message.content}" else "Received: ${message.content}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input field and send button for messages when connected
            Row {
                TextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Message") }
                )
                Button(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            viewModel.sendMessage(messageInput)
                            messageInput = ""

                            // Ensure that the list isn't empty before scrolling
                            coroutineScope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.scrollToItem(messages.size - 1) // Scroll to the bottom when a new message is sent
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Send")
                }
            }
        }
    }
}
