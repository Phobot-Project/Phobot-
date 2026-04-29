package com.teamphobot.robotcontroller;

/**
 * KobukiProtocol builds the binary packets that the Kobuki robot expects.
 *
 * Packet format: [0xAA] [0x55] [Length] [Payload...] [Checksum]
 *
 * Base Control sub-payload:
 *   [0x01] [0x04] [speed_lo] [speed_hi] [radius_lo] [radius_hi]
 *
 * Speed: signed 16-bit, mm/s. Radius: 0=straight, 1=spin in place.
 */
public class KobukiProtocol {

    private static final byte HEADER_0 = (byte) 0xAA;
    private static final byte HEADER_1 = (byte) 0x55;
    private static final byte BASE_CONTROL_ID = 0x01;
    private static final byte BASE_CONTROL_LEN = 0x04;
    private static final int DRIVE_SPEED = 200;
    private static final int SPIN_SPEED = 100;

    public static byte[] forward() {
        return buildBaseControlPacket(DRIVE_SPEED, 0);
    }

    public static byte[] backward() {
        return buildBaseControlPacket(-DRIVE_SPEED, 0);
    }

    public static byte[] left() {
        return buildBaseControlPacket(SPIN_SPEED, 1);
    }

    public static byte[] right() {
        return buildBaseControlPacket(-SPIN_SPEED, 1);
    }

    public static byte[] stop() {
        return buildBaseControlPacket(0, 0);
    }

    public static byte[] buildBaseControlPacket(int speedMmPerSec, int radiusMm) {
        byte speedLo = (byte) (speedMmPerSec & 0xFF);
        byte speedHi = (byte) ((speedMmPerSec >> 8) & 0xFF);
        byte radiusLo = (byte) (radiusMm & 0xFF);
        byte radiusHi = (byte) ((radiusMm >> 8) & 0xFF);

        byte[] payload = new byte[] {
            BASE_CONTROL_ID, BASE_CONTROL_LEN,
            speedLo, speedHi, radiusLo, radiusHi
        };

        byte length = (byte) payload.length;
        byte checksum = length;
        for (byte b : payload) { checksum ^= b; }

        byte[] packet = new byte[3 + payload.length + 1];
        packet[0] = HEADER_0;
        packet[1] = HEADER_1;
        packet[2] = length;
        System.arraycopy(payload, 0, packet, 3, payload.length);
        packet[packet.length - 1] = checksum;
        return packet;
    }

    public static String packetToHex(byte[] packet) {
        if (packet == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < packet.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", packet[i] & 0xFF));
        }
        return sb.toString();
    }
}
