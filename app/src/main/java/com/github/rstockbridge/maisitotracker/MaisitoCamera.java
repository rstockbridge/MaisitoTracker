package com.github.rstockbridge.maisitotracker;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Collections;

import static android.content.Context.CAMERA_SERVICE;
import static android.graphics.ImageFormat.JPEG;
import static android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON;
import static android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE;
import static com.github.rstockbridge.maisitotracker.Constants.TAG;

public final class MaisitoCamera {

    private static final int IMAGE_WIDTH = 320;
    private static final int IMAGE_HEIGHT = 240;
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
                    captureSession = null;
                }
            };

    private MaisitoCamera() {
        // This constructor intentionally left blank.
    }

    public void initialize(
            @NonNull final Context context,
            @NonNull final Handler backgroundHandler,
            @NonNull OnImageAvailableListener imageAvailableListener
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
        imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);

        // Open the camera resource
        try {
            manager.openCamera(id, deviceStateCallback, backgroundHandler);
        } catch (final CameraAccessException e) {
            Log.e(TAG, "Camera access exception when opening camera.", e);
        }
    }

    /**
     * Begin a still image capture
     */
    public void takePicture() {
        if (device == null) {
            Log.e(TAG, "Cannot capture image. Camera not initialized.");
            return;
        }

        try {
            device.createCaptureSession(
                    Collections.singletonList(imageReader.getSurface()),
                    sessionCallback,
                    null
            );
        } catch (final CameraAccessException e) {
            Log.e(TAG, "Access exception while preparing picture.", e);
        }
    }

    /**
     * Callback handling session state changes
     */
    private final CameraCaptureSession.StateCallback sessionCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // The camera is already closed
                    if (device == null) {
                        return;
                    }

                    // When the session is ready, we start capture.
                    captureSession = cameraCaptureSession;
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
