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
import static com.github.rstockbridge.maisitotracker.Pressure.UNKNOWN;

final class Brain {

    @NonNull
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    @NonNull
    private Pressure lastPressure = UNKNOWN;

    @NonNull
    private final Poster poster;

    @Nullable
    private ScheduledFuture pendingPost;

    Brain(@NonNull final Poster poster) {
        this.poster = poster;
    }

    void processGpioValue(final boolean gpioValue) {
        final Pressure newPressure = Pressure.fromGpioValue(gpioValue);

        Log.d(TAG, "New pressure: " + newPressure.toString());

        if (newPressure.equals(UNKNOWN)) return;
        if (newPressure.equals(lastPressure)) return;

        lastPressure = newPressure;

        if (lastPressure.equals(UNKNOWN)) return;

        final boolean catPresent = newPressure == Pressure.HIGH;

        if (catPresent && pendingPost == null) {
            pendingPost = executor.schedule(new Runnable() {
                @Override
                public void run() {
                    poster.post("Maisy's in her heated bed!");
                }
            }, 30, TimeUnit.SECONDS);
        } else if (!catPresent && pendingPost != null) {
            pendingPost.cancel(false);
        }

        // todo never post within 5 minutes of previous post
    }

}
