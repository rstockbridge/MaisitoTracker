package com.github.rstockbridge.maisitotracker;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.rstockbridge.maisitotracker.posting.PosterProvider;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.rstockbridge.maisitotracker.Constants.TAG;
import static com.google.android.things.pio.Gpio.ACTIVE_HIGH;
import static com.google.android.things.pio.Gpio.DIRECTION_IN;
import static com.google.android.things.pio.Gpio.EDGE_BOTH;

public class MainActivity extends AppCompatActivity {

    private final Brain brain = new Brain(new PosterProvider().getPoster());

    // Activity lifecycle

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "Main Activity created");

        gpio = configureGpio();
        processCurrentGpioValue(gpio);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Main Activity destroyed");

        tearingDown = true;
        tearDownGpio();

        super.onDestroy();
    }

    // GPIO state + methods

    private static final String GPIO_NAME = "BCM24";

    private Gpio gpio = null;

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

    private boolean tearingDown = false;

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

}
