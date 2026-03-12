package com.teamphobot.robotcontroller;

/**
 * AudioCommandManager converts spoken text from Android speech recognition
 * into robot command names.
 *
 * "stop" is always checked first for safety. If someone says
 * "stop going forward", the robot will stop.
 */
public class AudioCommandManager {

    /**
     * Maps speech text to a command name.
     *
     * @param spokenText The recognized speech (any case)
     * @return One of RobotController.CMD_* constants, or null if no match
     */
    public static String mapSpeechToCommand(String spokenText) {
        if (spokenText == null || spokenText.isEmpty()) {
            return null;
        }

        String s = spokenText.toLowerCase().trim();

        // STOP checked first. Safety priority.
        if (s.contains("stop")) return RobotController.CMD_STOP;
        if (s.contains("forward") || s.contains("go ahead")) return RobotController.CMD_FORWARD;
        if (s.contains("backward") || s.contains("back") || s.contains("reverse")) return RobotController.CMD_BACKWARD;
        if (s.contains("left")) return RobotController.CMD_LEFT;
        if (s.contains("right")) return RobotController.CMD_RIGHT;

        return null;
    }
}
