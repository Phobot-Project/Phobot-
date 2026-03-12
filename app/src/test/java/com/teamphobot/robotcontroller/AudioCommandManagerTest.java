package com.teamphobot.robotcontroller;

import org.junit.Test;

import static org.junit.Assert.*;

public class AudioCommandManagerTest {

    @Test
    public void testForward() {
        assertEquals(RobotController.CMD_FORWARD,
                AudioCommandManager.mapSpeechToCommand("forward"));
    }

    @Test
    public void testBackward() {
        assertEquals(RobotController.CMD_BACKWARD,
                AudioCommandManager.mapSpeechToCommand("backward"));
    }

    @Test
    public void testBackAlias() {
        assertEquals(RobotController.CMD_BACKWARD,
                AudioCommandManager.mapSpeechToCommand("go back"));
    }

    @Test
    public void testLeft() {
        assertEquals(RobotController.CMD_LEFT,
                AudioCommandManager.mapSpeechToCommand("turn left"));
    }

    @Test
    public void testRight() {
        assertEquals(RobotController.CMD_RIGHT,
                AudioCommandManager.mapSpeechToCommand("go right"));
    }

    @Test
    public void testStopPriority() {
        assertEquals(RobotController.CMD_STOP,
                AudioCommandManager.mapSpeechToCommand("stop going forward"));
    }

    @Test
    public void testNullReturnsNull() {
        assertNull(AudioCommandManager.mapSpeechToCommand(null));
    }

    @Test
    public void testGarbageReturnsNull() {
        assertNull(AudioCommandManager.mapSpeechToCommand("hello world"));
    }
}
