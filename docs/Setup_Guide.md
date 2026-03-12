# Setup Guide

## What You Need

1. Samsung Galaxy A17 (Android 16, USB Type-C)
2. USB OTG adapter (Type-C male to USB-A female)
3. USB cable (USB-A to the Kobuki's USB port)
4. Kobuki robot base, powered on
5. Android Studio on your computer

## Install the App

1. Open this project in Android Studio
2. Connect the Samsung A17 to your computer via USB cable
3. On the phone: Settings > About Phone > tap Build Number 7 times
   (this enables Developer Options)
4. On the phone: Settings > Developer Options > turn on USB Debugging
5. Your phone will ask "Allow USB Debugging?" - tap Allow
6. In Android Studio, click the device dropdown at the top
7. Select your Samsung A17 (NOT "Pixel 5" or any emulator)
8. Click the green Run button
9. The app installs and opens on your phone

## Connect the Robot

1. Disconnect the phone from the computer
2. Power on the Kobuki robot
3. Plug the USB OTG adapter into your phone's Type-C port
4. Connect the USB cable from the OTG adapter to the Kobuki's USB port
5. Your phone will show: "Allow PHOBOT Controller to access USB device?"
6. Tap Allow
7. Open the app and press Connect
8. The status bar should turn green
9. The device info should show VID=0x403 PID=0x6001 (FTDI chip)

## Troubleshooting

"No USB device found"
- Check the OTG adapter is plugged in firmly
- Check the robot is powered on
- Try unplugging and replugging the cable
- Try a different OTG adapter

"Connection failed: FTDI initialization failed"
- The robot's USB port may not be powered
- Check the Kobuki's power switch is ON
- Try disconnecting and reconnecting

"Bulk transfer failed"
- This should not happen with the new code (FTDI is initialized now)
- If it still happens, the USB cable may be damaged
- Try a different cable

Buttons do nothing / robot does not move
- Check the event log at the bottom of the app
- Look for "Sent FORWARD [AA 55 ...]" messages
- If packets are being sent but the robot does not move, the robot's
  motor controller may need to be checked
