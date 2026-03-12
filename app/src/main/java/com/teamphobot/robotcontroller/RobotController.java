package com.teamphobot.robotcontroller;

import android.util.Log;

/**
 * RobotController provides high-level robot movement commands for the Kobuki.
 *
 * When you call forward(), this class:
 *   1. Checks if the robot is connected
 *   2. Builds a Kobuki protocol packet via KobukiPacket
 *   3. Sends the packet bytes via FtdiDevice
 *   4. Reports success or failure back to MainActivity
 *
 * The Kobuki packet for forward() looks like:
 *   AA 55 06 01 04 64 00 00 00 [checksum]
 *   (speed=100mm/s, radius=0 = straight line)
 */
public class RobotController {

    private static final String TAG = "RobotController";

    public static final String CMD_FORWARD  = "FORWARD";
    public static final String CMD_BACKWARD = "BACKWARD";
    public static final String CMD_LEFT     = "LEFT";
    public static final String CMD_RIGHT    = "RIGHT";
    public static final String CMD_STOP     = "STOP";

    public interface CommandResultListener {
        void onCommandSent(String command, String hexData);
        void onCommandFailed(String command, String reason);
    }

    private final ConnectionManager connectionManager;
    private CommandResultListener resultListener;

    public RobotController(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void setCommandResultListener(CommandResultListener listener) {
        this.resultListener = listener;
    }

    public void forward() {
        sendKobukiCommand(CMD_FORWARD, KobukiPacket.forward());
    }

    public void backward() {
        sendKobukiCommand(CMD_BACKWARD, KobukiPacket.backward());
    }

    public void left() {
        sendKobukiCommand(CMD_LEFT, KobukiPacket.turnLeft());
    }

    public void right() {
        sendKobukiCommand(CMD_RIGHT, KobukiPacket.turnRight());
    }

    public void stop() {
        sendKobukiCommand(CMD_STOP, KobukiPacket.stop());
    }

    /**
     * Sends a Kobuki protocol packet to the robot.
     *
     * @param commandName Human-readable name for logging (e.g., "FORWARD")
     * @param packet      The raw Kobuki packet bytes from KobukiPacket
     */
    private void sendKobukiCommand(String commandName, byte[] packet) {
        Log.d(TAG, "sendKobukiCommand: " + commandName
                + " packet=" + KobukiPacket.toHexString(packet));

        // Check connection
        if (!connectionManager.isConnected()) {
            reportFailed(commandName, "Robot is not connected");
            return;
        }

        if (packet == null) {
            reportFailed(commandName, "Failed to build packet");
            return;
        }

        // Send via FTDI
        FtdiDevice ftdi = connectionManager.getFtdiDevice();
        boolean sent = ftdi.send(packet);

        if (sent) {
            String hex = KobukiPacket.toHexString(packet);
            Log.d(TAG, "Command sent: " + commandName + " [" + hex + "]");
            if (resultListener != null) {
                resultListener.onCommandSent(commandName, hex);
            }
        } else {
            reportFailed(commandName, ftdi.getLastError());
        }
    }

    private void reportFailed(String command, String reason) {
        Log.e(TAG, "Command failed: " + command + " - " + reason);
        if (resultListener != null) {
            resultListener.onCommandFailed(command, reason);
        }
    }
}
