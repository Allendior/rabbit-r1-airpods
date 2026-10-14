# Control volume with a top-edge swipe on your Rabbit R1

This Magisk module watches the R1 touchscreen without taking control of it.
Start a horizontal swipe in the top 100 pixels of the 480×640 display:

- swipe left to lower media volume;
- swipe right to raise media volume.

Longer swipes change more volume steps, up to five. Android's normal volume
panel appears after the gesture. The listener reads only touchscreen device
`/dev/input/event3`; it does not read or remap the scroll wheel at
`/dev/input/event2`, so your wheel remains dedicated to scrolling.

## Install

Install `rabbit-r1-volume-swipe-v1.0.zip` in Magisk and reboot. The listener
starts after Android finishes booting.

## Verify

Swipe horizontally from the top edge. You can inspect the listener log with:

```sh
adb shell 'su -c "tail -20 /data/local/tmp/r1_volume_swipe.log"'
```

Each recognized gesture writes `gesture=raise` or `gesture=lower` and the
number of volume steps.

## Remove

Disable or uninstall `Rabbit R1 Top-Swipe Volume` in Magisk and reboot. The
module does not modify the system or vendor partitions.
