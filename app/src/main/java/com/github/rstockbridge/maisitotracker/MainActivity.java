package com.github.rstockbridge.maisitotracker;

import android.os.Bundle;
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

    private static final String GPIO_NAME = "BCM24";

    private Gpio gpio;

    private SwitchState lastSwitchState;

    private GpioCallback gpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(final Gpio gpio) {
            try {
                processGpioValue(gpio.getValue());
            } catch (final IOException e) {
                Log.e(TAG, "Unable to read value from GPIO " + GPIO_NAME, e);
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

        try {
            Log.d(TAG, "Configuring GPIO " + GPIO_NAME);

            final PeripheralManager manager = PeripheralManager.getInstance();
            gpio = manager.openGpio(GPIO_NAME);
            gpio.setDirection(DIRECTION_IN);
            gpio.setActiveType(ACTIVE_HIGH);
            gpio.setEdgeTriggerType(EDGE_BOTH);
            gpio.registerGpioCallback(gpioCallback);

            processGpioValue(gpio.getValue());
        } catch (final IOException e) {
            Log.e(TAG, "Unable to access GPIO " + GPIO_NAME, e);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Main Activity destroyed");

        if (gpio != null) {
            gpio.unregisterGpioCallback(gpioCallback);

            try {
                gpio.close();
                gpio = null;
            } catch (final IOException e) {
                Log.e(TAG, "Unable to close GPIO " + GPIO_NAME, e);
            }
        }

        super.onDestroy();
    }

    private void processGpioValue(final boolean gpioValue) {
        final SwitchState switchState = SwitchState.fromGpioValue(gpioValue);

        if (switchState.equals(lastSwitchState)) {
            return;
        }

        lastSwitchState = switchState;
        Log.d(TAG, "New switch state detected: " + switchState.toString().toLowerCase(US));
    }

}
