package com.github.rstockbridge.maisitotracker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SensorManager.DynamicSensorCallback;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

//    private static final String TAG = MainActivity.class.getSimpleName();
//
//    private SensorManager mSensorManager;
//    private TemperaturePressureEventListener mSensorEventListener;
//    private DynamicSensorCallback mDynamicSensorCallback = new DynamicSensorCallback() {
//        @Override
//        public void onDynamicSensorConnected(Sensor sensor) {
//            if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
//                Log.i(TAG, "Temperature sensor connected");
//                mSensorEventListener = new TemperaturePressureEventListener();
//                mSensorManager.registerListener(mSensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
//            }
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("maisito-tracker", "Main Activity created");

//        startTemperaturePressureRequest();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d("maisito-tracker", "Main Activity started");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("maisito-tracker", "Main Activity resumed");
    }

    @Override
    protected void onPause() {
        Log.d("maisito-tracker", "Main Activity paused");

        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d("maisito-tracker", "Main Activity stopped");

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d("maisito-tracker", "Main Activity destroyed");

        super.onDestroy();
//        stopTemperaturePressureRequest();
    }

//    private void startTemperaturePressureRequest() {
//        this.startService(new Intent(this, PressureSensorService.class));
//        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        mSensorManager.registerDynamicSensorCallback(mDynamicSensorCallback);
//    }
//
//    private void stopTemperaturePressureRequest() {
//        this.stopService(new Intent(this, PressureSensorService.class));
//        mSensorManager.unregisterDynamicSensorCallback(mDynamicSensorCallback);
//        mSensorManager.unregisterListener(mSensorEventListener);
//    }
//
//    private class TemperaturePressureEventListener implements SensorEventListener {
//        @Override
//        public void onSensorChanged(SensorEvent event) {
//            Log.i(TAG, "sensor changed: " + event.values[0]);
//        }
//
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {
//            Log.i(TAG, "sensor accuracy changed: " + accuracy);
//        }
//    }
}
