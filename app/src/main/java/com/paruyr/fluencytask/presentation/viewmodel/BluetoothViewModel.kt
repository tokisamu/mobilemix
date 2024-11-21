package com.paruyr.fluencytask.presentation.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paruyr.fluencytask.ECC
import com.paruyr.fluencytask.ElGamel
import com.paruyr.fluencytask.SecretShare
import com.paruyr.fluencytask.FinitePrimeField
import com.paruyr.fluencytask.MainActivity
import com.paruyr.fluencytask.Point
import com.paruyr.fluencytask.PrimeCurve
import com.paruyr.fluencytask.data.BluetoothRepository
import com.paruyr.fluencytask.domain.model.FluencyMessage
import com.paruyr.fluencytask.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.security.SecureRandom
import java.util.ArrayList
import java.util.Calendar
import java.util.UUID


class BluetoothViewModel(
    application: Application,
    private val sendMessageUseCase: SendMessageUseCase,
    private val bluetoothRepository: BluetoothRepository,
) : AndroidViewModel(application) {
    private val _messages = mutableStateListOf<FluencyMessage>()
    val messages: List<FluencyMessage> get() = _messages
    private val _needsPermissions = MutableStateFlow(false)
    val needsPermissions: StateFlow<Boolean> = _needsPermissions

    private val _isConnected = mutableStateOf(false)
    val isConnected: Boolean get() = _isConnected.value

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val _discoveredDevices = mutableStateListOf<BluetoothDevice>()
    val discoveredDevices: List<BluetoothDevice> get() = _discoveredDevices
    init {
        // Handle permissions and start listening for connections when granted
        checkBluetoothPermissions()
        if (_needsPermissions.value.not()) {
            startListeningForConnections()
        }

        // Collect messages received from the repository
        viewModelScope.launch {
            bluetoothRepository.observeMessages().collect { message ->
                _messages.add(message)
            }
        }

        // Collect disconnection events from the repository
        viewModelScope.launch {
            bluetoothRepository.observeDisconnection().collect {
                handleDisconnection()
            }
        }
    }

    private fun handleDisconnection() {
        checkBluetoothPermissions()
        _isConnected.value = false
        bluetoothRepository.resetConnection()
        Log.e("BluetoothViewModel", "Device disconnected. Please reconnect.")
        startListeningForConnections() // Automatically start listening again
    }

    fun makeDeviceDiscoverable(context: Context, durationInSeconds: Int = 300) {
        checkBluetoothPermissions()
        if (bluetoothAdapter?.isEnabled == true) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, durationInSeconds)
            }
            context.startActivity(discoverableIntent)
        } else {
            Log.e("BluetoothViewModel", "Bluetooth is not enabled.")
        }
    }

    // Send a message to the connected device
    fun sendMessage(message: String) {
        viewModelScope.launch {
            sendMessageUseCase(FluencyMessage(message, true))
            _messages.add(FluencyMessage(message, true)) // Add to local state
        }
    }

    private fun checkBluetoothPermissions() {
        val requiredPermissions =
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION // Needed for discovery
            )

        val context: Context = getApplication<Application>().applicationContext
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        //ActivityCompat.requestPermissions(MainActivity(), arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE), 1);
        _needsPermissions.value = !allGranted // If not all permissions are granted, request them
    }

    // Call this function once permissions are granted
    fun onPermissionsGranted() {
        _needsPermissions.value = false
        startListeningForConnections() // Start listening for connections after permissions are granted
    }

    // Start Bluetooth discovery and collect devices
    @SuppressLint("MissingPermission")
    fun startBluetoothDiscovery() {
        checkBluetoothPermissions()
        _discoveredDevices.clear() // Clear previous discovered devices

        viewModelScope.launch {
            Log.d("BluetoothViewModel", "Starting Bluetooth discovery...")

            bluetoothRepository.startDiscovery()?.collect { device ->
                // Prevent duplicate devices from being added
                if (_discoveredDevices.none { it.address == device.address }) {
                    Log.d(
                        "BluetoothViewModel",
                        "Discovered device: ${device.name ?: device.address}"
                    )
                    _discoveredDevices.add(device)
                } else {
                    Log.d(
                        "BluetoothViewModel",
                        "Duplicate device ignored: ${device.name ?: device.address}"
                    )
                }
            }
        }
    }

    // Automatically start listening for connections when app starts or permission is granted
    @SuppressLint("MissingPermission")
    private fun startListeningForConnections() {
        viewModelScope.launch {
            bluetoothRepository.startListeningForConnections()?.collect { clientDevice ->
                Log.d(
                    "BluetoothViewModel",
                    "Client device connected: ${clientDevice.name ?: clientDevice.address}"
                )
                _isConnected.value = true
            }
        }
    }

    // Stop Bluetooth discovery
    fun stopDiscovery() {
        bluetoothRepository.stopDiscovery()
        Log.d("BluetoothViewModel", "Bluetooth discovery stopped.")
    }

    fun createGroup() {
        bluetoothRepository.createGroup();
    }

    fun joinGroup() {
        bluetoothRepository.joinGroup(1);
    }

    // Connect to a selected Bluetooth device
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            Log.d(
                "BluetoothViewModel",
                "Attempting to connect to device: ${device.name ?: device.address}"
            )
            val success = bluetoothRepository.connectToDevice(device)
            _isConnected.value = success

            if (success) {
                Log.d(
                    "BluetoothViewModel",
                    "Successfully connected to ${device.name ?: device.address}"
                )
                _discoveredDevices.clear() // Clear discovered devices after successful connection
            } else {
                Log.e("BluetoothViewModel", "Failed to connect to ${device.name ?: device.address}")
            }
        }
    }
}
