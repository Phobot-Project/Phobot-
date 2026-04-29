package com.teamphobot.robotcontroller;

/**
 * Maps speech recognition text to robot commands.
 * "stop" is always checked first for safety.
 */
public class AudioCommandManager {

    public static String mapSpeechToCommand(String spokenText) {
        if (spokenText == null || spokenText.isEmpty()) return null;
        String s = spokenText.toLowerCase().trim();

        if (s.contains("stop"))     return CommandEncoder.CMD_STOP;
        if (s.contains("forward"))  return CommandEncoder.CMD_FORWARD;
        if (s.contains("backward")) return CommandEncoder.CMD_BACKWARD;
        if (s.contains("back"))     return CommandEncoder.CMD_BACKWARD;
        if (s.contains("reverse"))  return CommandEncoder.CMD_BACKWARD;
        if (s.contains("left"))     return CommandEncoder.CMD_LEFT;
        if (s.contains("right"))    return CommandEncoder.CMD_RIGHT;
        return null;
    }
}
