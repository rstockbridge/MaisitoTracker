package com.github.rstockbridge.maisitotracker;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static com.github.rstockbridge.maisitotracker.BuildConfig.HIGH_PRESSURE_TRIGGER_DURATION_S;
import static com.github.rstockbridge.maisitotracker.Constants.TAG;
import static com.github.rstockbridge.maisitotracker.Pressure.UNKNOWN;
import static java.util.concurrent.TimeUnit.SECONDS;

final class Brain {

    public interface OnShouldPostListener {
        void onShouldPost();
    }

    @NonNull
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    @NonNull
    private Pressure lastPressure = UNKNOWN;

    @NonNull
    private final OnShouldPostListener listener;

    @Nullable
    private ScheduledFuture pendingPost;

    public Brain(@NonNull final OnShouldPostListener listener) {
        this.listener = listener;
    }

    void processGpioValue(final boolean gpioValue) {
        final Pressure newPressure = Pressure.fromGpioValue(gpioValue);

        if (newPressure.equals(UNKNOWN)) return;
        if (newPressure.equals(lastPressure)) return;

        lastPressure = newPressure;

        Log.d(TAG, "New pressure: " + newPressure.toString());

        if (lastPressure.equals(UNKNOWN)) return;

        final boolean catPresent = newPressure == Pressure.HIGH;

        if (catPresent && pendingPost == null) {
            Log.d(TAG, "Scheduling new post.");

            pendingPost = executor.schedule(
                    () -> {
                        Log.d(TAG, "Executing scheduled post.");
                        pendingPost = null;
                        listener.onShouldPost();
                    },
                    HIGH_PRESSURE_TRIGGER_DURATION_S,
                    SECONDS);
        } else if (!catPresent && pendingPost != null) {
            Log.d(TAG, "Canceling scheduled post.");
            pendingPost.cancel(false);
            pendingPost = null;
        }

        // todo never post within 5 minutes of previous post
    }

}
