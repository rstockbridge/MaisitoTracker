package com.github.rstockbridge.maisitotracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.github.rstockbridge.maisitotracker.posting.PostData;
import com.github.rstockbridge.maisitotracker.posting.PosterProvider;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.github.rstockbridge.maisitotracker.Constants.TAG;
import static com.google.android.things.pio.Gpio.ACTIVE_HIGH;
import static com.google.android.things.pio.Gpio.DIRECTION_IN;
import static com.google.android.things.pio.Gpio.EDGE_BOTH;

public class MainActivity extends AppCompatActivity implements Brain.OnShouldPostListener {

    interface OnCustomImageAvailableListener {
        void onImageAvailable(@NonNull ImageReader imageReader, boolean shouldPost);
    }

    //region Views

    private ImageView imageView;

    //endregion

    //region Lifecycle methods

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "MainActivity::onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_view);

        findViewById(R.id.take_picture_button).setOnClickListener(view ->
                MaisitoCamera.instance.takePicture(/*shouldPost=*/false)
        );

        brain = new Brain(new SystemClock(), new SharedPrefsStorage(this), this);

        gpio = configureGpio();
        processCurrentGpioValue(gpio);

        cameraThread = new HandlerThread("CameraBackground");
        cameraThread.start();
        Handler cameraHandler = new Handler(cameraThread.getLooper());

        MaisitoCamera.instance.initialize(
                this,
                cameraHandler,
                onImageAvailableListener
        );

        tearingDown = false;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "MainActivity::onDestroy");

        tearingDown = true;

        tearDownCamera();
        tearDownGpio();

        super.onDestroy();
    }

    //endregion

    //region GPIO

    private static final String GPIO_NAME = "BCM24";

    private Gpio gpio = null;
    private boolean tearingDown = false;

    private final GpioCallback gpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(final Gpio gpio) {
            if (tearingDown) {
                return false;
            } else {
                processCurrentGpioValue(gpio);
                return true;
            }
        }

        @Override
        public void onGpioError(final Gpio gpio, final int error) {
            if (tearingDown) {
                return;
            }

            Log.e(TAG, "GPIO error received: " + error);
        }
    };

    @NonNull
    private Gpio configureGpio() {
        try {
            Log.d(TAG, "Configuring " + GPIO_NAME);

            final PeripheralManager manager = PeripheralManager.getInstance();
            final Gpio gpio = manager.openGpio(GPIO_NAME);
            gpio.setDirection(DIRECTION_IN);
            gpio.setActiveType(ACTIVE_HIGH);
            gpio.setEdgeTriggerType(EDGE_BOTH);
            gpio.registerGpioCallback(gpioCallback);
            return gpio;
        } catch (final IOException e) {
            throw new RuntimeException("Unable to access " + GPIO_NAME);
        }
    }

    private void processCurrentGpioValue(final Gpio gpio) {
        try {
            brain.processGpioValue(gpio.getValue());
        } catch (final IOException e) {
            Log.e(TAG, "Unable to read value from " + GPIO_NAME, e);
        }
    }

    private void tearDownGpio() {
        if (gpio != null) {
            gpio.unregisterGpioCallback(gpioCallback);

            try {
                gpio.close();
                gpio = null;
            } catch (final IOException e) {
                Log.e(TAG, "Unable to close GPIO " + gpio.getName(), e);
            }
        } else {
            Log.e(TAG, "GPIO never initialized; no teardown required.");
        }
    }

    //endregion

    //region Camera

    private HandlerThread cameraThread;

    private final OnCustomImageAvailableListener onImageAvailableListener =
            new OnCustomImageAvailableListener() {
                @Override
                public void onImageAvailable(
                        @NonNull final ImageReader reader,
                        final boolean shouldPost
                ) {

                    final Image image = reader.acquireLatestImage();
                    final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    final byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    image.close();

                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    new Handler(Looper.getMainLooper())
                            .post(() -> imageView.setImageBitmap(bitmap));

                    if (shouldPost) {
                        final PostData postData = new PostData(
                                "Maisy has entered her heated bed!",
                                new ByteArrayInputStream(bytes)
                        );

                        new PosterProvider().getPoster().post(postData);
                    }
                }
            };

    private void tearDownCamera() {
        cameraThread.quitSafely();
        MaisitoCamera.instance.shutDown();
    }

    //endregion

    //region Brain

    private Brain brain;

    @Override
    public void onShouldPost() {
        new Handler(Looper.getMainLooper())
                .post(() -> MaisitoCamera.instance.takePicture(/*shouldPost=*/true));
    }

    //endregion

}
