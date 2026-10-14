# Downloads

Download the file that matches the part of the setup you need.

| File | What you use it for |
| --- | --- |
| `rabbit-r1-controls-v1.1.0.apk` | Install this Vector/libxposed module for Eye capture, automatic orientation, upright Hermes frames, and live Hardware-page volume swipes. |
| `rabbit-r1-bluetooth-audio-profiles-v1.0.zip` | Install this ZIP in Magisk to enable the R1's A2DP, HFP, and AVRCP Bluetooth profiles at boot. |
| `librepods-android13-parcelable.patch` | Apply this patch to a LibrePods checkout before building the Android FOSS debug APK. |

Verify each file with `shasum -a 256 -c SHA256SUMS.txt` before you use it.

The camera APK needs Vector Framework or another compatible libxposed framework.

The A2DP audio-HAL binary is deliberately absent. Read the repository's
[guide](../GUIDE.md) for the compatibility and redistribution details.
