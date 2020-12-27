package com.github.rstockbridge.maisitotracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.content.Context.MODE_PRIVATE;
import static com.github.rstockbridge.maisitotracker.Constants.PREFS_NAME;

public final class SharedPrefsStorage implements Storage {

    private static final String LAST_TWEET_TIME_KEY = "LastTweetTime";
    private static final long MISSING_VALUE = Long.MIN_VALUE;

    private final SharedPreferences prefs;

    public SharedPrefsStorage(@NonNull final Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    @Override
    public void setLastTweetTimeMs(long timeMs) {
        prefs.edit()
                .putLong(LAST_TWEET_TIME_KEY, timeMs)
                .apply();
    }

    @Nullable
    @Override
    public Long getLastTweetTimeMs() {
        final long stored = prefs.getLong(LAST_TWEET_TIME_KEY, MISSING_VALUE);

        if (stored != MISSING_VALUE) {
            return stored;
        } else {
            return null;
        }
    }

}
