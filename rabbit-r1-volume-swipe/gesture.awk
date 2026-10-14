function hex_to_dec(hex,    digits, value, i, digit) {
    digits = "0123456789abcdef"
    hex = tolower(hex)
    sub(/^0x/, "", hex)
    value = 0
    for (i = 1; i <= length(hex); i++) {
        digit = index(digits, substr(hex, i, 1)) - 1
        if (digit < 0) {
            return 0
        }
        value = (value * 16) + digit
    }
    return value
}

function absolute(value) {
    return value < 0 ? -value : value
}

function change_volume(direction, steps,    i, command) {
    for (i = 1; i <= steps; i++) {
        command = "/system/bin/cmd media_session volume --stream 3 --adj " direction
        if (i == steps) {
            command = command " --show"
        }
        system(command " >/dev/null 2>&1")
    }
    print "gesture=" direction " steps=" steps
    fflush()
}

/ABS_MT_TRACKING_ID/ {
    tracking_id = tolower($NF)

    if (tracking_id == "ffffffff") {
        if (active && have_x && have_y && start_y <= 100) {
            delta_x = current_x - start_x
            delta_y = current_y - start_y
            distance_x = absolute(delta_x)
            distance_y = absolute(delta_y)

            if (distance_x >= 70 && distance_x > (distance_y * 1.4)) {
                steps = int(distance_x / 75)
                if (steps < 1) steps = 1
                if (steps > 5) steps = 5
                change_volume(delta_x > 0 ? "raise" : "lower", steps)
            }
        }

        active = 0
        have_x = 0
        have_y = 0
    } else {
        active = 1
        have_x = 0
        have_y = 0
    }
    next
}

active && /ABS_MT_POSITION_X/ {
    current_x = hex_to_dec($NF)
    if (!have_x) {
        start_x = current_x
        have_x = 1
    }
    next
}

active && /ABS_MT_POSITION_Y/ {
    current_y = hex_to_dec($NF)
    if (!have_y) {
        start_y = current_y
        have_y = 1
    }
    next
}
