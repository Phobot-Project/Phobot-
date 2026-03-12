# Robot_PHOBOT

Android app that controls a Kobuki robot base (Yujin Robot / Quanser) over USB
from a Samsung Galaxy A17 phone running Android 16.

---

## What This App Does

You connect the phone to the Kobuki robot using a USB Type-C cable (with OTG).
The app sends Kobuki binary protocol packets over USB to make the robot move
forward, backward, turn left, turn right, or stop. You control it with
on-screen buttons or voice commands.

---

## How It Works (Technical)

The Kobuki robot has an FTDI FT232R USB-to-serial chip inside (VID=0x0403,
PID=0x6001). The phone connects to this chip as a USB Host.

Before sending any movement data, the app initializes the FTDI chip with
USB control transfers:
  - Reset the chip
  - Set baud rate to 115200
  - Set data format to 8 data bits, no parity, 1 stop bit (8N1)
  - Set flow control to none
  - Purge RX and TX buffers

After initialization, the app sends Kobuki protocol packets over USB bulk
transfer. Each packet has this format:

  [0xAA] [0x55] [Length] [0x01] [0x04] [SpeedLSB] [SpeedMSB] [RadiusLSB] [RadiusMSB] [Checksum]

  0xAA 0x55 = packet header
  Length = payload size (always 6 for base control)
  0x01 = Base Control sub-payload ID
  0x04 = 4 data bytes follow
  Speed = signed 16-bit little-endian, in mm/s (100 = forward, -100 = backward)
  Radius = signed 16-bit little-endian, in mm (0 = straight, 1 = spin left, -1 = spin right)
  Checksum = XOR of all bytes from Length through the last data byte

---

## Requirements

- Samsung Galaxy A17 (Android 16, USB Type-C)
- USB OTG adapter or direct Type-C cable to Kobuki
- Kobuki robot base powered on (DC 14.8V)
- Android Studio to build and install the app

---

## Build and Run

1. Open Android Studio
2. File > Open > select this project folder
3. Wait for Gradle sync
4. Connect your Samsung A17 via USB
5. Enable Developer Options (Settings > About Phone > tap Build Number 7 times)
6. Enable USB Debugging (Settings > Developer Options)
7. Select your Samsung A17 in the device dropdown (NOT the emulator)
8. Press Run (green play button)
9. App installs on the real phone

---

## Hardware Setup

1. Power on the Kobuki robot
2. Plug USB OTG adapter into the Samsung A17 Type-C port
3. Connect USB cable from OTG adapter to the Kobuki's USB port
4. Phone shows USB permission popup. Tap Allow.
5. Open the PHOBOT Controller app
6. Press Connect
7. Status should turn green with device info showing FTDI VID/PID

---

## Testing Procedure

1. Connect to the Kobuki (follow Hardware Setup above)
2. Press STOP first (verify robot does not move)
3. Press FORWARD (robot moves forward)
4. Press STOP (robot stops)
5. Press BACKWARD (robot moves backward)
6. Press STOP
7. Press LEFT (robot spins counter-clockwise)
8. Press STOP
9. Press RIGHT (robot spins clockwise)
10. Press STOP
11. Test voice: press Voice Command, say "forward"
12. Press Voice Command, say "stop"
13. Press Disconnect

---

## File Guide

| File                       | What It Does                                              |
|----------------------------|-----------------------------------------------------------|
| MainActivity.java          | App screen. Buttons, status, voice control, event log.    |
| FtdiDevice.java            | Initializes FTDI chip (baud rate, 8N1) and sends bytes.   |
| KobukiPacket.java          | Builds Kobuki binary protocol packets with checksum.      |
| RobotController.java       | High-level commands: forward(), stop(), etc.              |
| ConnectionManager.java     | USB device discovery, permission, connect/disconnect.     |
| PermissionReceiver.java    | Handles USB permission dialog result.                     |
| AudioCommandManager.java   | Maps speech text to robot commands.                       |
| usb_device_filter.xml      | Tells Android to auto-detect FTDI/Kobuki USB devices.     |

---

## Voice Commands

Supported: forward, backward (or back, reverse), left, right, stop.
"stop" always takes priority over other words for safety.
Needs microphone permission and Google speech recognition service.

---

## Known Limitations

- Tested on Samsung A17 with Android 16 and Kobuki (FTDI FT232R)
- Robot speed is fixed at 100 mm/s (no speed control slider)
- One-way communication only (app sends, does not read robot status)
- Voice recognition needs internet on most phones
- Will NOT work on an emulator (no real USB port)

---

## Team

Team PHOBOT
