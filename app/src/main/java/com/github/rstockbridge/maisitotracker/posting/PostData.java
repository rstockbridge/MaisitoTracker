package com.github.rstockbridge.maisitotracker.posting;

import android.support.annotation.NonNull;

import java.io.InputStream;

public final class PostData {

    @NonNull
    public final String body;

    @NonNull
    public final InputStream imageStream;

    public PostData(@NonNull final String body, @NonNull final InputStream imageStream) {
        this.body = body;
        this.imageStream = imageStream;
    }

    @Override
    public String toString() {
        return "PostData{" +
                "body='" + body + '\'' +
                ", imageStream=" + imageStream +
                '}';
    }

}
