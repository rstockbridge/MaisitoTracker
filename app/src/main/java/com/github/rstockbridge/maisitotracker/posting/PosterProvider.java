package com.github.rstockbridge.maisitotracker.posting;

import android.support.annotation.NonNull;

import com.github.rstockbridge.maisitotracker.BuildConfig;

public final class PosterProvider {

    @NonNull
    public Poster getPoster() {
        if (BuildConfig.DRY_RUN) {
            return new LogPoster();
        } else {
            return new TwitterPoster();
        }
    }

}
