package com.github.rstockbridge.maisitotracker.posting;

import android.support.annotation.NonNull;
import android.util.Log;

import static com.github.rstockbridge.maisitotracker.Constants.TAG;

public final class LogPoster implements Poster {

    @Override
    public void post(@NonNull final PostData postData) {
        Log.d(TAG, "[DEBUG] PostData " + postData + "posted.");
    }

}
