package com.teamphobot.robotcontroller;

/**
 * CommandEncoder converts command names into Kobuki binary packets.
 */
public class CommandEncoder {

    public static final String CMD_FORWARD  = "FORWARD";
    public static final String CMD_BACKWARD = "BACKWARD";
    public static final String CMD_LEFT     = "LEFT";
    public static final String CMD_RIGHT    = "RIGHT";
    public static final String CMD_STOP     = "STOP";

    public static byte[] encode(String command) {
        if (command == null) return null;
        switch (command) {
            case CMD_FORWARD:  return KobukiProtocol.forward();
            case CMD_BACKWARD: return KobukiProtocol.backward();
            case CMD_LEFT:     return KobukiProtocol.left();
            case CMD_RIGHT:    return KobukiProtocol.right();
            case CMD_STOP:     return KobukiProtocol.stop();
            default:           return null;
        }
    }

    public static String label(String command) {
        if (command == null) return "UNKNOWN";
        switch (command) {
            case CMD_FORWARD:  return "FWD";
            case CMD_BACKWARD: return "BWD";
            case CMD_LEFT:     return "LFT";
            case CMD_RIGHT:    return "RGT";
            case CMD_STOP:     return "STP";
            default:           return "???";
        }
    }
}
