# Known Limitations

## USB
- Connects to first serial device found (no device selection UI)
- FTDI, CP2102, and CH340 chips supported. Other chips may not work.

## Bluetooth
- User must pair the HC-05/HC-06 from Android Settings before using the app
- HC-05/HC-06 baud rate must be set to 115200 (AT+BAUD8)
- Bluetooth range is typically 10 meters (depends on environment)
- No auto-reconnect if Bluetooth disconnects

## WiFi
- User must know the ESP's IP address and port
- Phone must be on the same WiFi network as the ESP (or ESP's AP network)
- No auto-discovery of the ESP device
- No encryption on the TCP connection

## General
- Fixed speed: 200 mm/s forward, 100 mm/s turn (no speed slider)
- One-way communication (sends commands, no sensor data back)
- Kobuki watchdog stops robot in ~600ms if connection drops
- Voice recognition needs internet on most phones
