package com.github.rstockbridge.maisitotracker;

import android.os.Bundle;
import android.support.annotation.NonNull;
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

    private GpioCallback gpioCallback = new GpioCallback() {
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

        configureGpio(gpio1, GPIO_1_NAME);
        configureGpio(gpio2, GPIO_2_NAME);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Main Activity destroyed");

        if (gpio1 != null) {
            gpio1.unregisterGpioCallback(gpioCallback);

            try {
                gpio1.close();
                gpio1 = null;
            } catch (final IOException e) {
                Log.e(TAG, "Unable to close GPIO " + GPIO_1_NAME, e);
            }
        }

        if (gpio2 != null) {
            gpio2.unregisterGpioCallback(gpioCallback);

            try {
                gpio2.close();
                gpio2 = null;
            } catch (final IOException e) {
                Log.e(TAG, "Unable to close GPIO " + GPIO_2_NAME, e);
            }
        }

        super.onDestroy();
    }

    private void configureGpio(Gpio gpio, @NonNull final String gpioName) {
        try {
            Log.d(TAG, "Configuring GPIO " + gpioName);

            final PeripheralManager manager = PeripheralManager.getInstance();
            gpio = manager.openGpio(gpioName);
            gpio.setDirection(DIRECTION_IN);
            gpio.setActiveType(ACTIVE_HIGH);
            gpio.setEdgeTriggerType(EDGE_BOTH);
            gpio.registerGpioCallback(gpioCallback);

            processGpioValue(gpio.getName(), gpio.getValue());
        } catch (final IOException e) {
            Log.e(TAG, "Unable to access GPIO " + gpio.getName(), e);
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

}
