# Robot PHOBOT Controller

Android app that controls a Kobuki robot (Yujin Robot / Quanser edition)
via three connection methods: USB cable, Bluetooth, and WiFi.

Phone: Samsung Galaxy A17 (Android 16, Type-C)
Robot: Kobuki base, DC 14.8V, Made in Korea by Yujin Robot

---

## Connection Modes

### USB (Direct Cable)
Connect phone to Kobuki via Type-C cable (or OTG adapter).
The app initializes the FTDI/CP2102/CH340 serial chip and sends
Kobuki binary packets over USB bulk transfer.

### Bluetooth (Wireless via HC-05/HC-06)
Attach an HC-05 or HC-06 Bluetooth serial adapter to the Kobuki's
serial port (TX, RX, GND, VCC). Pair it from Android Settings first.
The app connects via Bluetooth RFCOMM and sends the same Kobuki
packets through the wireless serial link.

### WiFi (Wireless via ESP8266/ESP32)
Attach an ESP8266 or ESP32 to the Kobuki's serial port. The ESP runs
a TCP server. The app connects to the ESP's IP and port, and sends
Kobuki packets through the TCP socket.

All three modes send identical Kobuki binary packets. Only the
transport layer changes.

---

## Command Protocol

    [0xAA] [0x55] [Length] [0x01] [0x04] [Speed_Lo] [Speed_Hi] [Radius_Lo] [Radius_Hi] [Checksum]

| Command  | Speed   | Radius | Hex Packet                     |
|----------|---------|--------|--------------------------------|
| FORWARD  | +200    | 0      | AA 55 06 01 04 C8 00 00 00 CB  |
| BACKWARD | -200    | 0      | AA 55 06 01 04 38 FF 00 00 C4  |
| LEFT     | +100    | 1      | AA 55 06 01 04 64 00 01 00 66  |
| RIGHT    | -100    | 1      | AA 55 06 01 04 9C FF 01 00 61  |
| STOP     | 0       | 0      | AA 55 06 01 04 00 00 00 00 03  |

Commands are re-sent every 250ms (Kobuki watchdog is ~600ms).

---

## Build and Run

1. Open this project in Android Studio
2. Wait for Gradle sync
3. Connect Samsung A17 to computer via USB
4. Enable Developer Options + USB Debugging on phone
5. Select Samsung A17 in the device dropdown (NOT emulator)
6. Press Run. App installs on the phone.

---

## USB Setup

1. Power on Kobuki
2. Connect Type-C cable from Kobuki to phone
3. Allow USB access when prompted
4. Select USB mode in the app
5. Press Connect

## Bluetooth Setup

1. Wire HC-05/HC-06 to Kobuki serial port (TX->RX, RX->TX, GND, VCC)
2. Configure adapter baud rate to 115200 (AT+BAUD8 for HC-05)
3. Pair the adapter from Android Settings > Bluetooth
4. In the app, select Bluetooth mode
5. Press "Select Bluetooth Device" and pick the HC-05/HC-06
6. Press Connect

## WiFi Setup

1. Wire ESP8266/ESP32 to Kobuki serial port
2. Flash the ESP with a TCP-to-serial bridge sketch (baud 115200)
3. Connect the phone to the ESP's WiFi network (or same router)
4. In the app, select WiFi mode
5. Enter the ESP's IP address and port
6. Press Connect

---

## Voice Commands

"forward", "backward"/"back", "left", "right", "stop"
(stop always takes priority)

---

## Project Files

| File                      | Purpose                                          |
|---------------------------|--------------------------------------------------|
| MainActivity.java         | App screen, mode selection, buttons, voice, log  |
| RobotController.java      | High-level commands + 250ms repeat timer         |
| CommandEncoder.java        | Maps command names to KobukiProtocol calls        |
| KobukiProtocol.java       | Builds binary packets (AA 55 + speed + checksum) |
| UsbHelper.java            | FTDI/CP2102/CH340 init + USB bulk transfer       |
| BluetoothHelper.java      | RFCOMM serial connection via HC-05/HC-06         |
| WifiHelper.java           | TCP socket connection via ESP8266/ESP32           |
| ConnectionManager.java    | Manages all three transports, unified send()     |
| PermissionReceiver.java   | USB permission dialog handler                    |
| AudioCommandManager.java  | Speech-to-command mapping                        |

---

## Architecture

    UI (MainActivity)
        |
        v
    RobotController         (forward/stop + repeat timer)
        |
        v
    CommandEncoder           (picks KobukiProtocol method)
        |
        v
    KobukiProtocol           (builds binary packet)
        |
        v
    ConnectionManager.send() (routes to active transport)
        |
        +-- UsbHelper        (FTDI init + USB bulk transfer)
        +-- BluetoothHelper  (RFCOMM serial via HC-05/HC-06)
        +-- WifiHelper       (TCP socket via ESP8266/ESP32)

---

## Known Limitations

- USB connects to the first serial device found
- Bluetooth requires pre-pairing the adapter from Android Settings
- WiFi requires knowing the ESP's IP and port
- No speed control (fixed 200 mm/s forward, 100 mm/s turn)
- One-way only (sends commands, does not read sensors)
- Voice needs internet on most phones

---

## Team

Team PHOBOT
