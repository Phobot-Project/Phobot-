# USB Device Notes

## Kobuki Robot Base

Manufacturer: Yujin Robot (distributed by Quanser)
Label: QUANSER INNOVATE EDUCATE, WWW.QUANSER.COM, YUJIN ROBOT
Power: DC 14.8V
Made in Korea

## USB Connection

The Kobuki uses an FTDI FT232R chip for USB communication.
- FTDI Vendor ID: 0x0403 (decimal 1027)
- FTDI Product ID: 0x6001 (decimal 24577)

The FTDI chip converts USB to serial (UART). The Kobuki's internal
microcontroller communicates at 115200 baud, 8 data bits, no parity,
1 stop bit (8N1), no flow control.

## Why FTDI Initialization Is Required

The FTDI FT232R chip needs to be configured before it will pass data
through to the serial port. Without configuration, all bulk transfers
return -1 (error).

The app sends these USB control transfers during initialization:
1. Reset (request=0, value=0)
2. Purge RX (request=0, value=1)
3. Purge TX (request=0, value=2)
4. Set baud rate 115200 (request=3, value=0x001A, divisor=26)
5. Set 8N1 data format (request=4, value=0x0008)
6. Set no flow control (request=2, value=0x0000)
7. Set DTR high (request=1, value=0x0101)
8. Set RTS high (request=1, value=0x0202)

Control transfer type: 0x40 (vendor, host-to-device)

## Kobuki Protocol

The Kobuki uses a binary packet protocol:

  Header:  0xAA 0x55
  Length:  1 byte (payload size)
  Payload: sub-payload ID + sub-payload length + data
  Checksum: XOR of all bytes from Length through end of payload

Base Control sub-payload (ID = 0x01):
  Sub-payload length: 0x04
  Speed: signed 16-bit LE, mm/s (positive = forward)
  Radius: signed 16-bit LE, mm (0 = straight, 1 = spin left, -1 = spin right)

## Example Packets

Forward (100 mm/s, straight):
  AA 55 06 01 04 64 00 00 00 63

Backward (-100 mm/s, straight):
  AA 55 06 01 04 9C FF 00 00 60

Turn Left (100 mm/s, radius=1):
  AA 55 06 01 04 64 00 01 00 62

Turn Right (100 mm/s, radius=-1):
  AA 55 06 01 04 64 00 FF FF 64

Stop (0 mm/s, radius=0):
  AA 55 06 01 04 00 00 00 00 03

## Phone Requirements

Samsung Galaxy A17, Android 16, USB Type-C.
The phone must support USB OTG (On-The-Go) to act as a USB host.
USB Type-C OTG adapter needed if the Kobuki uses USB-A port.
