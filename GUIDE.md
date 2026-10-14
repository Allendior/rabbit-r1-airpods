# Set up Android camera, gestures, Hermes vision, and AirPods on Rabbit R1

This guide is written for a rooted Rabbit R1 running the R1 Escape/Lineage-based
Android 13 image. Every change has a rollback path and leaves the system and
vendor partitions untouched.

## Before you start

You need Magisk root and an ADB connection. The camera extension also needs
Vector Framework or another modern libxposed API 101-compatible framework.

Check the device build first:

```sh
adb shell getprop ro.build.fingerprint
adb shell getprop ro.lineage.version
```

The tested fingerprint begins with
`rabbit/gsi_r1/r1:13/TQ3A.230805.001.S1`. Keep a copy of the files in
[`downloads`](downloads/) and verify them before installation:

```sh
cd downloads
shasum -a 256 -c SHA256SUMS.txt
```

## 1. Add photo capture to Vision Eye

Install the camera controls APK:

```sh
adb install -r downloads/rabbit-r1-camera-controls-v1.0.0.apk
```

Enable **Rabbit R1 Camera Controls** in Vector, then scope it only to
`dev.r1native.shell`. Restart the launcher or reboot. Vector's command-line
equivalent is:

```sh
adb shell 'su -c "/data/adb/modules/zygisk_vector/cli modules enable dev.r1native.cameracontrols"'
adb shell 'su -c "/data/adb/modules/zygisk_vector/cli scope set dev.r1native.cameracontrols dev.r1native.shell/0"'
adb shell am force-stop dev.r1native.shell
adb shell am start -n dev.r1native.shell/.MainActivity
```

Open **EYE**, then open the camera. You now have two capture methods:

- tap **CAPTURE ◉**;
- short-press the R1 power/side button while Eye is open.

A short press saves the photo and leaves Eye open. Photos appear in Gallery
under `DCIM/RabbitR1` with names such as `R1_20261014_115343_974.jpg`.

The module uses the device orientation sensor. Portrait produces a portrait
JPEG; either landscape direction produces a landscape JPEG. It also corrects
the upside-down front-camera transform in the preview and in the shared frame
that reaches agents.

### Let Hermes inspect the camera view

Select Hermes as the current agent and keep Eye open. Hold the power/side
button, ask a question such as “What am I looking at?”, and release the button.
The launcher captures the current Eye frame and sends it as an OpenAI-compatible
`image_url` data payload with your transcript. The camera controls module fixes
the frame orientation before that step, so Hermes receives the same upright
view you would save to Gallery.

## 2. Add the top-edge volume gesture

Install `downloads/rabbit-r1-volume-swipe-v1.0.zip` in Magisk and reboot.

Start a horizontal swipe inside the top 100 pixels of the 480×640 screen:

- swipe left to lower media volume;
- swipe right to raise media volume.

The module reads the touchscreen only. It does not remap the hardware wheel, so
the wheel remains available for scrolling. Verify recognized gestures with:

```sh
adb shell 'su -c "tail -20 /data/local/tmp/r1_volume_swipe.log"'
```

## 3. Enable the Bluetooth audio profiles

Install `downloads/rabbit-r1-bluetooth-audio-profiles-v1.0.zip` in Magisk and
reboot. It enables A2DP source, HFP audio gateway, and AVRCP target at boot.

Pair and connect your AirPods, then check that Android selected them:

```sh
adb shell 'dumpsys bluetooth_manager | grep -E "mActiveDevice|mConnectionState"'
```

Pairing alone may still produce silence on the affected image. Continue with
the audio HAL repair when Android says connected but media does not play.

## 4. Repair silent A2DP media audio

The affected R1 image combines an Android 13 system with Rabbit's Android 12
MediaTek vendor partition. Its A2DP provider and vendor Bluetooth audio module
keep incompatible audio-session state, so AudioPolicy cannot open the media
output even though Bluetooth reports a connection.

The tested repair overlays only
`/vendor/lib64/hw/audio.bluetooth.default.so` with a compatible standard HIDL
Bluetooth audio module through Magisk. The source vendor partition stays
unchanged. This repository does not redistribute that third-party prebuilt
until its redistribution rights are confirmed.

For the working device, the active replacement had this SHA-256:

```text
bf69b46cde565a778ebf6fcaf2bf4c302ac289737d58f0320a590ed3f3282363
```

After installing a legally usable compatible module and rebooting, check:

```sh
adb shell getprop init.svc.vendor.audio-hal
adb shell 'su -c "sha256sum /vendor/lib64/hw/audio.bluetooth.default.so"'
```

The service should report `running`. Ordinary media should then play through
the AirPods.

## 5. Build LibrePods for Android 13

LibrePods v1.0.1-rc1 can crash when its battery broadcast reaches Android 13's
typed Parcelable API. Apply the included compatibility patch:

```sh
git clone https://github.com/librepods-org/librepods.git
cd librepods
git apply ../downloads/librepods-android13-parcelable.patch
cd android
./gradlew :app:assembleFossDebug
```

Install the resulting FOSS debug APK. Enable its Vector module and scope it to
`com.android.bluetooth`, then reboot. LibrePods should show battery information
and listening-mode controls after the AirPods connect.

## Roll back

### Camera controls

Disable **Rabbit R1 Camera Controls** in Vector, force-stop the launcher, then
start it again. Uninstall the extension when you no longer need it:

```sh
adb uninstall dev.r1native.cameracontrols
```

Existing photos in `DCIM/RabbitR1` remain in Gallery.

### Volume gesture

Disable or uninstall **Rabbit R1 Top-Swipe Volume** in Magisk and reboot.

### Bluetooth changes

Disable the corresponding Magisk module and reboot. To disable the tested
standard HAL overlay from ADB:

```sh
adb shell 'su -c "touch /data/adb/modules/rabbit_r1_standard_bt_hal/disable"'
adb reboot
```

The original vendor file returns automatically because the repair is an
overlay rather than a partition modification.
