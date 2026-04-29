package com.teamphobot.robotcontroller;

import org.junit.Test;
import static org.junit.Assert.*;

public class KobukiProtocolTest {

    @Test
    public void testStopPacket() {
        byte[] pkt = KobukiProtocol.stop();
        assertEquals((byte) 0xAA, pkt[0]);
        assertEquals((byte) 0x55, pkt[1]);
        assertEquals(6, pkt[2]);
        assertEquals(0x01, pkt[3]);
        assertEquals(0x04, pkt[4]);
        assertEquals(0x00, pkt[5]); // speed lo
        assertEquals(0x00, pkt[6]); // speed hi
        assertEquals(0x00, pkt[7]); // radius lo
        assertEquals(0x00, pkt[8]); // radius hi
        assertEquals(0x03, pkt[9]); // checksum
    }

    @Test
    public void testForwardPacket() {
        byte[] pkt = KobukiProtocol.forward();
        assertEquals((byte) 0xC8, pkt[5]); // 200 lo
        assertEquals((byte) 0x00, pkt[6]); // 200 hi
        assertEquals((byte) 0x00, pkt[7]); // radius 0
    }

    @Test
    public void testBackwardPacket() {
        byte[] pkt = KobukiProtocol.backward();
        assertEquals((byte) 0x38, pkt[5]); // -200 lo
        assertEquals((byte) 0xFF, pkt[6]); // -200 hi
    }

    @Test
    public void testLeftPacket() {
        byte[] pkt = KobukiProtocol.left();
        assertEquals((byte) 0x64, pkt[5]); // 100 lo
        assertEquals((byte) 0x01, pkt[7]); // radius 1 = spin
    }

    @Test
    public void testRightPacket() {
        byte[] pkt = KobukiProtocol.right();
        assertEquals((byte) 0x9C, pkt[5]); // -100 lo
        assertEquals((byte) 0x01, pkt[7]); // radius 1 = spin
    }

    @Test
    public void testAllPacketsAre10Bytes() {
        assertEquals(10, KobukiProtocol.forward().length);
        assertEquals(10, KobukiProtocol.backward().length);
        assertEquals(10, KobukiProtocol.left().length);
        assertEquals(10, KobukiProtocol.right().length);
        assertEquals(10, KobukiProtocol.stop().length);
    }

    @Test
    public void testChecksumCorrect() {
        byte[] pkt = KobukiProtocol.forward();
        byte xor = pkt[2];
        for (int i = 3; i < pkt.length - 1; i++) xor ^= pkt[i];
        assertEquals(xor, pkt[pkt.length - 1]);
    }

    @Test
    public void testPacketToHex() {
        assertEquals("AA 55 06 01 04 00 00 00 00 03", KobukiProtocol.packetToHex(KobukiProtocol.stop()));
    }
}
