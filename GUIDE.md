# Rabbit R1 Android 13: LibrePods and AirPods audio

## Scope

This guide applies to the Rabbit R1 Escape Android 13 system with Rabbit's
Android 12 MediaTek vendor partition, root through Magisk, and a working ADB
connection. The tested device fingerprint began with
`rabbit/gsi_r1/r1:13/TQ3A.230805.001.S1`.

It fixes two separate issues:

1. Android had Bluetooth's A2DP, HFP, and AVRCP services available but disabled.
2. After profiles were enabled, AirPods could pair and show battery data but
   media audio could not open because the stock Bluetooth audio module and the
   active AOSP provider used different Bluetooth session libraries.

## Install

1. In Magisk, install `rabbit-r1-bluetooth-audio/rabbit-r1-bluetooth-audio.zip`.
   This enables the required Bluetooth profiles on every boot.
2. Install the privately retained `rabbit-r1-standard-bt-hal.zip` in Magisk.
   It overlays only `/vendor/lib64/hw/audio.bluetooth.default.so`; it does not
   flash or alter the actual vendor partition.
3. Reboot.
4. Pair the AirPods in Android Bluetooth settings and connect them.
5. Apply `patches/librepods-android13-parcelable.patch` to a LibrePods clone,
   build it with `cd android && ./gradlew :app:assembleFossDebug`, and install
   the FOSS debug APK.
   In Vector/Xposed, scope LibrePods to `com.android.bluetooth`, then reboot.

## Verify

Run these commands from a host with ADB. Substitute the device serial when
needed.

```sh
adb shell 'getprop init.svc.vendor.audio-hal'
adb shell 'dumpsys bluetooth_manager | grep -E "mActiveDevice|mConnectionState"'
adb shell 'su -c "sha256sum /vendor/lib64/hw/audio.bluetooth.default.so"'
```

Expected results are `running`, a connected active A2DP device, and this hash:

```
bf69b46cde565a778ebf6fcaf2bf4c302ac289737d58f0320a590ed3f3282363
```

Then play ordinary media. LibrePods should expose the connected AirPods and
their battery/listening-mode controls. The tested device played a 44.1 kHz
stereo `AudioTrack` through the AirPods after reboot.

## Android 13 LibrePods source compatibility fix

LibrePods v1.0.1-rc1 used the Android 13 typed Parcelable overload for battery
broadcasts and crashed when the broadcast was delivered from Bluetooth. In
`PopupWindow.kt` and `IslandWindow.kt`, set the extras class loader first, then
use the compatible deprecated overload:

```kotlin
intent.setExtrasClassLoader(Battery::class.java.classLoader)
@Suppress("DEPRECATION")
val batteryList = intent.getParcelableArrayListExtra<Battery>("data")
```

The local LibrePods source already contains this change.

## Rollback

In Magisk, disable `Rabbit R1 Standard Bluetooth Audio HAL` and reboot. The
original vendor HAL reappears automatically. To disable it from root:

```sh
adb shell 'su -c "touch /data/adb/modules/rabbit_r1_standard_bt_hal/disable"'
adb reboot
```

Disable `Rabbit R1 Bluetooth Audio Profiles` as well only if you also want to
undo A2DP/HFP/AVRCP profile enablement. Re-enable a module and reboot to restore
it.

## For an AI agent diagnosing a new R1

First establish the installed Android and vendor versions. Do not assume a
device described as LineageOS is actually running LineageOS. Check Bluetooth
profile enablement, A2DP connection state, and audio-policy logs. On this image,
the signature failure is a successful A2DP provider session followed by
`A2DP_SOFTWARE_ENCODING_DATAPATH is not ready` from the vendor audio HAL.

Do not rely on A2DP offload or legacy-A2DP properties here: the tested image
advertises an offload capability mask of zero and lacks `audio.a2dp.default.so`.
The targeted standard HAL overlay is the repair. Verify its SHA-256 before
installing and retain the rollback path above.

## Binary provenance

The Bluetooth HAL binary is a generic 64-bit module from a public
[Rokid Android 12L vendor dump](https://github.com/buildwithfenna/rokid-docs/blob/665ed1ae182afea432797bd90607b0f8eaa8c722/yodaos/DECOMPILED/vendor/lib64/hw/audio.bluetooth.default.so), not an R1-specific
upstream release. Its SHA-256 is recorded above. It was examined for its
standard HIDL and `libbluetooth_audio_session.so` dependencies; all required
symbols resolved on the R1. Validate it on any different R1 image before use.

The source dump being public does not establish redistribution rights for the
binary. Keep the actual `.so` and prebuilt module private unless the rights are
confirmed. The guide, source changes, checksums, and build instructions are
safe to publish separately.
