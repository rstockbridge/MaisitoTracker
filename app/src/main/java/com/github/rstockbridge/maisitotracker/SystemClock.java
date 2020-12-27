package com.github.rstockbridge.maisitotracker;

public final class SystemClock implements Clock {
    @Override
    public long nowMs() {
        return System.currentTimeMillis();
    }
}
