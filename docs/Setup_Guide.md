# Setup Guide

## What You Need

For USB:
- Samsung A17 (Type-C) or any Android 5.0 and above across Samsung, Pixel, Xiaomi, OnePlus, and similar Android phones.
- USB cable (Type-C to Kobuki)
- Kobuki powered on

For Bluetooth:
- HC-05 or HC-06 Bluetooth serial adapter
- Jumper wires to connect adapter to Kobuki serial port
- Pair the adapter from phone Settings > Bluetooth

For WiFi:
- ESP8266 or ESP32 module
- Jumper wires to connect ESP to Kobuki serial port
- TCP-to-serial bridge firmware on the ESP

## Install the App

1. Open project in Android Studio
2. Connect Samsung A17 via USB to the computer
3. Settings > About Phone > tap Build Number 7 times
4. Developer Options > USB Debugging ON
5. Select Samsung A17 in the Android Studio device dropdown
6. Press Run

## USB Connection

1. Power on Kobuki
2. Connect USB cable from Kobuki to phone Type-C port
3. Tap Allow on the USB permission popup
4. Open app, select USB mode, press Connect
5. Status turns green

## Bluetooth Connection

1. Wire HC-05 to Kobuki: HC-05 TX -> Kobuki RX, HC-05 RX -> Kobuki TX, GND, VCC (3.3V or 5V)
2. Configure HC-05 baud rate: AT+BAUD8 (for 115200)
3. On phone: Settings > Bluetooth > Pair with HC-05 (default PIN: 1234)
4. Open app, select Bluetooth mode
5. Tap "Select Bluetooth Device" and pick HC-05
6. Press Connect
7. Status turns blue

## WiFi Connection

1. Wire ESP8266 to Kobuki: ESP TX -> Kobuki RX, ESP RX -> Kobuki TX, GND, VCC
2. Flash ESP with TCP-serial bridge at 115200 baud
3. Connect phone to ESP's WiFi network
4. Open app, select WiFi mode
5. Enter IP (default: 192.168.4.1) and port (e.g. 8080)
6. Press Connect
7. Status turns yellow

## Troubleshooting

USB "No device found": Check cable, check OTG, check robot power
USB "Bulk transfer failed": Should not happen (FTDI is initialized). Try different cable.
BT "Connection failed": Check pairing, check HC-05 power, check baud rate config
WiFi "Connection failed": Check IP, check port, check WiFi network, check ESP power
