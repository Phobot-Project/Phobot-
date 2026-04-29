package com.teamphobot.robotcontroller;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.HashMap;

/**
 * ConnectionManager handles USB, Bluetooth, and WiFi connections.
 * It exposes a single send() method so the rest of the app does not
 * need to know which transport is active.
 */
public class ConnectionManager implements PermissionReceiver.PermissionResultListener {

    private static final String TAG = "ConnectionManager";

    public enum Mode { USB, BLUETOOTH, WIFI }

    public interface ConnectionListener {
        void onConnected(String deviceInfo, Mode mode);
        void onDisconnected(String reason);
        void onError(String errorMessage);
    }

    private final Context context;
    private final UsbManager usbManager;
    private final UsbHelper usbHelper;
    private final BluetoothHelper btHelper;
    private final WifiHelper wifiHelper;
    private final PermissionReceiver permissionReceiver;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ConnectionListener listener;
    private Mode activeMode = Mode.USB;
    private boolean receiverRegistered = false;

    // Stored connection targets (set by MainActivity before calling connect)
    private BluetoothDevice selectedBtDevice;
    private String wifiIp;
    private int wifiPort;

    public ConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
        this.usbHelper = new UsbHelper(usbManager);
        this.btHelper = new BluetoothHelper();
        this.wifiHelper = new WifiHelper();
        this.permissionReceiver = new PermissionReceiver(this);
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.listener = listener;
    }

    public void setMode(Mode mode) {
        this.activeMode = mode;
    }

    public Mode getMode() { return activeMode; }

    public void setBluetoothDevice(BluetoothDevice device) {
        this.selectedBtDevice = device;
    }

    public void setWifiTarget(String ip, int port) {
        this.wifiIp = ip;
        this.wifiPort = port;
    }

    // --- Receiver management ---

    public void registerReceiver() {
        if (receiverRegistered) return;
        IntentFilter filter = new IntentFilter(PermissionReceiver.ACTION_USB_PERMISSION);
        ContextCompat.registerReceiver(context, permissionReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        receiverRegistered = true;
    }

    public void unregisterReceiver() {
        if (!receiverRegistered) return;
        try { context.unregisterReceiver(permissionReceiver); } catch (Exception ignored) {}
        receiverRegistered = false;
    }

    // --- Connect (dispatches to the right helper) ---

    public void connect() {
        switch (activeMode) {
            case USB:
                connectUsb();
                break;
            case BLUETOOTH:
                connectBluetooth();
                break;
            case WIFI:
                connectWifi();
                break;
        }
    }

    private void connectWifi() {
        if (wifiIp == null || wifiIp.isEmpty()) {
            notifyError("Please enter an IP address");
            return;
        }

        Log.d(TAG, "WiFi connecting to " + wifiIp + ":" + wifiPort);
        wifiHelper.connect(wifiIp, wifiPort, success ->
            mainHandler.post(() -> {
                if (success) {
                    notifyConnected("WiFi: " + wifiHelper.getDeviceName(), Mode.WIFI);
                } else {
                    notifyError(wifiHelper.getLastError());
                }
            })
        );
    }

    private void connectBluetooth() {
        if (selectedBtDevice == null) {
            notifyError("No Bluetooth device selected");
            return;
        }

        Log.d(TAG, "Bluetooth connecting to " + selectedBtDevice.getAddress());
        btHelper.connect(selectedBtDevice, success ->
            mainHandler.post(() -> {
                if (success) {
                    notifyConnected("BT: " + btHelper.getDeviceName(), Mode.BLUETOOTH);
                } else {
                    notifyError(btHelper.getLastError());
                }
            })
        );
    }

    // --- USB connect ---

    private void connectUsb() {
        UsbDevice device = findUsbDevice();
        if (device == null) {
            notifyError("No USB device found. Check the cable and OTG adapter.");
            return;
        }
        if (usbManager.hasPermission(device)) {
            openUsbDevice(device);
        } else {
            requestUsbPermission(device);
        }
    }

    private UsbDevice findUsbDevice() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.isEmpty()) return null;
        for (UsbDevice d : deviceList.values()) {
            int vid = d.getVendorId();
            if (vid == 0x0403 || vid == 0x10C4 || vid == 0x1A86) return d;
        }
        return deviceList.values().iterator().next();
    }

    private void requestUsbPermission(UsbDevice device) {
        PendingIntent pi = PendingIntent.getBroadcast(context, 0,
                new Intent(PermissionReceiver.ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE);
        usbManager.requestPermission(device, pi);
    }

    private void openUsbDevice(UsbDevice device) {
        boolean success = usbHelper.openDevice(device);
        if (success) {
            String info = "USB: " + usbHelper.getDeviceName()
                    + " (VID=0x" + String.format("%04X", usbHelper.getVendorId())
                    + " PID=0x" + String.format("%04X", usbHelper.getProductId())
                    + " Chip=" + usbHelper.chipName() + ")";
            notifyConnected(info, Mode.USB);
        } else {
            notifyError("USB: " + usbHelper.getLastError());
        }
    }

    @Override
    public void onPermissionGranted(UsbDevice device) { openUsbDevice(device); }

    @Override
    public void onPermissionDenied(UsbDevice device) {
        notifyError("USB permission denied. Please allow access.");
    }

    // --- Disconnect ---

    public void disconnect() {
        usbHelper.close();
        btHelper.close();
        wifiHelper.close();
        if (listener != null) listener.onDisconnected("Disconnected");
    }

    // --- Send (uses whichever transport is connected) ---

    public boolean send(byte[] data) {
        if (usbHelper.isConnected()) return usbHelper.send(data);
        if (btHelper.isConnected()) return btHelper.send(data);
        if (wifiHelper.isConnected()) return wifiHelper.send(data);
        return false;
    }

    public String getLastSendError() {
        if (activeMode == Mode.USB) return usbHelper.getLastError();
        if (activeMode == Mode.BLUETOOTH) return btHelper.getLastError();
        if (activeMode == Mode.WIFI) return wifiHelper.getLastError();
        return "Not connected";
    }

    // --- Status ---

    public boolean isConnected() {
        return usbHelper.isConnected() || btHelper.isConnected() || wifiHelper.isConnected();
    }

    public BluetoothHelper getBluetoothHelper() { return btHelper; }

    // --- Notification helpers ---

    private void notifyConnected(String info, Mode mode) {
        if (listener != null) listener.onConnected(info, mode);
    }

    private void notifyError(String msg) {
        if (listener != null) listener.onError(msg);
    }
}
