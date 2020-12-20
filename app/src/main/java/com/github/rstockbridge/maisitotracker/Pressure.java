package com.github.rstockbridge.maisitotracker;

import android.support.annotation.NonNull;

public enum Pressure {

    LOW, HIGH, UNKNOWN;

    @NonNull
    public static Pressure fromGpioValue(final boolean value) {
        return value ? HIGH : LOW;
    }

}
