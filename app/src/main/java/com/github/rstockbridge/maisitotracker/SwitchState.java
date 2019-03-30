package com.github.rstockbridge.maisitotracker;

import android.support.annotation.NonNull;

public enum SwitchState {

    OPEN, CLOSED;

    @NonNull
    public static SwitchState fromGpioValue(final boolean value) {
        return value ? CLOSED : OPEN;
    }

}