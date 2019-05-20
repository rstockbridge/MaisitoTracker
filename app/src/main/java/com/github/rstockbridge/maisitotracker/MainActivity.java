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

import static com.github.rstockbridge.maisitotracker.Constants.TAG;
import static com.google.android.things.pio.Gpio.ACTIVE_HIGH;
import static com.google.android.things.pio.Gpio.DIRECTION_IN;
import static com.google.android.things.pio.Gpio.EDGE_BOTH;
import static java.util.Locale.US;

public class MainActivity extends AppCompatActivity {

    private static final String GPIO_1_NAME = "BCM24";
    private static final String GPIO_2_NAME = "BCM20";

    private Gpio gpio1;
    private Gpio gpio2;

    private SwitchState lastSwitchState;

    private final GpioCallback gpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(final Gpio gpio) {
            try {
                processGpioValue(gpio.getName(), gpio.getValue());
            } catch (final IOException e) {
                Log.e(TAG, "Unable to read value from GPIO " + gpio.getName(), e);
            }

            return true;
        }

        @Override
        public void onGpioError(final Gpio gpio, final int error) {
            Log.e(TAG, "GPIO error received: " + error);
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "Main Activity created");

        new PosterProvider().getPoster().post("An activity was created");

        gpio1 = configureGpio(GPIO_1_NAME);
        gpio2 = configureGpio(GPIO_2_NAME);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Main Activity destroyed");

        tearDownGpio(gpio1);
        tearDownGpio(gpio2);

        super.onDestroy();
    }

    // GPIO lifecycle

    @NonNull
    private Gpio configureGpio(@NonNull final String gpioName) {
        try {
            Log.d(TAG, "Configuring GPIO " + gpioName);

            final PeripheralManager manager = PeripheralManager.getInstance();
            final Gpio gpio = manager.openGpio(gpioName);
            gpio.setDirection(DIRECTION_IN);
            gpio.setActiveType(ACTIVE_HIGH);
            gpio.setEdgeTriggerType(EDGE_BOTH);
            gpio.registerGpioCallback(gpioCallback);

            processGpioValue(gpio.getName(), gpio.getValue());

            return gpio;
        } catch (final IOException e) {
            throw new RuntimeException("Unable to access GPIO " + gpioName);
        }
    }

    private void processGpioValue(@NonNull final String gpioName, final boolean gpioValue) {
        final SwitchState switchState = SwitchState.fromGpioValue(gpioValue);

        if (switchState.equals(lastSwitchState)) {
            return;
        }

        lastSwitchState = switchState;
        Log.d(TAG, "New switch state detected for GPIO " + gpioName + ": " + switchState.toString().toLowerCase(US));
    }

    private void tearDownGpio(@Nullable final Gpio gpio) {
        if (gpio != null) {
            gpio.unregisterGpioCallback(gpioCallback);

            try {
                gpio.close();
            } catch (final IOException e) {
                Log.e(TAG, "Unable to close GPIO " + gpio.getName(), e);
            }
        } else {
            Log.e(TAG, "GPIO never initialized; no teardown required.");
        }
    }

}
