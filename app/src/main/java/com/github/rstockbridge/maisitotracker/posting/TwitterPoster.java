package com.github.rstockbridge.maisitotracker.posting;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Date;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import static com.github.rstockbridge.maisitotracker.BuildConfig.ACCESS_TOKEN;
import static com.github.rstockbridge.maisitotracker.BuildConfig.ACCESS_TOKEN_SECRET;
import static com.github.rstockbridge.maisitotracker.BuildConfig.CONSUMER_KEY;
import static com.github.rstockbridge.maisitotracker.BuildConfig.CONSUMER_SECRET;
import static com.github.rstockbridge.maisitotracker.Constants.TAG;

public final class TwitterPoster implements Poster {

    @NonNull
    private final Twitter twitter;

    TwitterPoster() {
        twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
        twitter.setOAuthAccessToken(new AccessToken(ACCESS_TOKEN, ACCESS_TOKEN_SECRET));
    }

    @Override
    public void post(@NonNull final String message) {
        new TweetTask(twitter).execute(message);
    }

    private static final class TweetTask extends AsyncTask<String, Void, Void> {

        @NonNull
        private final Twitter twitter;

        private TweetTask(@NonNull final Twitter twitter) {
            this.twitter = twitter;
        }

        @Override
        protected Void doInBackground(final String... message) {
            try {
                final String timeStamp = String.valueOf(new Date().getTime());
                twitter.updateStatus(message[0] + " @ " + timeStamp + "!");
            } catch (final TwitterException e) {
                Log.e(TAG, "Exception while attempting to tweet:", e);
            }

            return null;
        }

    }

}
