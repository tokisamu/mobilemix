# Bluetooth Chat App

This is a Bluetooth-based chat application that allows users to connect and exchange messages
wirelessly between two Android devices using Bluetooth. It handles both connection establishment and
messaging while ensuring the app reacts to connection drops.

## Features

- **Device Discovery**: One user can make their phone discoverable, while the other scans for nearby
  devices.
- **Connection Handling**: Once a device is found, users can connect by selecting the device name.
  The app shows when the devices are connected.
- **Disconnection Management**: If one device goes out of range or disconnects, the app resets the
  connection status but retains the previous messages.
- **Chatting**: Users can exchange messages in real-time once the connection is established.
- **Automatic Reset**: The app resets the state and allows reconnection without losing chat history
  after a disconnect.

## Instructions

1. **Making a device discoverable**:
    - One user should press the `Discoverable` button to make their phone visible for Bluetooth
      scanning.

2. **Scanning and connecting**:
    - The second user should press the `Start Scan` button, find the first user's phone in the list
      of discovered devices, and click the device name to connect.

3. **Chatting**:
    - Once connected, the chat screen will allow users to send and receive messages.
    - If either device disconnects or moves out of range, the app will show a "Disconnected" message
      but will retain the chat history.

## Technologies Used

- **Kotlin**: Main language used for Android development.
- **Jetpack Compose**: For building the UI.
- **Bluetooth API**: For handling Bluetooth communication.
- **Koin**: Dependency injection framework.
- **MVVM Architecture**: For clean and maintainable code.
- **Coroutines**: For asynchronous operations.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

**Author**: Paruyr Saghatelyan
