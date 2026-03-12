package com.teamphobot.robotcontroller;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for KobukiPacket.
 * Verifies packet structure: header, length, payload, checksum.
 */
public class KobukiPacketTest {

    @Test
    public void testPacketStartsWithHeader() {
        byte[] packet = KobukiPacket.forward();
        assertEquals((byte) 0xAA, packet[0]);
        assertEquals((byte) 0x55, packet[1]);
    }

    @Test
    public void testPayloadLengthIs6() {
        byte[] packet = KobukiPacket.stop();
        // Byte 2 = payload length = 6 (subID + subLen + 4 data bytes)
        assertEquals(6, packet[2]);
    }

    @Test
    public void testBaseControlSubPayloadId() {
        byte[] packet = KobukiPacket.forward();
        // Byte 3 = sub-payload ID = 0x01 (Base Control)
        assertEquals(0x01, packet[3]);
    }

    @Test
    public void testBaseControlSubPayloadLength() {
        byte[] packet = KobukiPacket.forward();
        // Byte 4 = sub-payload data length = 0x04
        assertEquals(0x04, packet[4]);
    }

    @Test
    public void testTotalPacketLength() {
        // Header(2) + Length(1) + Payload(6) + Checksum(1) = 10
        byte[] packet = KobukiPacket.forward();
        assertEquals(10, packet.length);
    }

    @Test
    public void testStopPacketSpeedIsZero() {
        byte[] packet = KobukiPacket.stop();
        // Speed LSB (byte 5) and MSB (byte 6) should both be 0
        assertEquals(0x00, packet[5]);
        assertEquals(0x00, packet[6]);
    }

    @Test
    public void testStopPacketRadiusIsZero() {
        byte[] packet = KobukiPacket.stop();
        // Radius LSB (byte 7) and MSB (byte 8) should both be 0
        assertEquals(0x00, packet[7]);
        assertEquals(0x00, packet[8]);
    }

    @Test
    public void testForwardSpeedIs100() {
        byte[] packet = KobukiPacket.forward();
        // Speed = 100 = 0x0064, LSB = 0x64, MSB = 0x00
        assertEquals(0x64, packet[5] & 0xFF);
        assertEquals(0x00, packet[6] & 0xFF);
    }

    @Test
    public void testForwardRadiusIsZero() {
        byte[] packet = KobukiPacket.forward();
        // Radius = 0 (straight line)
        assertEquals(0x00, packet[7] & 0xFF);
        assertEquals(0x00, packet[8] & 0xFF);
    }

    @Test
    public void testBackwardSpeedIsNegative100() {
        byte[] packet = KobukiPacket.backward();
        // Speed = -100 = 0xFF9C in signed 16-bit LE
        // LSB = 0x9C, MSB = 0xFF
        assertEquals(0x9C, packet[5] & 0xFF);
        assertEquals(0xFF, packet[6] & 0xFF);
    }

    @Test
    public void testLeftRadiusIs1() {
        byte[] packet = KobukiPacket.turnLeft();
        // Radius = 1 (spin counter-clockwise)
        assertEquals(0x01, packet[7] & 0xFF);
        assertEquals(0x00, packet[8] & 0xFF);
    }

    @Test
    public void testRightRadiusIsNegative1() {
        byte[] packet = KobukiPacket.turnRight();
        // Radius = -1 = 0xFFFF in signed 16-bit LE
        assertEquals(0xFF, packet[7] & 0xFF);
        assertEquals(0xFF, packet[8] & 0xFF);
    }

    @Test
    public void testChecksumIsCorrect() {
        byte[] packet = KobukiPacket.stop();
        // Verify checksum: XOR of all bytes from length through payload
        byte expected = packet[2]; // start with length byte
        for (int i = 3; i < packet.length - 1; i++) {
            expected ^= packet[i];
        }
        assertEquals(expected, packet[packet.length - 1]);
    }

    @Test
    public void testChecksumForForward() {
        byte[] packet = KobukiPacket.forward();
        byte expected = packet[2];
        for (int i = 3; i < packet.length - 1; i++) {
            expected ^= packet[i];
        }
        assertEquals(expected, packet[packet.length - 1]);
    }

    @Test
    public void testHexString() {
        byte[] packet = KobukiPacket.stop();
        String hex = KobukiPacket.toHexString(packet);
        assertNotNull(hex);
        assertTrue(hex.startsWith("AA 55"));
    }
}
