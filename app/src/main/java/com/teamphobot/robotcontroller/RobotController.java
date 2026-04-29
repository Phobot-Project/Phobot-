package com.teamphobot.robotcontroller;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * RobotController provides high-level movement commands.
 *
 * It sends Kobuki packets through ConnectionManager.send(), which forwards
 * them via USB, Bluetooth, or WiFi depending on which transport is active.
 *
 * Commands are re-sent every 250ms because the Kobuki has a ~600ms
 * watchdog timer that stops the robot if no command arrives.
 */
public class RobotController {

    private static final String TAG = "RobotController";
    private static final int REPEAT_INTERVAL_MS = 250;

    public interface CommandResultListener {
        void onCommandSent(String command);
        void onCommandFailed(String command, String reason);
    }

    private final ConnectionManager connectionManager;
    private CommandResultListener resultListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String activeCommand = null;
    private byte[] activePacket = null;

    public RobotController(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void setCommandResultListener(CommandResultListener listener) {
        this.resultListener = listener;
    }

    public void forward()  { startCommand(CommandEncoder.CMD_FORWARD); }
    public void backward() { startCommand(CommandEncoder.CMD_BACKWARD); }
    public void left()     { startCommand(CommandEncoder.CMD_LEFT); }
    public void right()    { startCommand(CommandEncoder.CMD_RIGHT); }

    public void stop() {
        cancelRepeat();
        activeCommand = null;
        activePacket = null;
        sendOnce(CommandEncoder.CMD_STOP);
    }

    private void startCommand(String command) {
        cancelRepeat();
        byte[] packet = CommandEncoder.encode(command);
        if (packet == null) { reportFailed(command, "Unknown command"); return; }
        if (!sendPacket(command, packet)) return;
        activeCommand = command;
        activePacket = packet;
        scheduleRepeat();
    }

    private void sendOnce(String command) {
        byte[] packet = CommandEncoder.encode(command);
        if (packet == null) { reportFailed(command, "Unknown command"); return; }
        sendPacket(command, packet);
    }

    private boolean sendPacket(String command, byte[] packet) {
        if (!connectionManager.isConnected()) {
            reportFailed(command, "Robot is not connected");
            return false;
        }
        boolean sent = connectionManager.send(packet);
        if (sent) {
            Log.d(TAG, "Sent: " + command);
            if (resultListener != null) resultListener.onCommandSent(command);
            return true;
        } else {
            reportFailed(command, connectionManager.getLastSendError());
            return false;
        }
    }

    private final Runnable repeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (activeCommand != null && activePacket != null
                    && connectionManager.isConnected()) {
                boolean sent = connectionManager.send(activePacket);
                if (!sent) { cancelRepeat(); return; }
                handler.postDelayed(this, REPEAT_INTERVAL_MS);
            }
        }
    };

    private void scheduleRepeat() { handler.postDelayed(repeatRunnable, REPEAT_INTERVAL_MS); }
    private void cancelRepeat()   { handler.removeCallbacks(repeatRunnable); }

    private void reportFailed(String command, String reason) {
        Log.e(TAG, "Failed: " + command + " - " + reason);
        if (resultListener != null) resultListener.onCommandFailed(command, reason);
    }

    public void shutdown() {
        cancelRepeat();
        activeCommand = null;
        activePacket = null;
    }
}
