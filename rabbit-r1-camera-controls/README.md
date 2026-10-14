# Rabbit R1 Eye camera controls

This libxposed module extends `dev.r1native.shell` without replacing or
re-signing the launcher APK.

## Features

- adds a **CAPTURE ◉** button to Vision Eye;
- maps a short power/side-button press to photo capture while Eye is open;
- keeps Eye open after the press;
- saves JPEG files through MediaStore to `DCIM/RabbitR1`;
- corrects the upside-down Eye preview;
- rotates captured frames from the orientation sensor for portrait and both
  landscape directions;
- corrects the same frame before the launcher's Hermes vision pipeline receives
  it.

## Why this is a module

Launcher 0.3.0 already contains `EyeCamera.capture(FrameCallback)` and the agent
vision flow, but its Eye page exposes only **FLIP** and **CLOSE**. A short button
press is also sent to the page as `sideClick`, which closes Eye. The module
hooks those existing paths, adds the missing UI, and saves through Android's
MediaStore. The original launcher certificate and app data stay intact.

The stock `EyeCamera.upright()` calculation uses a fixed sensor/motor rotation.
It does not include the device orientation, and its front preview applies a
vertical flip. The module replaces the JPEG calculation with:

- front/motor-facing camera: `sensor + device rotation`;
- rear-facing camera: `sensor - device rotation`.

## Build

```sh
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Install

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell 'su -c "/data/adb/modules/zygisk_vector/cli modules enable dev.r1native.cameracontrols"'
adb shell 'su -c "/data/adb/modules/zygisk_vector/cli scope set dev.r1native.cameracontrols dev.r1native.shell/0"'
```

Restart `dev.r1native.shell` or reboot. The module targets libxposed API 101 and
was tested with Vector Framework 2.2/API 102 on Android 13.

## Verify

Open Eye and take one portrait and one landscape photo. Check Gallery or:

```sh
adb shell 'ls -lt /storage/emulated/0/DCIM/RabbitR1 | head'
adb logcat -d | grep R1CameraControls
```

The log records the device rotation, JPEG rotation, MediaStore URI, and byte
count without recording image content.
