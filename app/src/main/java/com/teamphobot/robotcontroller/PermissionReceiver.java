package com.teamphobot.robotcontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

/**
 * PermissionReceiver listens for the result of a USB permission request.
 * When the user taps Allow or Deny on the USB permission dialog,
 * this receiver fires and tells ConnectionManager what happened.
 */
public class PermissionReceiver extends BroadcastReceiver {

    private static final String TAG = "PermissionReceiver";

    public static final String ACTION_USB_PERMISSION =
            "com.teamphobot.robotcontroller.USB_PERMISSION";

    public interface PermissionResultListener {
        void onPermissionGranted(UsbDevice device);
        void onPermissionDenied(UsbDevice device);
    }

    private final PermissionResultListener listener;

    public PermissionReceiver(PermissionResultListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_USB_PERMISSION.equals(intent.getAction())) {
            return;
        }

        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        boolean granted = intent.getBooleanExtra(
                UsbManager.EXTRA_PERMISSION_GRANTED, false);

        Log.d(TAG, "Permission result: granted=" + granted);

        if (device == null) return;

        if (granted) {
            listener.onPermissionGranted(device);
        } else {
            listener.onPermissionDenied(device);
        }
    }
}
