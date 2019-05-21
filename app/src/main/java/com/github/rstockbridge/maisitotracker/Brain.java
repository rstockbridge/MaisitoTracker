package com.github.rstockbridge.maisitotracker;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.rstockbridge.maisitotracker.posting.Poster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.github.rstockbridge.maisitotracker.Constants.TAG;
import static com.github.rstockbridge.maisitotracker.SwitchState.UNKNOWN;

final class Brain {

    @NonNull
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    @NonNull
    private final Map<String, SwitchState> switchStates = new HashMap<>();

    @NonNull
    private final Poster poster;

    @Nullable
    private ScheduledFuture postFuture;

    Brain(@NonNull final List<String> gpioNames, @NonNull final Poster poster) {
        for (final String gpioName : gpioNames) {
            switchStates.put(gpioName, UNKNOWN);
        }

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

        // todo if new state means at least half of switches are closed and no action is currently
        //  scheduled, schedule action to post at least 30s in the future

        final int newClosedSwitchCount = getClosedSwitchCount();

        if (postFuture == null && shouldSchedulePost(newClosedSwitchCount)) {
            postFuture = executor.schedule(new Runnable() {
                @Override
                public void run() {
                    poster.post("Maisy's in her heated bed!");
                }
            }, 30, TimeUnit.SECONDS);
        }

        // todo if new state means fewer than half of switches are engaged and action is currently
        //  scheduled, cancel pending action

        if (postFuture != null && shouldCancelPost(newClosedSwitchCount)) {
            postFuture.cancel(false);
        }

        // todo never post within 5 minutes of previous post


    }

    private boolean shouldSchedulePost(final int newSwitchClosedCount) {
        return newSwitchClosedCount >= (int) Math.ceil(switchStates.size() / 2.0);
    }

    private boolean shouldCancelPost(final int newSwitchClosedCount) {
        return newSwitchClosedCount < (int) Math.ceil(switchStates.size() / 2.0);
    }

    private int getClosedSwitchCount() {
        int result = 0;

        for (final SwitchState switchState : switchStates.values()) {
            if (switchState == SwitchState.CLOSED) {
                result++;
            }
        }

        return result;
    }

}
