# Rabbit R1 Bluetooth audio profiles

The Rabbit R1 Android 13 build had only `GattService` enabled. Its Bluetooth package includes A2DP, HFP, and AVRCP services, but their profile properties were unset. This Magisk module sets the three properties at boot so paired audio devices can connect.

The runtime change was tested on the R1 with `resetprop` and a Bluetooth service restart. To remove the boot-time change, disable or uninstall this module in Magisk and reboot.

The property names come from Android 13 Bluetooth's `A2dpService.isEnabled()`, `HeadsetService.isEnabled()`, and `AvrcpTargetService.isEnabled()` checks.

This enables Bluetooth profiles and pairing. On the tested R1 image, it must be
installed together with the `rabbit-r1-standard-bt-hal` module to make A2DP
media audio work. That module resolves the separate vendor audio-HAL session
failure; see the top-level `GUIDE.md` for installation and rollback.
