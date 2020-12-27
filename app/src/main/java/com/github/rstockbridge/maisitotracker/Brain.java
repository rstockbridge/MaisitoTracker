package com.github.rstockbridge.maisitotracker;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static com.github.rstockbridge.maisitotracker.BuildConfig.COOLDOWN_M;
import static com.github.rstockbridge.maisitotracker.BuildConfig.HIGH_PRESSURE_TRIGGER_DURATION_S;
import static com.github.rstockbridge.maisitotracker.Constants.TAG;
import static com.github.rstockbridge.maisitotracker.Pressure.UNKNOWN;
import static java.util.concurrent.TimeUnit.MINUTES;
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
    private final Clock clock;

    @NonNull
    private final Storage storage;

    @NonNull
    private final OnShouldPostListener listener;

    @Nullable
    private ScheduledFuture pendingPost;

    public Brain(
            @NonNull final Clock clock,
            @NonNull final Storage storage,
            @NonNull final OnShouldPostListener listener
    ) {

        this.clock = clock;
        this.listener = listener;
        this.storage = storage;
    }

    void processGpioValue(final boolean gpioValue) {
        final Pressure newPressure = Pressure.fromGpioValue(gpioValue);

        if (newPressure.equals(UNKNOWN)) return;
        if (newPressure.equals(lastPressure)) return;

        lastPressure = newPressure;

        Log.d(TAG, "New pressure: " + newPressure.toString());

        if (lastPressure.equals(UNKNOWN)) return;

        final boolean catPresent = newPressure == Pressure.HIGH;
        final Long tweetMs = storage.getLastTweetTimeMs();
        final boolean coolingDown =
                tweetMs != null && (clock.nowMs() - tweetMs) <= MINUTES.toMillis(COOLDOWN_M);

        if (catPresent && pendingPost == null) {
            if (coolingDown) {
                Log.d(TAG, "Cat present but cooling down from last tweet; not scheduling post.");
            } else {
                Log.d(TAG, "Scheduling new post.");

                pendingPost = executor.schedule(
                        () -> {
                            Log.d(TAG, "Executing scheduled post.");
                            pendingPost = null;
                            listener.onShouldPost();
                            storage.setLastTweetTimeMs(clock.nowMs());
                        },
                        HIGH_PRESSURE_TRIGGER_DURATION_S,
                        SECONDS);
            }
        } else if (!catPresent && pendingPost != null) {
            Log.d(TAG, "Canceling scheduled post.");
            pendingPost.cancel(false);
            pendingPost = null;
        }
    }

}
