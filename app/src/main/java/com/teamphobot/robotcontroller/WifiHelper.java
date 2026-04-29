package com.teamphobot.robotcontroller;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * WifiHelper handles WiFi TCP socket communication with the Kobuki robot.
 *
 * How it works:
 *   1. An ESP8266 or ESP32 WiFi module is connected to the Kobuki's serial port.
 *   2. The ESP runs a TCP server (e.g. on port 8080).
 *   3. The phone connects to the ESP's WiFi network (or same local network).
 *   4. This class opens a TCP socket to the ESP's IP and port.
 *   5. Kobuki binary packets are sent through the socket's OutputStream.
 *   6. The ESP forwards the data to the Kobuki over its serial connection.
 *
 * Common setup:
 *   - ESP8266 in AP mode creates its own WiFi network (default IP: 192.168.4.1)
 *   - Or ESP connected to the same WiFi router as the phone
 *   - TCP server running on a known port (e.g. 8080)
 */
public class WifiHelper {

    private static final String TAG = "WifiHelper";

    // TCP connection timeout in milliseconds
    private static final int CONNECT_TIMEOUT_MS = 5000;

    private Socket socket;
    private OutputStream outputStream;
    private String connectedIp;
    private int connectedPort;
    private String lastError = "";

    /**
     * Connects to the WiFi bridge on a background thread.
     *
     * @param ip       The IP address of the WiFi bridge (e.g. "192.168.4.1")
     * @param port     The TCP port (e.g. 8080)
     * @param callback Called with true on success, false on failure
     */
    public void connect(String ip, int port, ConnectCallback callback) {
        new Thread(() -> {
            try {
                // Close any existing connection first
                close();

                // Create a new TCP socket
                socket = new Socket();

                // Connect with timeout
                socket.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS);

                // Disable Nagle's algorithm for low latency
                socket.setTcpNoDelay(true);

                // Get the output stream
                outputStream = socket.getOutputStream();
                connectedIp = ip;
                connectedPort = port;
                lastError = "";

                Log.d(TAG, "Connected to " + ip + ":" + port);
                callback.onResult(true);

            } catch (IOException e) {
                lastError = "WiFi connection failed: " + e.getMessage();
                Log.e(TAG, lastError, e);
                close();
                callback.onResult(false);
            }
        }).start();
    }

    /**
     * Sends a byte array through the TCP socket.
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
            lastError = "WiFi send failed: " + e.getMessage();
            Log.e(TAG, lastError, e);
            return false;
        }
    }

    /**
     * Closes the TCP socket and releases resources.
     */
    public void close() {
        try { if (outputStream != null) outputStream.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        outputStream = null;
        socket = null;
        connectedIp = null;
        connectedPort = 0;
        lastError = "";
        Log.d(TAG, "WiFi connection closed");
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && outputStream != null;
    }

    public String getDeviceName() {
        if (connectedIp == null) return "None";
        return connectedIp + ":" + connectedPort;
    }

    public String getLastError() { return lastError; }

    /** Callback for async connect operation. */
    public interface ConnectCallback {
        void onResult(boolean success);
    }
}
