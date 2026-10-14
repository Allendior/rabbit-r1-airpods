# LibrePods and Bluetooth audio on a rooted Rabbit R1

This is a tested setup for the Rabbit R1 Escape Android 13 image paired with
AirPods Pro. It enables Android Bluetooth audio profiles, makes LibrePods work
on Android 13, and fixes the R1's A2DP media-audio failure without reflashing
the vendor partition.

The final state was verified after a reboot: the AirPods were connected as the
active A2DP device, LibrePods started at boot, and media audio played through
the AirPods.

## Start here

Read [GUIDE.md](GUIDE.md) for prerequisites, installation, verification,
rollback, and an agent-friendly diagnostic sequence. The technical evidence and
rejected alternatives are in [BLUETOOTH_AUDIO_INVESTIGATION.md](BLUETOOTH_AUDIO_INVESTIGATION.md).

## Included artifacts

- `rabbit-r1-bluetooth-audio/rabbit-r1-bluetooth-audio.zip` enables A2DP,
  HFP, and AVRCP profiles at boot.
- `rabbit-r1-standard-bt-hal/` documents the A2DP repair. Its prebuilt HAL is
  intentionally excluded from public publication pending redistribution-rights
  confirmation.
- `patches/librepods-android13-parcelable.patch` is the small Android 13 fix
  for a current LibrePods checkout.

The original LibrePods APK is kept locally as `librepods-original.apk` for
rollback. It should not be published in a public repository unless its license
allows redistribution.
