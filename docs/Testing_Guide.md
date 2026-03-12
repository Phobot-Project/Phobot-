# Testing Guide

## Test Environment

- Flat, open surface with 2+ meters of clear space
- Kobuki robot on the ground (not on a table)
- USB cable with enough slack
- Samsung A17 with the app installed

## Safety

- Test STOP before any movement command
- Keep hands clear of wheels
- Do not run near table edges or stairs

## Test Steps

### Connection Tests

1. Power on Kobuki
2. Plug OTG adapter into Samsung A17
3. Connect USB cable to Kobuki
4. Tap Allow on the USB permission popup
5. Open app, press Connect
6. Verify: status green, shows "Connected"
7. Verify: device info shows VID=0x403 PID=0x6001
8. Verify: event log shows "FTDI initialized: 115200 baud, 8N1"

### Movement Tests

9.  Press STOP - verify robot does not move
10. Press FORWARD - verify robot moves forward
11. Press STOP - verify robot stops
12. Press BACKWARD - verify robot moves backward
13. Press STOP
14. Press LEFT - verify robot spins counter-clockwise
15. Press STOP
16. Press RIGHT - verify robot spins clockwise
17. Press STOP

### Voice Tests

18. Press "Start Voice Command"
19. Allow microphone permission if asked
20. Say "forward" - robot moves forward
21. Press voice button, say "stop" - robot stops
22. Say "left" - robot turns left
23. Say "stop"

### Disconnect Tests

24. Press Disconnect
25. Verify: status red, buttons disabled
26. Press Connect - verify reconnection works

## What to Record

- Phone: Samsung Galaxy A17, Android 16
- Robot: Kobuki (Yujin Robot / Quanser)
- Cable: USB Type-C OTG + USB-A cable
- Pass/Fail for each step
- Screenshots of app showing connected state and event log
- Any errors from the event log
