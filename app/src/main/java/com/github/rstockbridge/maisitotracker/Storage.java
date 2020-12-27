package com.github.rstockbridge.maisitotracker;

import android.support.annotation.Nullable;

public interface Storage {
    void setLastTweetTimeMs(long timeMs);

    @Nullable
    Long getLastTweetTimeMs();
}
