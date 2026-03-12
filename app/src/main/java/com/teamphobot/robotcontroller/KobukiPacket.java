package com.teamphobot.robotcontroller;

/**
 * KobukiPacket builds binary command packets for the Kobuki robot base.
 *
 * The Kobuki uses a specific serial protocol. Every packet looks like this:
 *
 *   [0xAA] [0x55] [Length] [Payload...] [Checksum]
 *
 *   0xAA 0x55  = header (always these two bytes)
 *   Length     = number of bytes in the payload (not counting header or checksum)
 *   Payload   = one or more sub-payloads
 *   Checksum  = XOR of all bytes from Length through the end of payload
 *
 * For movement control, the sub-payload is:
 *
 *   [0x01] [0x04] [SpeedLSB] [SpeedMSB] [RadiusLSB] [RadiusMSB]
 *
 *   0x01 = sub-payload ID for "Base Control"
 *   0x04 = sub-payload data length (4 bytes follow)
 *   Speed = signed 16-bit, little-endian, in mm/s
 *   Radius = signed 16-bit, little-endian, in mm
 *
 * Special radius values:
 *   0      = drive straight
 *   1      = spin in place counter-clockwise (turn left)
 *   -1     = spin in place clockwise (turn right)
 *
 * The Kobuki communicates at 115200 baud, 8N1, over its FTDI USB-serial chip.
 */
public class KobukiPacket {

    // Packet header bytes
    private static final byte HEADER_0 = (byte) 0xAA;
    private static final byte HEADER_1 = (byte) 0x55;

    // Sub-payload ID for base control
    private static final byte BASE_CONTROL_ID = 0x01;

    // Sub-payload data length for base control (speed 2 bytes + radius 2 bytes)
    private static final byte BASE_CONTROL_LEN = 0x04;

    // Movement speed in mm/s (100 mm/s is a safe speed for testing)
    private static final int DEFAULT_SPEED = 100;

    /**
     * Builds a Kobuki base control packet.
     *
     * @param speedMmPerSec Speed in mm/s. Positive = forward, negative = backward.
     * @param radiusMm      Radius in mm. 0 = straight, 1 = spin left, -1 = spin right.
     * @return Complete packet bytes ready to send over USB
     */
    public static byte[] buildBaseControl(int speedMmPerSec, int radiusMm) {
        // Payload: [subID] [subLen] [speedLSB] [speedMSB] [radiusLSB] [radiusMSB]
        byte[] payload = new byte[6];
        payload[0] = BASE_CONTROL_ID;
        payload[1] = BASE_CONTROL_LEN;
        payload[2] = (byte) (speedMmPerSec & 0xFF);         // speed LSB
        payload[3] = (byte) ((speedMmPerSec >> 8) & 0xFF);  // speed MSB
        payload[4] = (byte) (radiusMm & 0xFF);              // radius LSB
        payload[5] = (byte) ((radiusMm >> 8) & 0xFF);       // radius MSB

        // Total payload length
        byte payloadLength = (byte) payload.length;

        // Compute checksum: XOR of payloadLength and all payload bytes
        byte checksum = payloadLength;
        for (byte b : payload) {
            checksum ^= b;
        }

        // Build complete packet: [AA] [55] [len] [payload...] [checksum]
        byte[] packet = new byte[3 + payload.length + 1];
        packet[0] = HEADER_0;
        packet[1] = HEADER_1;
        packet[2] = payloadLength;
        System.arraycopy(payload, 0, packet, 3, payload.length);
        packet[packet.length - 1] = checksum;

        return packet;
    }

    // --- Convenience methods for each movement command ---

    /** Forward at default speed, straight line. */
    public static byte[] forward() {
        return buildBaseControl(DEFAULT_SPEED, 0);
    }

    /** Backward at default speed, straight line. */
    public static byte[] backward() {
        return buildBaseControl(-DEFAULT_SPEED, 0);
    }

    /** Spin in place to the left (counter-clockwise). */
    public static byte[] turnLeft() {
        return buildBaseControl(DEFAULT_SPEED, 1);
    }

    /** Spin in place to the right (clockwise). */
    public static byte[] turnRight() {
        return buildBaseControl(DEFAULT_SPEED, -1);
    }

    /** Stop all movement. Speed = 0, radius = 0. */
    public static byte[] stop() {
        return buildBaseControl(0, 0);
    }

    /**
     * Returns a hex string of the packet bytes for logging.
     * Example output: "AA 55 06 01 04 64 00 00 00 63"
     */
    public static String toHexString(byte[] packet) {
        if (packet == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < packet.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02X", packet[i] & 0xFF));
        }
        return sb.toString();
    }
}
