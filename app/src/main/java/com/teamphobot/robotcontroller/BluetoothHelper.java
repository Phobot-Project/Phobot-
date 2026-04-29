package com.teamphobot.robotcontroller;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * BluetoothHelper handles Bluetooth RFCOMM serial communication.
 *
 * How it works:
 *   1. A Bluetooth-to-serial adapter (HC-05 or HC-06) is wired to the
 *      Kobuki robot's serial port (TX/RX pins).
 *   2. The user pairs the adapter from Android Settings first.
 *   3. This class connects to the paired adapter via RFCOMM (serial port profile).
 *   4. Kobuki binary packets are sent through the Bluetooth socket's OutputStream.
 *   5. The adapter forwards the data to the Kobuki over its serial connection.
 *
 * The adapter handles baud rate and serial settings on its side (usually
 * configured via AT commands, e.g. AT+BAUD8 for 115200). No FTDI init
 * is needed here because the adapter itself is the serial bridge.
 */
public class BluetoothHelper {

    private static final String TAG = "BluetoothHelper";

    // Standard Serial Port Profile UUID for RFCOMM
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private BluetoothDevice connectedDevice;
    private String lastError = "";

    public BluetoothHelper() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Returns true if the phone has Bluetooth hardware.
     */
    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null;
    }

    /**
     * Returns true if Bluetooth is turned on.
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Returns a list of paired Bluetooth devices.
     * The user must have paired the HC-05/HC-06 from Android Settings first.
     */
    @SuppressLint("MissingPermission")
    public List<BluetoothDevice> getPairedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        if (bluetoothAdapter == null) return devices;

        try {
            Set<BluetoothDevice> bonded = bluetoothAdapter.getBondedDevices();
            if (bonded != null) {
                devices.addAll(bonded);
            }
        } catch (SecurityException e) {
            lastError = "Bluetooth permission denied";
            Log.e(TAG, lastError, e);
        }

        return devices;
    }

    /**
     * Connects to a Bluetooth device on a background thread.
     * When done, calls the callback on the calling thread.
     *
     * @param device   The paired BluetoothDevice to connect to
     * @param callback Called with true on success, false on failure
     */
    @SuppressLint("MissingPermission")
    public void connect(BluetoothDevice device, ConnectCallback callback) {
        new Thread(() -> {
            try {
                // Cancel discovery to speed up connection
                try { bluetoothAdapter.cancelDiscovery(); } catch (Exception ignored) {}

                // Create RFCOMM socket
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);

                // Connect (this blocks until connected or timeout)
                socket.connect();

                // Get output stream for sending data
                outputStream = socket.getOutputStream();
                connectedDevice = device;
                lastError = "";

                Log.d(TAG, "Connected to " + device.getName());
                callback.onResult(true);

            } catch (IOException e) {
                lastError = "Bluetooth connection failed: " + e.getMessage();
                Log.e(TAG, lastError, e);
                close();
                callback.onResult(false);
            } catch (SecurityException e) {
                lastError = "Bluetooth permission denied";
                Log.e(TAG, lastError, e);
                close();
                callback.onResult(false);
            }
        }).start();
    }

    /**
     * Sends a byte array through the Bluetooth RFCOMM connection.
     *
     * @param data Kobuki protocol packet bytes
     * @return true if sent successfully
     */
    public synchronized boolean send(byte[] data) {
        if (outputStream == null) {
            lastError = "Not connected";
            return false;
        }
        if (data == null || data.length == 0) {
            lastError = "No data";
            return false;
        }

        try {
            outputStream.write(data);
            outputStream.flush();
            lastError = "";
            return true;
        } catch (IOException e) {
            lastError = "Bluetooth send failed: " + e.getMessage();
            Log.e(TAG, lastError, e);
            return false;
        }
    }

    /**
     * Closes the Bluetooth connection and releases resources.
     */
    public void close() {
        try { if (outputStream != null) outputStream.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        outputStream = null;
        socket = null;
        connectedDevice = null;
        lastError = "";
        Log.d(TAG, "Bluetooth connection closed");
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && outputStream != null;
    }

    @SuppressLint("MissingPermission")
    public String getDeviceName() {
        if (connectedDevice == null) return "None";
        try {
            String name = connectedDevice.getName();
            return (name != null && !name.isEmpty()) ? name : connectedDevice.getAddress();
        } catch (SecurityException e) {
            return connectedDevice.getAddress();
        }
    }

    public String getLastError() { return lastError; }

    /** Callback for async connect operation. */
    public interface ConnectCallback {
        void onResult(boolean success);
    }
}
