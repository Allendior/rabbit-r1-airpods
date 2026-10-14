# Repair A2DP media audio on your Rabbit R1

Use this Magisk module when your AirPods connect but media remains silent on the
affected R1 Escape Android 13 image. It overlays only
`/vendor/lib64/hw/audio.bluetooth.default.so`. Your system starts an AOSP HIDL
2.1 Bluetooth audio session, while the stock MediaTek module waits on a
separate MediaTek session library. The replacement uses the standard session
library already on your R1.

The binary is from the public [Rokid Android 12L vendor dump](https://github.com/buildwithfenna/rokid-docs/blob/665ed1ae182afea432797bd90607b0f8eaa8c722/yodaos/DECOMPILED/vendor/lib64/hw/audio.bluetooth.default.so).
SHA-256: `bf69b46cde565a778ebf6fcaf2bf4c302ac289737d58f0320a590ed3f3282363`.
It is a generic `audio.bluetooth.default.so` built from AOSP-style `system/bt/audio_bluetooth_hw` code and links to `libbluetooth_audio_session.so`, not MediaTek's private session library. All 15 imported Bluetooth session symbols were verified against the R1's standard session library. The on-device dynamic linker resolved all dependencies. This proves load compatibility, not full functional compatibility.

The original R1 HAL SHA-256 is
`d51436177ab9434af022b18ad24b7287c3329aef536cc8579be8427436150e42`.
This module does not modify your original vendor partition. Disable it in Magisk
or create `/data/adb/modules/rabbit_r1_standard_bt_hal/disable` and reboot to
restore the original HAL.

As a temporary live test, the standard module was bind-mounted over the R1
module and `vendor.audio-hal` was restarted on 2026-10-14. AudioPolicyManager
then opened a 44.1 kHz A2DP output where it had previously failed. A 3-second
440 Hz test tone reached the AirPods and was heard by the user. The module
was subsequently installed in Magisk and survived a reboot. After reboot, the
active HAL SHA-256 matched the tested binary, the vendor audio service was
running, Bluetooth reported the AirPods as the active connected A2DP device,
and the user confirmed audio was coming from the AirPods. A raw ALSA capture
did not validate the microphone because it bypassed Android's routing and mixer
setup. Speaker playback and microphone recording should be checked through
normal Android apps when a later workflow depends on them.
