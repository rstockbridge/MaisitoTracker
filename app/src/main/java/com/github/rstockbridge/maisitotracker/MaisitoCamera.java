package com.github.rstockbridge.maisitotracker;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.rstockbridge.maisitotracker.MainActivity.OnCustomImageAvailableListener;

import java.util.Collections;

import static android.content.Context.CAMERA_SERVICE;
import static android.graphics.ImageFormat.JPEG;
import static android.hardware.camera2.CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE;
import static android.hardware.camera2.CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP;
import static android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON;
import static android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE;
import static com.github.rstockbridge.maisitotracker.Constants.TAG;

public final class MaisitoCamera {

    private static final int IMAGE_WIDTH = 1640;
    private static final int IMAGE_HEIGHT = 1232;
    private static final int MAX_IMAGES = 1;

    public static final MaisitoCamera instance = new MaisitoCamera();

    private CameraDevice device;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;

    private final CameraDevice.StateCallback deviceStateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull final CameraDevice cameraDevice) {
                    Log.d(TAG, "Opened camera.");
                    MaisitoCamera.this.device = cameraDevice;
                }

                @Override
                public void onDisconnected(@NonNull final CameraDevice cameraDevice) {
                    Log.d(TAG, "Camera disconnected, closing.");
                    cameraDevice.close();
                }

                @Override
                public void onError(@NonNull final CameraDevice cameraDevice, int i) {
                    Log.d(TAG, "Camera device error, closing.");
                    cameraDevice.close();
                }

                @Override
                public void onClosed(@NonNull final CameraDevice cameraDevice) {
                    Log.d(TAG, "Closed camera, releasing.");
                    MaisitoCamera.this.device = null;
                }
            };

    private final CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureProgressed(
                        @NonNull final CameraCaptureSession session,
                        @NonNull final CaptureRequest request,
                        @NonNull final CaptureResult partialResult
                ) {

                    Log.d(TAG, "Partial capture result received.");
                }

                @Override
                public void onCaptureCompleted(
                        @NonNull final CameraCaptureSession session,
                        @NonNull final CaptureRequest request,
                        @NonNull final TotalCaptureResult result
                ) {

                    Log.d(TAG, "Capture completed; closing and clearing CaptureSession.");
                    session.close();
                    shouldPost = false;
                    captureSession = null;
                }
            };

    private MaisitoCamera() {
        // This constructor intentionally left blank.
    }

    public void initialize(
            @NonNull final Context context,
            @NonNull final Handler backgroundHandler,
            @NonNull final OnCustomImageAvailableListener imageAvailableListener
    ) {

        // Discover the camera instance
        final CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        String[] camIds = {};

        try {
            camIds = manager.getCameraIdList();
        } catch (final CameraAccessException e) {
            Log.e(TAG, "Camera access exception when reading camera IDs.", e);
        }

        if (camIds.length < 1) {
            Log.e(TAG, "No cameras found.");
            return;
        }

        final String id = camIds[0];
        Log.d(TAG, "Using camera id " + id);

        // Initialize the image processor
        imageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, JPEG, MAX_IMAGES);
        imageReader.setOnImageAvailableListener(
                imageReader -> imageAvailableListener.onImageAvailable(imageReader, shouldPost),
                backgroundHandler
        );

        // Open the camera resource
        try {
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
            Log.d(TAG, "Camera comp step: " + characteristics.get(CONTROL_AE_COMPENSATION_STEP));
            Log.d(TAG, "Camera comp range: " + characteristics.get(CONTROL_AE_COMPENSATION_RANGE));
            manager.openCamera(id, deviceStateCallback, backgroundHandler);
        } catch (final CameraAccessException e) {
            Log.e(TAG, "Camera access exception when opening camera.", e);
        }
    }

    private boolean shouldPost = false;

    /**
     * Begin a still image capture
     */
    @MainThread
    public void takePicture(final boolean shouldPost) {
        if (device == null) {
            Log.e(TAG, "Cannot capture image. Camera not initialized.");
            return;
        }

        try {
            this.shouldPost = shouldPost;

            Log.d(TAG, "Creating capture session.");

            device.createCaptureSession(
                    Collections.singletonList(imageReader.getSurface()),
                    sessionCallback,
                    null
            );
        } catch (final CameraAccessException e) {
            this.shouldPost = false;

            Log.e(TAG, "Camera access exception while creating capture session.", e);
        }
    }

    /**
     * Callback handling session state changes
     */
    private final CameraCaptureSession.StateCallback sessionCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull final CameraCaptureSession session) {
                    if (device == null) {
                        Log.e(TAG, "Camera already closed; not triggering capture for session " + session);
                        return;
                    }

                    // When the session is ready, we start capture.
                    captureSession = session;
                    triggerImageCapture();
                }

                @Override
                public void onConfigureFailed(
                        @NonNull final CameraCaptureSession cameraCaptureSession
                ) {

                    Log.e(TAG, "Failed to configure camera.");
                }
            };

    /**
     * Execute a new capture request within the active session
     */
    private void triggerImageCapture() {
        try {
            final CaptureRequest.Builder captureBuilder =
                    device.createCaptureRequest(TEMPLATE_STILL_CAPTURE);

            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CONTROL_AE_MODE, CONTROL_AE_MODE_ON);

            Log.d(TAG, "Attempting capture...");
            captureSession.capture(captureBuilder.build(), captureCallback, null);
        } catch (final CameraAccessException e) {
            Log.e(TAG, "Camera access exception when performing capture.", e);
        }
    }

    public void shutDown() {
        if (device != null) {
            device.close();
        }
    }

}
