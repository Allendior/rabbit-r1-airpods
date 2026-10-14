# Fix AirPods audio and LibrePods on your rooted Rabbit R1

Use this guide when your AirPods pair with your R1 but media does not play, or
when LibrePods cannot show AirPods battery data on Android 13.

The steps were verified on the R1 Escape Android 13 system with Rabbit's
Android 12 MediaTek vendor partition. They do not flash a partition. Magisk
overlays the one file required for the A2DP repair, so you can undo it by
disabling the module and rebooting.

## Before you start

You need:

- a Rabbit R1 with Magisk root;
- the R1 Escape Android 13 image, ideally with a fingerprint beginning
  `rabbit/gsi_r1/r1:13/TQ3A.230805.001.S1`;
- an ADB connection so you can check the result and recover easily;
- your AirPods nearby.

First identify your running system. Do not rely only on the ROM name you were
told; this guide addresses a specific Android 13 and vendor combination.

```sh
adb shell getprop ro.build.fingerprint
adb shell getprop ro.lineage.version
```

If you see a substantially different Android or vendor build, read the
[technical investigation](BLUETOOTH_AUDIO_INVESTIGATION.md) before continuing.

## 1. Enable the Bluetooth profiles

Install `rabbit-r1-bluetooth-audio/rabbit-r1-bluetooth-audio.zip` from Magisk,
then reboot. This enables A2DP source, HFP audio gateway, and AVRCP target at
every boot.

After reboot, pair your AirPods in Android Bluetooth settings and connect them.
You should see them as the active A2DP device:

```sh
adb shell 'dumpsys bluetooth_manager | grep -E "mActiveDevice|mConnectionState"'
```

Pairing alone is not enough on the affected R1 image. If audio is still silent,
continue with the A2DP repair.

## 2. Repair A2DP media audio

On the affected R1 Escape build, Android's A2DP provider and Rabbit's vendor
Bluetooth audio module keep separate audio-session state. Bluetooth can report
that your AirPods are connected while AudioPolicy cannot open a media output.

The working repair replaces only
`/vendor/lib64/hw/audio.bluetooth.default.so` through a Magisk overlay with a
standard HIDL Bluetooth audio module. The original vendor partition stays
unchanged.

This repository deliberately does not include that prebuilt module. It came
from a public vendor dump, and a public dump does not automatically grant
redistribution rights. If you obtain a legally usable copy for this tested
build, verify it before installing:

```sh
adb shell 'su -c "sha256sum /vendor/lib64/hw/audio.bluetooth.default.so"'
```

After installing the Magisk module and rebooting, the active HAL must have this
SHA-256:

```
bf69b46cde565a778ebf6fcaf2bf4c302ac289737d58f0320a590ed3f3282363
```

You should also see the vendor audio service running:

```sh
adb shell getprop init.svc.vendor.audio-hal
```

Expected output: `running`.

Now play ordinary media. Your AirPods should receive it. The tested device
played a 44.1 kHz stereo `AudioTrack` through AirPods after reboot.

## 3. Build LibrePods for Android 13

LibrePods v1.0.1-rc1 can crash when its battery broadcast reaches the Android
13 typed Parcelable API. Apply
`patches/librepods-android13-parcelable.patch` to a LibrePods checkout:

```sh
git clone https://github.com/librepods-org/librepods.git
cd librepods
git apply ../patches/librepods-android13-parcelable.patch
cd android
./gradlew :app:assembleFossDebug
```

Install the resulting FOSS debug APK. In Vector or LSPosed, scope LibrePods to
`com.android.bluetooth`, then reboot.

With your AirPods connected, LibrePods should show their battery information
and let you change listening modes.

## 4. Roll back safely

If A2DP playback regresses, disable `Rabbit R1 Standard Bluetooth Audio HAL` in
Magisk and reboot. The original vendor HAL will return automatically. From ADB:

```sh
adb shell 'su -c "touch /data/adb/modules/rabbit_r1_standard_bt_hal/disable"'
adb reboot
```

Disable `Rabbit R1 Bluetooth Audio Profiles` as well only when you also want to
undo A2DP, HFP, and AVRCP profile enablement.

## If you are diagnosing another R1

Start with the Android build, vendor build, enabled Bluetooth profiles, A2DP
connection state, and audio-policy logs. On the affected image, the distinctive
failure is a successful A2DP provider session followed by
`A2DP_SOFTWARE_ENCODING_DATAPATH is not ready` from the vendor audio HAL.

Do not expect offload or legacy-A2DP properties to repair this version: it
advertises an offload capability mask of zero and does not include
`audio.a2dp.default.so`. Verify any replacement HAL against the system it will
run on and retain a Magisk rollback path.
