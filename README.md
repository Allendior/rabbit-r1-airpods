# Use AirPods with LibrePods on a rooted Rabbit R1

If your Rabbit R1 pairs with AirPods but media stays silent, this repository
documents the fix used on the R1 Escape Android 13 image. It also covers the
LibrePods Android 13 crash that can prevent battery and listening-mode controls
from working.

You will enable Android's Bluetooth audio profiles, repair the A2DP audio path,
and install LibrePods with the Android 13 compatibility patch. The fix was
verified after a reboot with AirPods connected as the active A2DP device and
media playing through them.

## Is this for your R1?

Use this guide if your R1 is rooted with Magisk, runs the R1 Escape Android 13
image, and has Rabbit's Android 12 MediaTek vendor partition. The tested build
fingerprint begins with `rabbit/gsi_r1/r1:13/TQ3A.230805.001.S1`.

If you run a different ROM or vendor image, start with the checks in the guide
before installing anything. The audio HAL fix is specific to the session-library
mismatch found on this image.

## Start here

Follow [GUIDE.md](GUIDE.md) for the setup, checks, and rollback steps. Read
[BLUETOOTH_AUDIO_INVESTIGATION.md](BLUETOOTH_AUDIO_INVESTIGATION.md) when you
want the technical cause, evidence, and alternatives that were ruled out.

## What you can use here

- `rabbit-r1-bluetooth-audio/rabbit-r1-bluetooth-audio.zip` enables the A2DP,
  HFP, and AVRCP profiles your R1 needs at boot.
- `patches/librepods-android13-parcelable.patch` fixes the Android 13 battery
  broadcast crash in a LibrePods checkout.
- `rabbit-r1-standard-bt-hal/` explains the A2DP repair and how to verify it.
  Its third-party prebuilt binary is intentionally not included in this public
  repository until redistribution rights are confirmed.

## Downloads

Open [downloads](downloads/) for ready-to-use files and their SHA-256 checksums.
