package com.teamphobot.robotcontroller;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

/**
 * UsbHelper handles USB serial communication with the Kobuki robot.
 * Initializes FTDI/CP2102/CH340 chips before sending data.
 */
public class UsbHelper {

    private static final String TAG = "UsbHelper";
    private static final int TIMEOUT_MS = 500;
    private static final int FTDI_REQUEST_TYPE = 0x40;

    private final UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbInterface usbInterface;
    private UsbEndpoint outEndpoint;
    private UsbEndpoint inEndpoint;
    private String lastError = "";
    private int chipType = 0;

    public UsbHelper(UsbManager usbManager) {
        this.usbManager = usbManager;
    }

    public boolean openDevice(UsbDevice targetDevice) {
        close();
        if (targetDevice == null) { lastError = "Device is null"; return false; }
        this.device = targetDevice;
        identifyChip();

        if (device.getInterfaceCount() == 0) { lastError = "No USB interfaces"; return false; }
        usbInterface = findBestInterface(device);
        if (usbInterface == null) { lastError = "No suitable interface"; return false; }
        findEndpoints(usbInterface);
        if (outEndpoint == null) { lastError = "No OUT endpoint"; return false; }

        connection = usbManager.openDevice(device);
        if (connection == null) { lastError = "Failed to open device"; return false; }

        boolean claimed = connection.claimInterface(usbInterface, true);
        if (!claimed) { lastError = "Failed to claim interface"; connection.close(); connection = null; return false; }

        initializeSerialChip();
        lastError = "";
        Log.d(TAG, "Opened: " + device.getDeviceName() + " chip=" + chipName());
        return true;
    }

    private void identifyChip() {
        int vid = device.getVendorId();
        if (vid == 0x0403) chipType = 1;      // FTDI
        else if (vid == 0x10C4) chipType = 2;  // CP2102
        else if (vid == 0x1A86) chipType = 3;  // CH340
        else chipType = 0;
    }

    public String chipName() {
        switch (chipType) {
            case 1: return "FTDI";
            case 2: return "CP2102";
            case 3: return "CH340";
            default: return "GENERIC";
        }
    }

    private void initializeSerialChip() {
        switch (chipType) {
            case 1: initFTDI(); break;
            case 2: initCP2102(); break;
            case 3: initCH340(); break;
            default: initFTDI(); break;
        }
    }

    private void initFTDI() {
        Log.d(TAG, "Init FTDI 115200 8N1");
        connection.controlTransfer(FTDI_REQUEST_TYPE, 0, 0, 0, null, 0, TIMEOUT_MS);
        connection.controlTransfer(FTDI_REQUEST_TYPE, 3, 0x001A, 0, null, 0, TIMEOUT_MS);
        connection.controlTransfer(FTDI_REQUEST_TYPE, 4, 0x0008, 0, null, 0, TIMEOUT_MS);
        connection.controlTransfer(FTDI_REQUEST_TYPE, 2, 0, 0, null, 0, TIMEOUT_MS);
        connection.controlTransfer(FTDI_REQUEST_TYPE, 1, 0x0303, 0, null, 0, TIMEOUT_MS);
    }

    private void initCP2102() {
        Log.d(TAG, "Init CP2102 115200 8N1");
        connection.controlTransfer(0x41, 0x00, 0x0001, 0, null, 0, TIMEOUT_MS);
        byte[] baud = {(byte)0x00, (byte)0xC2, (byte)0x01, (byte)0x00};
        connection.controlTransfer(0x41, 0x1E, 0, 0, baud, baud.length, TIMEOUT_MS);
        connection.controlTransfer(0x41, 0x03, 0x0800, 0, null, 0, TIMEOUT_MS);
    }

    private void initCH340() {
        Log.d(TAG, "Init CH340 115200 8N1");
        connection.controlTransfer(0x40, 0xA1, 0, 0, null, 0, TIMEOUT_MS);
        connection.controlTransfer(0x40, 0x9A, 0x1312, 0x00F3, null, 0, TIMEOUT_MS);
        connection.controlTransfer(0x40, 0x9A, 0x0F2C, 0x0007, null, 0, TIMEOUT_MS);
        connection.controlTransfer(0x40, 0x9A, 0x2518, 0x00C3, null, 0, TIMEOUT_MS);
        connection.controlTransfer(0x40, 0xA4, 0x00FF, 0, null, 0, TIMEOUT_MS);
    }

    private UsbInterface findBestInterface(UsbDevice dev) {
        UsbInterface fallback = null;
        for (int i = 0; i < dev.getInterfaceCount(); i++) {
            UsbInterface iface = dev.getInterface(i);
            int cls = iface.getInterfaceClass();
            if (cls == UsbConstants.USB_CLASS_VENDOR_SPEC) return iface;
            if (cls == UsbConstants.USB_CLASS_CDC_DATA) return iface;
            if (fallback == null) fallback = iface;
        }
        return fallback;
    }

    private void findEndpoints(UsbInterface iface) {
        outEndpoint = null; inEndpoint = null;
        for (int i = 0; i < iface.getEndpointCount(); i++) {
            UsbEndpoint ep = iface.getEndpoint(i);
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT && outEndpoint == null) outEndpoint = ep;
                if (ep.getDirection() == UsbConstants.USB_DIR_IN && inEndpoint == null) inEndpoint = ep;
            }
        }
        if (outEndpoint == null) {
            for (int i = 0; i < iface.getEndpointCount(); i++) {
                UsbEndpoint ep = iface.getEndpoint(i);
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) { outEndpoint = ep; break; }
            }
        }
    }

    public boolean send(byte[] data) {
        if (connection == null) { lastError = "Not connected"; return false; }
        if (outEndpoint == null) { lastError = "No OUT endpoint"; return false; }
        if (data == null || data.length == 0) { lastError = "No data"; return false; }
        int result = connection.bulkTransfer(outEndpoint, data, data.length, TIMEOUT_MS);
        if (result < 0) { lastError = "Bulk transfer failed (result=" + result + ")"; return false; }
        lastError = "";
        return true;
    }

    public void close() {
        if (connection != null) {
            if (usbInterface != null) connection.releaseInterface(usbInterface);
            connection.close();
        }
        connection = null; usbInterface = null; outEndpoint = null; inEndpoint = null; device = null; chipType = 0; lastError = "";
    }

    public boolean isConnected() { return connection != null && outEndpoint != null; }
    public String getDeviceName() {
        if (device == null) return "None";
        String n = device.getProductName();
        return (n != null && !n.isEmpty()) ? n : device.getDeviceName();
    }
    public int getVendorId() { return device != null ? device.getVendorId() : -1; }
    public int getProductId() { return device != null ? device.getProductId() : -1; }
    public String getLastError() { return lastError; }
}
