# Known Limitations

## FTDI-Specific
This app initializes the FTDI FT232R chip with control transfers for 115200
baud, 8N1. If the robot uses a different USB-serial chip (CH340, CP2102),
the control transfer parameters may be different. The FTDI initialization
code is in FtdiDevice.java.

## One-Way Communication
The app sends commands to the Kobuki but does not read sensor data back.
There is no feedback confirming the robot received or executed a command.

## Fixed Speed
Movement speed is fixed at 100 mm/s. There is no speed slider.
To change the speed, edit the DEFAULT_SPEED value in KobukiPacket.java.

## Single Device
The app connects to the first FTDI device it finds. If multiple USB devices
are attached, it may pick the wrong one.

## Voice Recognition
Requires internet on most phones (Google speech service).
Accuracy varies with accent and background noise.
Each voice command requires pressing the button (no continuous listening).

## Emulator
Will not work on an Android emulator. The emulator has no real USB port.
Always test on the real Samsung A17 phone.

## Tested Configuration Only
Tested on: Samsung Galaxy A17, Android 16, USB Type-C, Kobuki (FTDI FT232R).
Other phone/robot combinations are not guaranteed to work without changes.
