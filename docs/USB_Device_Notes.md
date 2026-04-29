# USB and Wireless Device Notes

## USB (Direct Cable)

The Kobuki uses an FTDI FT232R chip. Before sending data, UsbHelper
initializes it: reset, 115200 baud, 8N1, no flow control, DTR+RTS high.

| Chip   | VID    | PID    |
|--------|--------|--------|
| FTDI   | 0x0403 | 0x6001 |
| CP2102 | 0x10C4 | 0xEA60 |
| CH340  | 0x1A86 | 0x7523 |

## Bluetooth (HC-05/HC-06)

HC-05/HC-06 wiring to Kobuki:
- HC-05 TX -> Kobuki RX
- HC-05 RX -> Kobuki TX
- GND -> GND
- VCC -> 3.3V or 5V (check your module)

Configure baud rate: Connect to HC-05 via AT mode, send AT+BAUD8 (115200).
Default pairing PIN is usually 1234 or 0000.
RFCOMM UUID used: 00001101-0000-1000-8000-00805F9B34FB (standard SPP).

## WiFi (ESP8266/ESP32)

ESP wiring to Kobuki:
- ESP TX -> Kobuki RX
- ESP RX -> Kobuki TX
- GND -> GND
- VCC -> 3.3V

The ESP needs firmware that:
1. Creates a WiFi AP (or connects to existing network)
2. Runs a TCP server on a known port
3. Forwards TCP data to Serial at 115200 baud
4. Forwards Serial data to TCP (optional, for sensor data)

Default ESP8266 AP mode IP: 192.168.4.1
Common port: 8080

The app sends the exact same Kobuki binary packets through TCP.
The ESP transparently passes them to the Kobuki's serial port.
