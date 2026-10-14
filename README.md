# Rabbit R1 Android Mods: camera, AirPods, gestures, and Hermes vision

Turn a rooted Rabbit R1 running Android 13 into a more capable everyday device.
This repository collects the fixes and installable files tested on a Rabbit R1
with the R1 Escape/Lineage-based system image.

## What you can add

- **Eye camera controls:** take a photo from the Eye screen or press the power
  button while Eye is open.
- **Automatic photo orientation:** portrait photos stay portrait and landscape
  photos stay landscape, with the preview and saved JPEG facing upright.
- **Hermes vision:** hold the power button in Eye, ask about the scene, and
  release it to send the current corrected frame to Hermes.
- **Top-edge volume gesture:** swipe left or right across the top of the screen
  for volume while keeping the wheel dedicated to scrolling.
- **AirPods and LibrePods:** restore A2DP media audio and fix LibrePods battery
  and listening-mode support on Android 13.

## Start here

Follow [GUIDE.md](GUIDE.md) for installation, verification, and rollback. The
[downloads](downloads/) directory contains the ready-to-install APK, Magisk
modules, compatibility patch, and SHA-256 checksums.

| Feature | Ready-to-use file |
| --- | --- |
| Eye capture, power-button photos, orientation, Hermes frames | `rabbit-r1-camera-controls-v1.0.0.apk` |
| Top-edge volume swipe | `rabbit-r1-volume-swipe-v1.0.zip` |
| Bluetooth profile setup | `rabbit-r1-bluetooth-audio-profiles-v1.0.zip` |
| LibrePods Android 13 fix | `librepods-android13-parcelable.patch` |

## Tested device

The current files were tested on Android 13 with a fingerprint beginning
`rabbit/gsi_r1/r1:13/TQ3A.230805.001.S1`, Magisk root, and Vector Framework
2.2/API 102. Read each component's notes before using it on another build.

The source for the camera and volume additions is included so you can audit,
change, or rebuild it instead of treating the downloads as opaque files.

## Technical notes

- [Camera controls](rabbit-r1-camera-controls/README.md)
- [Volume swipe](rabbit-r1-volume-swipe/README.md)
- [Bluetooth audio investigation](BLUETOOTH_AUDIO_INVESTIGATION.md)
- [Standard Bluetooth audio HAL notes](rabbit-r1-standard-bt-hal/README.md)
