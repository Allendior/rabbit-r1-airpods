# Enable Bluetooth audio profiles on your Rabbit R1

Install this Magisk module when your Rabbit R1 Android 13 image does not enable
Bluetooth audio profiles by default. Your Bluetooth package already contains
A2DP, HFP, and AVRCP; this module enables their profile properties at boot so
your paired audio devices can connect.

To undo the change, disable or uninstall this module in Magisk and reboot.

The property names come from Android 13 Bluetooth's `A2dpService.isEnabled()`, `HeadsetService.isEnabled()`, and `AvrcpTargetService.isEnabled()` checks.

This enables profiles and pairing. On the affected R1 Escape image, you also
need the standard Bluetooth audio HAL repair before A2DP media audio will work.
See the top-level `GUIDE.md` for the diagnostic and rollback steps.
