package com.teamphobot.robotcontroller;

import org.junit.Test;
import static org.junit.Assert.*;

public class CommandEncoderTest {

    @Test
    public void testForwardReturnsPacket() {
        byte[] pkt = CommandEncoder.encode(CommandEncoder.CMD_FORWARD);
        assertNotNull(pkt);
        assertEquals(10, pkt.length);
        assertEquals((byte) 0xAA, pkt[0]);
    }

    @Test
    public void testNullReturnsNull() { assertNull(CommandEncoder.encode(null)); }

    @Test
    public void testUnknownReturnsNull() { assertNull(CommandEncoder.encode("DANCE")); }

    @Test
    public void testLabels() {
        assertEquals("FWD", CommandEncoder.label(CommandEncoder.CMD_FORWARD));
        assertEquals("STP", CommandEncoder.label(CommandEncoder.CMD_STOP));
        assertEquals("UNKNOWN", CommandEncoder.label(null));
    }
}
