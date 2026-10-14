#!/system/bin/sh

MODDIR=${0%/*}
PIDFILE=/data/local/tmp/r1_volume_swipe.pid
LOGFILE=/data/local/tmp/r1_volume_swipe.log

if [ -f "$PIDFILE" ]; then
    OLD_PID=$(cat "$PIDFILE" 2>/dev/null)
    if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
        exit 0
    fi
fi

(
    while [ "$(getprop sys.boot_completed)" != "1" ]; do
        sleep 2
    done

    while true; do
        /system/bin/getevent -lt /dev/input/event3 |
            /system/bin/awk -f "$MODDIR/gesture.awk"
        sleep 1
    done
) >>"$LOGFILE" 2>&1 &

echo $! >"$PIDFILE"
