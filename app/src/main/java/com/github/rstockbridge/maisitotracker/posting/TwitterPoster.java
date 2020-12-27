package com.github.rstockbridge.maisitotracker.posting;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.UUID;

import twitter4j.StatusUpdate;
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
    public void post(@NonNull final PostData postData) {
        new TweetTask(twitter).execute(postData);
    }

    private static final class TweetTask extends AsyncTask<PostData, Void, Void> {

        @NonNull
        private final Twitter twitter;

        private TweetTask(@NonNull final Twitter twitter) {
            this.twitter = twitter;
        }

        @Override
        protected Void doInBackground(final PostData... allPostData) {
            try {
                final PostData postData = allPostData[0];
                final String imageFileName = UUID.randomUUID().toString();

                final StatusUpdate statusUpdate = new StatusUpdate(postData.body);
                statusUpdate.setMedia(imageFileName, postData.imageStream);

                twitter.updateStatus(statusUpdate);
            } catch (final TwitterException e) {
                Log.e(TAG, "Exception while attempting to tweet.", e);
            }

            return null;
        }

    }

}
