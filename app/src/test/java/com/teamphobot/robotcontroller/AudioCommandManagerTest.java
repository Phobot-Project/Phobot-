package com.teamphobot.robotcontroller;

import org.junit.Test;
import static org.junit.Assert.*;

public class AudioCommandManagerTest {

    @Test
    public void testForward() { assertEquals(CommandEncoder.CMD_FORWARD, AudioCommandManager.mapSpeechToCommand("forward")); }

    @Test
    public void testBack() { assertEquals(CommandEncoder.CMD_BACKWARD, AudioCommandManager.mapSpeechToCommand("go back")); }

    @Test
    public void testStopPriority() { assertEquals(CommandEncoder.CMD_STOP, AudioCommandManager.mapSpeechToCommand("stop going forward")); }

    @Test
    public void testNull() { assertNull(AudioCommandManager.mapSpeechToCommand(null)); }

    @Test
    public void testGarbage() { assertNull(AudioCommandManager.mapSpeechToCommand("hello world")); }
}
