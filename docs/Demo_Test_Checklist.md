# Demo Test Checklist

Phone: Samsung Galaxy A17, Android 16, USB Type-C
Robot: Kobuki (Yujin Robot / Quanser), DC 14.8V

## Before Demo
- [ ] Kobuki powered on
- [ ] Phone charged (50%+)
- [ ] USB OTG adapter ready
- [ ] USB cable ready
- [ ] App installed on real phone (not emulator)
- [ ] Test area flat and clear

## Connection
- [ ] OTG adapter plugged into phone
- [ ] USB cable connected to Kobuki
- [ ] USB permission popup appeared
- [ ] Tapped Allow
- [ ] App opened
- [ ] Connect button pressed
- [ ] Status shows "Connected" (green)
- [ ] Device info shows VID=0x403 PID=0x6001
- [ ] Event log shows "FTDI initialized: 115200 baud, 8N1"

## Movement Control
- [ ] STOP tested first (robot stays still)
- [ ] FORWARD (robot moves forward)
- [ ] STOP (robot stops)
- [ ] BACKWARD (robot moves backward)
- [ ] STOP
- [ ] LEFT (robot spins counter-clockwise)
- [ ] STOP
- [ ] RIGHT (robot spins clockwise)
- [ ] STOP

## Event Log Check
- [ ] Log shows "Sent FORWARD [AA 55 ...]"
- [ ] Log shows hex packet data for each command
- [ ] No "FAIL" or "ERROR" messages

## Voice Control
- [ ] Microphone permission granted
- [ ] Voice button pressed, said "forward"
- [ ] Robot moved forward
- [ ] Said "stop", robot stopped

## Disconnect
- [ ] Disconnect pressed
- [ ] Status red, buttons disabled
- [ ] Reconnect tested

## Notes
Date:11/03/2026
Tester: Atif,Sifat,Zed,Talha
Issues: During testing , we found that the robot was not responding when the movement buttons were pressed in the app. The main issue we observed was that pressing forward, backward, left, or right was triggering the error message “bulk transfer failed (result = -1)”, which indicated that the USB communication between the phone and the robot was not being handled correctly in the earlier build.

After reviewing the problem, the main fixes were focused on the communication path and overall controller structure. We investigated the USB failure, reviewed the earlier project versions, and found that the previous app was relying on a more generic USB transfer approach that was not properly suited to the robot’s communication method. To address this, the controller was rebuilt with a more hardware-specific approach, including improved FTDI-based communication handling, updated Kobuki packet-based command logic, better USB device filtering, and a cleaner Android project structure.

In addition to that, the movement command flow was reorganised, missing class and layout dependencies were corrected, the UI was improved, and a basic audio command feature was added so that the build aligns more closely with the PIR requirements. Supporting documents such as setup guidance, testing notes, USB notes, and known limitations were also added to make the latest build easier for the team to review and test.

The updated package prepared after these fixes is the latest Robot_PHOBOT.zip build.
