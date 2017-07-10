package com.mobiusbobs.videoprocessing.core.hardware_control;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import com.mobiusbobs.videoprocessing.core.util.CameraUtils;

import java.io.IOException;

/**
 * VideoRecorder
 * <p/>
 * Created by rayshih on 4/14/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
@SuppressWarnings("deprecation")
public class CameraController {

    public static String TAG = "CameraController";

    private Callback callback;
    private CameraInstanceManager cameraInstanceManager = new CameraInstanceManager();
    private boolean frontCamera = true;
    private Camera mCamera;

    private int mCameraWidth;
    private int mCameraHeight;

    private SurfaceTexture surfaceTexture;

    public CameraController(Callback callback) {
        this.callback = callback;
    }

    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     * <p>
     * Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the preview.
     */
    public Camera openCamera(int desiredWidth, int desiredHeight, String type) {
        if (mCamera != null) {
            releaseCamera();
        }

        // get camera and set rotation
        CameraInstanceManager.CameraType cameraType =
                CameraInstanceManager.CameraType.valueOf(type);
        mCamera = cameraInstanceManager.getCamera(cameraType);

        if (mCamera == null) {
            callback.onError(new NullPointerException("open camera: getCamera returns null..."));
            return null;
        }
        mCamera.setDisplayOrientation(90);

        // setup preview size
        Camera.Parameters parms = mCamera.getParameters();
        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);

        // set auto-focus
        if (parms.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parms.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);

        // leave the frame rate set to default
        try {
            mCamera.setParameters(parms);
        } catch (RuntimeException e) {
            callback.onError(e);
        }

        // log preview fact
        int[] fpsRange = new int[2];
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        parms.getPreviewFpsRange(fpsRange);
        String previewFacts = mCameraPreviewSize.width + "x" + mCameraPreviewSize.height;
        if (fpsRange[0] == fpsRange[1]) {
            previewFacts += " @" + (fpsRange[0] / 1000.0) + "fps";
        } else {
            previewFacts += " @[" +
                    (fpsRange[0] / 1000.0) +
                    " - " +
                    (fpsRange[1] / 1000.0) +
                    "] fps";
        }
        Log.d(TAG, "previewFacts = " + previewFacts);

        // save for future switch
        mCameraWidth = mCameraPreviewSize.width;
        mCameraHeight = mCameraPreviewSize.height;

        notifyPreviewChanged();

        return mCamera;
    }

    public Camera switchCamera(boolean front) {
        frontCamera = front;
        openCamera(mCameraWidth, mCameraHeight, front ? "FRONT" : "BACK");
        restartPreview();
        return mCamera;
    }

    @SuppressWarnings("unused")
    public Camera switchCamera() {
        return switchCamera(!frontCamera);
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }

    public boolean isCameraOpened() {
        return mCamera != null;
    }

    // ----- preview -----
    public void startPreview(SurfaceTexture st) {
        if (mCamera == null) {
            return;
        }

        surfaceTexture = st;
        mCamera.stopPreview();

        try {
            mCamera.setPreviewTexture(st);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        mCamera.startPreview();
    }

    public void restartPreview() {
        if (surfaceTexture == null) {
            return;
        }

        startPreview(surfaceTexture);
    }

    private void notifyPreviewChanged() {
        // because rotate 90
        //noinspection SuspiciousNameCombination
        callback.onPreviewSizeChanged(mCameraHeight, mCameraWidth);
    }

    public interface Callback {
        void onPreviewSizeChanged(int width, int height);

        // error handle
        void onError(Exception e);
    }
}
