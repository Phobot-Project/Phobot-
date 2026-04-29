# Testing Guide

## Test Environment
- Flat surface, 2+ meters of clear space
- Robot charged and powered on
- Samsung A17 or Android 5.0 and above across Samsung, Pixel, Xiaomi, OnePlus, and similar Android phones with the app installed

## USB Tests

| Step | Action                        | Expected                           | P/F |
|------|-------------------------------|------------------------------------|-----|
| 1    | Connect USB, open app         | Permission popup appears           |     |
| 2    | Allow, select USB, Connect    | Status green, shows VID/PID/Chip   |     |
| 3    | STOP                          | Robot stays still                  |     |
| 4    | FORWARD then STOP             | Robot moves then stops             |     |
| 5    | BACKWARD then STOP            | Robot reverses then stops          |     |
| 6    | LEFT then STOP                | Robot spins left then stops        |     |
| 7    | RIGHT then STOP               | Robot spins right then stops       |     |
| 8    | Disconnect                    | Status red, buttons disabled       |     |

## Bluetooth Tests

| Step | Action                        | Expected                           | P/F |
|------|-------------------------------|------------------------------------|-----|
| 9    | Select BT mode                | BT options appear                  |     |
| 10   | Select BT Device              | Paired devices shown               |     |
| 11   | Pick HC-05, press Connect     | Status blue, shows device name     |     |
| 12   | FORWARD then STOP             | Robot moves then stops             |     |
| 13   | Voice: say "forward"          | Robot moves                        |     |
| 14   | Voice: say "stop"             | Robot stops                        |     |
| 15   | Disconnect                    | Status red                         |     |

## WiFi Tests

| Step | Action                        | Expected                           | P/F |
|------|-------------------------------|------------------------------------|-----|
| 16   | Select WiFi mode              | IP and port fields appear          |     |
| 17   | Enter IP and port, Connect    | Status yellow, shows IP:port       |     |
| 18   | FORWARD then STOP             | Robot moves then stops             |     |
| 19   | Disconnect                    | Status red                         |     |

## Record

- Phone: Samsung Galaxy A17(Android 16), Samsung Galaxy A14(Android 15), Samsung S22 Ultra(Android 16)
- Robot: Kobuki (Quanser/Yujin Robot)
- showing: Robot movement and voice controller to the client of the Project
- Date:29/04/2026
- Tester: Atif, Sifat, Zed, Talha, & Hafiz
