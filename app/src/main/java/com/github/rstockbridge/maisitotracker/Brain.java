package com.github.rstockbridge.maisitotracker;

import android.support.annotation.NonNull;
import android.util.Log;

import com.github.rstockbridge.maisitotracker.posting.Poster;

import java.util.HashMap;
import java.util.Map;

import static com.github.rstockbridge.maisitotracker.Constants.TAG;

final class Brain {

    @NonNull
    private final Map<String, SwitchState> switchStates = new HashMap<>();

    @NonNull
    private final Poster poster;

    Brain(@NonNull final Poster poster) {
        this.poster = poster;
    }

    void processGpioValue(@NonNull final String gpioName, final boolean gpioValue) {
        final SwitchState lastSwitchState = switchStates.get(gpioName);
        final SwitchState newSwitchState = SwitchState.fromGpioValue(gpioValue);

        if (newSwitchState.equals(lastSwitchState)) {
            return;
        }

        switchStates.put(gpioName, newSwitchState);
        Log.d(TAG, "New switch state for GPIO " + gpioName + ": " + newSwitchState.toString());
    }

}
