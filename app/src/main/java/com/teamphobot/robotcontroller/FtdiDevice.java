package com.teamphobot.robotcontroller;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

/**
 * FtdiDevice handles the FTDI FT232R USB-to-serial chip that sits inside
 * the Kobuki robot base.
 *
 * The Kobuki connects to the phone via USB. Inside the Kobuki, an FTDI
 * FT232R chip converts USB signals to serial (UART) signals. Before we
 * send any data, we must configure this chip using USB control transfers:
 *
 *   1. Reset the chip
 *   2. Set baud rate to 115200 (what Kobuki expects)
 *   3. Set data format to 8 data bits, no parity, 1 stop bit (8N1)
 *   4. Set flow control to none
 *
 * After these control transfers succeed, bulk transfers will work and
 * we can send Kobuki protocol packets.
 *
 * FTDI VID = 0x0403 (decimal 1027)
 * FTDI FT232R PID = 0x6001 (decimal 24577)
 *
 * Without this initialization, bulkTransfer() returns -1 every time.
 * That was the bug in the previous version of this app.
 */
public class FtdiDevice {

    private static final String TAG = "FtdiDevice";

    // FTDI vendor and product IDs
    public static final int FTDI_VID = 0x0403;
    public static final int FTDI_PID_FT232R = 0x6001;

    // FTDI control transfer request types
    private static final int FTDI_REQTYPE_OUT = 0x40;  // host-to-device, vendor, device

    // FTDI request codes
    private static final int SIO_RESET         = 0;
    private static final int SIO_SET_MODEM     = 1;
    private static final int SIO_SET_FLOW_CTRL = 2;
    private static final int SIO_SET_BAUD_RATE = 3;
    private static final int SIO_SET_DATA      = 4;

    // Reset values
    private static final int SIO_RESET_SIO     = 0;
    private static final int SIO_RESET_PURGE_RX = 1;
    private static final int SIO_RESET_PURGE_TX = 2;

    // Timeout for control transfers (ms)
    private static final int CTRL_TIMEOUT = 2000;

    // Timeout for bulk transfers (ms)
    private static final int BULK_TIMEOUT = 200;

    private final UsbManager usbManager;

    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbInterface usbInterface;
    private UsbEndpoint outEndpoint;
    private UsbEndpoint inEndpoint;

    private String lastError = "";

    public FtdiDevice(UsbManager usbManager) {
        this.usbManager = usbManager;
    }

    /**
     * Opens and initializes the FTDI device.
     *
     * Steps:
     *   1. Open the USB device connection
     *   2. Claim the first interface
     *   3. Find BULK OUT and BULK IN endpoints
     *   4. Reset the FTDI chip
     *   5. Set baud rate to 115200
     *   6. Set data format to 8N1
     *   7. Set flow control to none
     *   8. Purge RX and TX buffers
     *
     * @param targetDevice The USB device to open (should be FTDI)
     * @return true if everything succeeded, false if any step failed
     */
    public boolean open(UsbDevice targetDevice) {
        close();

        if (targetDevice == null) {
            lastError = "Device is null";
            return false;
        }

        this.device = targetDevice;

        Log.d(TAG, "Opening device: " + device.getDeviceName()
                + " VID=0x" + Integer.toHexString(device.getVendorId())
                + " PID=0x" + Integer.toHexString(device.getProductId()));

        // Step 1: get interface
        if (device.getInterfaceCount() == 0) {
            lastError = "Device has no interfaces";
            return false;
        }

        usbInterface = device.getInterface(0);

        // Step 2: find endpoints
        findEndpoints();

        if (outEndpoint == null) {
            lastError = "No OUT endpoint found";
            return false;
        }

        // Step 3: open connection
        connection = usbManager.openDevice(device);
        if (connection == null) {
            lastError = "Failed to open device";
            return false;
        }

        // Step 4: claim interface
        if (!connection.claimInterface(usbInterface, true)) {
            lastError = "Failed to claim interface";
            connection.close();
            connection = null;
            return false;
        }

        // Step 5: initialize FTDI chip
        if (!initFtdi()) {
            lastError = "FTDI initialization failed: " + lastError;
            connection.releaseInterface(usbInterface);
            connection.close();
            connection = null;
            return false;
        }

        Log.d(TAG, "FTDI device opened and initialized at 115200 baud, 8N1");
        lastError = "";
        return true;
    }

    /**
     * Finds BULK OUT and BULK IN endpoints on the interface.
     */
    private void findEndpoints() {
        outEndpoint = null;
        inEndpoint = null;

        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            UsbEndpoint ep = usbInterface.getEndpoint(i);

            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT && outEndpoint == null) {
                    outEndpoint = ep;
                    Log.d(TAG, "OUT endpoint: " + ep.getEndpointNumber()
                            + " address=0x" + Integer.toHexString(ep.getAddress()));
                }
                if (ep.getDirection() == UsbConstants.USB_DIR_IN && inEndpoint == null) {
                    inEndpoint = ep;
                    Log.d(TAG, "IN endpoint: " + ep.getEndpointNumber()
                            + " address=0x" + Integer.toHexString(ep.getAddress()));
                }
            }
        }
    }

    /**
     * Sends FTDI control transfers to configure the chip.
     * This is the critical step that was missing before.
     */
    private boolean initFtdi() {

        // 1. Reset the FTDI chip
        if (!controlOut(SIO_RESET, SIO_RESET_SIO, 0)) {
            lastError = "Reset failed";
            return false;
        }

        // 2. Set baud rate to 115200
        //    FTDI FT232R base clock = 3,000,000 Hz
        //    Divisor = 3000000 / 115200 = 26 (0x001A)
        //    wValue = divisor = 0x001A
        //    wIndex = 0x0000 (for FT232R, no high bits needed)
        if (!controlOut(SIO_SET_BAUD_RATE, 0x001A, 0x0000)) {
            lastError = "Set baud rate failed";
            return false;
        }

        // 3. Set data format: 8 data bits, no parity, 1 stop bit
        //    wValue = data bits | (parity << 8) | (stop bits << 11)
        //    8 data bits = 8
        //    No parity = 0
        //    1 stop bit = 0
        //    wValue = 0x0008
        if (!controlOut(SIO_SET_DATA, 0x0008, 0x0000)) {
            lastError = "Set data format failed";
            return false;
        }

        // 4. Set flow control: none
        //    wValue = 0, wIndex = 0
        if (!controlOut(SIO_SET_FLOW_CTRL, 0x0000, 0x0000)) {
            lastError = "Set flow control failed";
            return false;
        }

        // 5. Purge RX buffer
        if (!controlOut(SIO_RESET, SIO_RESET_PURGE_RX, 0)) {
            lastError = "Purge RX failed";
            return false;
        }

        // 6. Purge TX buffer
        if (!controlOut(SIO_RESET, SIO_RESET_PURGE_TX, 0)) {
            lastError = "Purge TX failed";
            return false;
        }

        // 7. Set DTR and RTS high (some devices need this)
        //    DTR = 0x0101, RTS = 0x0202
        controlOut(SIO_SET_MODEM, 0x0101, 0);
        controlOut(SIO_SET_MODEM, 0x0202, 0);

        Log.d(TAG, "FTDI init complete: 115200, 8N1, no flow control");
        return true;
    }

    /**
     * Sends a vendor control transfer to the FTDI chip.
     */
    private boolean controlOut(int request, int value, int index) {
        int result = connection.controlTransfer(
                FTDI_REQTYPE_OUT,
                request,
                value,
                index,
                null,
                0,
                CTRL_TIMEOUT
        );

        if (result < 0) {
            Log.e(TAG, "controlTransfer failed: request=" + request
                    + " value=0x" + Integer.toHexString(value)
                    + " result=" + result);
            return false;
        }

        return true;
    }

    /**
     * Sends raw bytes over USB bulk transfer.
     * The FTDI chip must be initialized first via open().
     *
     * @param data The bytes to send
     * @return true if transfer succeeded
     */
    public boolean send(byte[] data) {
        if (connection == null || outEndpoint == null) {
            lastError = "Not connected";
            return false;
        }

        if (data == null || data.length == 0) {
            lastError = "No data to send";
            return false;
        }

        int result = connection.bulkTransfer(
                outEndpoint, data, data.length, BULK_TIMEOUT);

        if (result < 0) {
            lastError = "Bulk transfer failed (result=" + result + ")";
            Log.e(TAG, lastError + " data.length=" + data.length);
            return false;
        }

        Log.d(TAG, "Sent " + result + " bytes");
        lastError = "";
        return true;
    }

    /**
     * Closes the connection and releases all resources.
     */
    public void close() {
        if (connection != null) {
            if (usbInterface != null) {
                connection.releaseInterface(usbInterface);
            }
            connection.close();
            Log.d(TAG, "FTDI device closed");
        }

        connection = null;
        usbInterface = null;
        outEndpoint = null;
        inEndpoint = null;
        device = null;
        lastError = "";
    }

    public boolean isConnected() {
        return connection != null && outEndpoint != null;
    }

    public UsbDevice getDevice() {
        return device;
    }

    public int getVendorId() {
        return device != null ? device.getVendorId() : -1;
    }

    public int getProductId() {
        return device != null ? device.getProductId() : -1;
    }

    public String getDeviceName() {
        if (device == null) return "None";
        String name = device.getProductName();
        if (name != null && !name.isEmpty()) return name;
        return device.getDeviceName();
    }

    public String getLastError() {
        return lastError;
    }

    /**
     * Checks if a USB device looks like an FTDI chip.
     * Returns true for known FTDI vendor/product IDs.
     */
    public static boolean isFtdiDevice(UsbDevice device) {
        if (device == null) return false;
        int vid = device.getVendorId();
        int pid = device.getProductId();

        // FTDI FT232R
        if (vid == 0x0403 && pid == 0x6001) return true;
        // FTDI FT232H
        if (vid == 0x0403 && pid == 0x6014) return true;
        // FTDI FT2232
        if (vid == 0x0403 && pid == 0x6010) return true;

        return false;
    }
}
