package com.paruyr.fluencytask.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.paruyr.fluencytask.domain.model.FluencyMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

interface BluetoothRepository {
    suspend fun sendMessage(message: FluencyMessage)
    fun observeMessages(): Flow<FluencyMessage>
    fun startDiscovery(): Flow<BluetoothDevice>?
    fun stopDiscovery()
    suspend fun connectToDevice(device: BluetoothDevice): Boolean
    suspend fun startListeningForConnections(): Flow<BluetoothDevice>?
    fun isConnected(): Boolean
    fun observeDisconnection(): Flow<Boolean>
    fun resetConnection() // Add reset function
}

// BluetoothRepositoryImpl: Implementation of the BluetoothRepository
class BluetoothRepositoryImpl(
    private val context: Context,
) : BluetoothRepository {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val _messages = MutableSharedFlow<FluencyMessage>()
    private val _discoveredDevices = MutableSharedFlow<BluetoothDevice>(replay = 1)
    private val _disconnectionHandler = MutableSharedFlow<Boolean>(replay = 1)
    private var bluetoothSocket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var isConnected = false

    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    Log.d(
                        "BluetoothRepository",
                        "Discovered device: ${device.name ?: "Unknown"} - ${device.address}"
                    )
                    _discoveredDevices.tryEmit(it)
                }
            }
        }
    }

    override suspend fun sendMessage(message: FluencyMessage) {
        bluetoothSocket?.let { socket ->
            withContext(Dispatchers.IO) {
                try {
                    val outputStream: OutputStream = socket.outputStream
                    outputStream.write(message.content.toByteArray())
                    Log.d("BluetoothRepository", "Sent message: ${message.content}")
                } catch (e: Exception) {
                    Log.e("BluetoothRepository", "Failed to send message: ${e.message}")
                }
            }
        }
    }

    // Observe received messages
    override fun observeMessages(): Flow<FluencyMessage> = _messages.asSharedFlow()

    // Start Bluetooth discovery
    @SuppressLint("MissingPermission")
    override fun startDiscovery(): Flow<BluetoothDevice>? {
        if (bluetoothAdapter?.isEnabled == true) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            context.registerReceiver(receiver, filter)
            bluetoothAdapter.startDiscovery()
            Log.d("BluetoothRepository", "Bluetooth discovery started.")
            return _discoveredDevices.asSharedFlow()
        } else {
            Log.e("BluetoothRepository", "Bluetooth is not enabled.")
            return null
        }
    }

    // Stop Bluetooth discovery
    @SuppressLint("MissingPermission")
    override fun stopDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
            Log.d("BluetoothRepository", "Bluetooth discovery stopped.")
        }
        context.unregisterReceiver(receiver)
    }

    // Connect to a discovered Bluetooth device
    @SuppressLint("MissingPermission")
    override suspend fun connectToDevice(device: BluetoothDevice): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                bluetoothSocket?.close()
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()
                isConnected = true
                Log.d("BluetoothRepository", "Connected to device: ${device.name}")
                startListeningForMessages(bluetoothSocket!!)
                true
            } catch (e: Exception) {
                Log.e("BluetoothRepository", "Failed to connect to device: ${e.message}")
                isConnected = false
                _disconnectionHandler.tryEmit(true) // Notify disconnection on failure
                false
            }
        }
    }

    // Start listening for connections as a server
    @SuppressLint("MissingPermission")
    override suspend fun startListeningForConnections(): Flow<BluetoothDevice>? {
        return withContext(Dispatchers.IO) {
            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("MyApp", MY_UUID)

            val clientDeviceFlow = MutableSharedFlow<BluetoothDevice>(replay = 1)
            GlobalScope.launch {
                try {
                    val socket = serverSocket?.accept()
                    bluetoothSocket = socket
                    socket?.remoteDevice?.let { device ->
                        Log.d("BluetoothRepository", "Client device connected: ${device.name}")
                        clientDeviceFlow.emit(device)
                        startListeningForMessages(socket)
                    }
                } catch (e: Exception) {
                    Log.e(
                        "BluetoothRepository",
                        "Error while listening for connections: ${e.message}"
                    )
                    _disconnectionHandler.tryEmit(true) // Notify disconnection if error happens
                }
            }
            clientDeviceFlow.asSharedFlow()
        }
    }

    // Observe disconnection
    override fun observeDisconnection(): Flow<Boolean> = _disconnectionHandler.asSharedFlow()

    // Check if the device is connected
    override fun isConnected(): Boolean = isConnected

    // Start listening for incoming messages
    private fun startListeningForMessages(socket: BluetoothSocket) {
        val inputStream: InputStream = socket.inputStream
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val buffer = ByteArray(1024)
                while (true) {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val incomingMessage = String(buffer, 0, bytes).trim()
                        if (incomingMessage.isNotBlank()) {
                            _messages.emit(FluencyMessage(incomingMessage, false))
                            Log.d("BluetoothRepository", "Received message: $incomingMessage")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothRepository", "Connection lost: ${e.message}")
                isConnected = false
                _disconnectionHandler.tryEmit(true) // Notify disconnection
            }
        }
    }

    // Reset the Bluetooth connection state
    override fun resetConnection() {
        try {
            bluetoothSocket?.close()
            serverSocket?.close()
            bluetoothSocket = null
            serverSocket = null
            isConnected = false
            Log.d("BluetoothRepository", "Bluetooth connection reset.")
        } catch (e: Exception) {
            Log.e("BluetoothRepository", "Error resetting connection: ${e.message}")
        }
    }
}
