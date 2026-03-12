package com.teamphobot.robotcontroller;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.HashMap;

/**
 * ConnectionManager handles the full USB connection lifecycle for the Kobuki robot.
 *
 * Flow:
 *   1. Scan for USB devices
 *   2. Find the Kobuki's FTDI chip (or first available device)
 *   3. Request USB permission from the user
 *   4. Open and initialize the FTDI chip via FtdiDevice
 *   5. Report connection status back to MainActivity
 */
public class ConnectionManager implements PermissionReceiver.PermissionResultListener {

    private static final String TAG = "ConnectionManager";

    public interface ConnectionListener {
        void onConnected(String deviceName, int vendorId, int productId);
        void onDisconnected(String reason);
        void onError(String errorMessage);
    }

    private final Context context;
    private final UsbManager usbManager;
    private final FtdiDevice ftdiDevice;
    private final PermissionReceiver permissionReceiver;
    private ConnectionListener listener;
    private boolean receiverRegistered = false;

    public ConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
        this.ftdiDevice = new FtdiDevice(usbManager);
        this.permissionReceiver = new PermissionReceiver(this);
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.listener = listener;
    }

    /** Register the USB permission receiver. Call in onCreate(). */
    public void registerReceiver() {
        if (receiverRegistered) return;

        IntentFilter filter = new IntentFilter(PermissionReceiver.ACTION_USB_PERMISSION);
        ContextCompat.registerReceiver(
                context, permissionReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);

        receiverRegistered = true;
    }

    /** Unregister the receiver. Call in onDestroy(). */
    public void unregisterReceiver() {
        if (!receiverRegistered) return;
        try {
            context.unregisterReceiver(permissionReceiver);
        } catch (Exception ignored) {
        }
        receiverRegistered = false;
    }

    /**
     * Scans for USB devices and connects to the Kobuki.
     * Prefers FTDI devices (Kobuki's USB chip).
     */
    public void connect() {
        UsbDevice device = findKobukiDevice();

        if (device == null) {
            notifyError("No USB device found. Check the cable and OTG adapter.");
            return;
        }

        Log.d(TAG, "Found device: " + device.getDeviceName()
                + " VID=0x" + Integer.toHexString(device.getVendorId())
                + " PID=0x" + Integer.toHexString(device.getProductId()));

        if (usbManager.hasPermission(device)) {
            openDevice(device);
        } else {
            requestPermission(device);
        }
    }

    /** Disconnect from the robot. */
    public void disconnect() {
        ftdiDevice.close();
        notifyDisconnected("User disconnected");
    }

    /**
     * Finds the Kobuki's USB device.
     * First looks for FTDI devices (VID 0x0403).
     * If none found, tries any available USB device.
     */
    private UsbDevice findKobukiDevice() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        if (deviceList.isEmpty()) {
            Log.d(TAG, "No USB devices attached");
            return null;
        }

        // First pass: look for FTDI device (Kobuki uses FTDI FT232R)
        for (UsbDevice device : deviceList.values()) {
            if (FtdiDevice.isFtdiDevice(device)) {
                Log.d(TAG, "Found FTDI device (Kobuki): " + device.getDeviceName());
                return device;
            }
        }

        // Second pass: look for any USB-serial chip
        // CH340 VID=0x1A86, CP2102 VID=0x10C4
        for (UsbDevice device : deviceList.values()) {
            int vid = device.getVendorId();
            if (vid == 0x1A86 || vid == 0x10C4) {
                Log.d(TAG, "Found USB-serial device: " + device.getDeviceName());
                return device;
            }
        }

        // Last resort: return first device
        UsbDevice first = deviceList.values().iterator().next();
        Log.d(TAG, "Using first available device: " + first.getDeviceName());
        return first;
    }

    private void requestPermission(UsbDevice device) {
        PendingIntent pi = PendingIntent.getBroadcast(
                context, 0,
                new Intent(PermissionReceiver.ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE);

        usbManager.requestPermission(device, pi);
        Log.d(TAG, "Requested USB permission");
    }

    private void openDevice(UsbDevice device) {
        boolean success = ftdiDevice.open(device);

        if (success) {
            notifyConnected(device);
        } else {
            notifyError("Connection failed: " + ftdiDevice.getLastError());
        }
    }

    // --- PermissionReceiver callbacks ---

    @Override
    public void onPermissionGranted(UsbDevice device) {
        Log.d(TAG, "Permission granted");
        openDevice(device);
    }

    @Override
    public void onPermissionDenied(UsbDevice device) {
        notifyError("USB permission denied. Please allow access.");
    }

    // --- Notification helpers ---

    private void notifyConnected(UsbDevice device) {
        if (listener != null) {
            String name = device.getProductName();
            if (name == null || name.isEmpty()) name = device.getDeviceName();
            listener.onConnected(name, device.getVendorId(), device.getProductId());
        }
    }

    private void notifyDisconnected(String reason) {
        if (listener != null) listener.onDisconnected(reason);
    }

    private void notifyError(String message) {
        if (listener != null) listener.onError(message);
    }

    // --- Public accessors ---

    /** Returns the FtdiDevice so RobotController can send packets. */
    public FtdiDevice getFtdiDevice() {
        return ftdiDevice;
    }

    /** Returns true if USB is connected and FTDI is initialized. */
    public boolean isConnected() {
        return ftdiDevice.isConnected();
    }
}
