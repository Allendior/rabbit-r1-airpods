# Rabbit R1 LibrePods and Bluetooth audio investigation

Date: 2026-10-14. Device reachable over Tailscale ADB. No partition image was flashed.

## What runs on this R1

- Current system: Android 13 (`SDK 33`), fingerprint `rabbit/gsi_r1/r1:13/TQ3A.230805.001.S1/...`; `ro.lineage.version` is empty. The active system is the R1 Escape AOSP 13 image, despite the device having been described as LineageOS.
- Current vendor: Rabbit Android 12, `VNDK 31` and MediaTek MT6765 family.
- LibrePods app: patched from v1.0.1-rc1 and installed; battery and listening mode controls work.
- AirPods: pair, connect, and show as active. The media-audio failure was below the LibrePods UI and is now resolved by the targeted Bluetooth audio module overlay.

## Failure before the targeted test

1. Android Bluetooth's A2DP source starts a standard HIDL `A2DP_SOFTWARE_ENCODING_DATAPATH` session. The provider reports `startSession_cb(SUCCESS)`.
2. The standard session library logs `has NO port state observer`.
3. The vendor audio HAL repeatedly logs `BTAudioHalDeviceProxy ... A2DP_SOFTWARE_ENCODING_DATAPATH is not ready`, times out, and AudioPolicyManager fails to make the A2DP device available.
4. The vendor `audio.bluetooth.default.so` has direct dependencies on `vendor.mediatek.hardware.bluetooth.audio@2.1.so`, `vendor.mediatek.hardware.bluetooth.audio@2.2.so`, and `libbluetooth_audio_session_mediatek.so`. Its `GetSessionInstance` import uses MediaTek's session type. The standard provider uses `libbluetooth_audio_session.so`, which has a separate session map. Both are loaded in the audio service process. This explains why the provider can start successfully while the vendor module waits forever.

AOSP's [session implementation](https://android.googlesource.com/platform/hardware/interfaces/+/b09556a1e964161e805e101c5c467d1f9db9ec32/bluetooth/audio/utils/session/BluetoothAudioSession.cpp) documents that `NO port state observer` means no output callback registered on that session. The [generic audio module build definition](https://android.googlesource.com/platform/system/bt/+/ea7ab70a711e642653dd5922b83aa04a53af9e0e/audio_bluetooth_hw/Android.bp) links the standard session library. An [R1 Escape issue](https://github.com/RabbitHoleEscapeR1/r1_escape/issues/3) reports the same pair/battery/no-audio pattern with Pixel Buds Pro and Sony headphones.

## Ruled out or addressed

- LibrePods compatibility gate: Vector Xposed scope and Bluetooth profile properties enabled. Its Android 13 battery popup crash was fixed in the local source. Battery and listening mode controls now work.
- AirPods-only or codec-only fault: the exact no-audio pattern is reported with other headphones, and the failure is before audio output opens.
- Missing Bluetooth provider or audio policy: the standard HIDL 2.0/2.1 provider is registered and vendor policy already includes a `bluetooth` module. Generic Samsung GSI modules that add these would not address this session split.
- A2DP offload property: Android's offload capability mask is zero; toggling the property still selected the failing software session. Trial properties were reverted.
- Legacy audio fallback: the image has no `audio.a2dp.default.so`; toggling the legacy property could not load it. Reverted.
- Current system-wide Bluetooth HAL toggle: this R1 Escape image has no PHH Treble Settings app or `sysbta` implementation, and its Bluetooth JNI does not reference the `sysbta` service. A property alone cannot provide that implementation.

## Targeted live test

A 64 bit generic `audio.bluetooth.default.so` from a [Rokid Android 12L vendor dump](https://github.com/buildwithfenna/rokid-docs/blob/665ed1ae182afea432797bd90607b0f8eaa8c722/yodaos/DECOMPILED/vendor/lib64/hw/audio.bluetooth.default.so) was inspected. It links only standard HIDL 2.0/2.1 and `libbluetooth_audio_session.so`. All 15 imported Bluetooth session symbols resolve against the R1's existing standard session library, and the on-device linker resolves every dependency. SHA-256 of test binary: `bf69b46cde565a778ebf6fcaf2bf4c302ac289737d58f0320a590ed3f3282363`.

It was temporarily bind-mounted over `/vendor/lib64/hw/audio.bluetooth.default.so` in the global mount namespace, then only `vendor.audio-hal` was restarted. The original partition file was not written. Android immediately opened a 44.1 kHz A2DP output. A 3-second `AudioTrack` test delivered 132,300 frames, and the HAL reported A2DP `Start ... status=SUCCESS` and later `Suspend ... status=SUCCESS`. The user heard the louder test tone in the AirPods at media volume 7/15. The mount disappears on reboot.

## Durable fix and fallback

`rabbit-r1-standard-bt-hal/` contains a Magisk module with the tested binary at `system/vendor/lib64/hw/audio.bluetooth.default.so`. Magisk's [module guide](https://github.com/topjohnwu/Magisk/blob/master/docs/guides.md) specifies this overlay path for `/vendor`. The module was installed and the R1 rebooted. After reboot, the active vendor HAL had the tested SHA-256, the Magisk module was present, `vendor.audio-hal` was running, Bluetooth reported the AirPods connected as the active A2DP device, and LibrePods started its foreground service from `BOOT_COMPLETED`. The user confirmed audio was coming from the AirPods after reboot. A direct raw capture from ALSA card 0/device 1 returned silence because it bypassed Android's microphone routing and mixer setup, so it is not a microphone validation. Speaker playback and microphone recording should be checked through ordinary Android apps if either is relevant to a later workflow. If any regression appears, disable this module in Magisk and reboot, or create `/data/adb/modules/rabbit_r1_standard_bt_hal/disable` as root.

If the targeted module fails after reboot, LineageOS 21 Android 14 GSI is the best documented fallback. A [recent R1 owner report](https://www.reddit.com/r/Rabbitr1/comments/1wnvphs/what_is_the_best_way_to_listen_to_music_on_r1/) specifically says their AirPods play audio on an `arm64_bgN` LineageOS 21 GSI. A [detailed R1 account](https://substrate.dougbelshaw.com/rabbit-r1-android) names build `21-20250621-UNOFFICIAL-arm64_bgN`; the [R1 installation guide](https://gist.github.com/veritas06/462844437bd8c5751b85d99c78c68fd8) covers flashing. This is a full system migration and may require wiping userdata, so the launcher, Hermes, root modules, and app data should be backed up and restored deliberately. CipherOS Android 16 has another [R1 functionality report](https://github.com/RabbitHoleEscapeR1/r1_escape/issues/92) but a broken side button, making it a poor first choice for this use.
