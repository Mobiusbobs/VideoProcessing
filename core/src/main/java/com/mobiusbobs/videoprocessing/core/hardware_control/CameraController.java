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

    private CameraInstanceManager cameraInstanceManager = new CameraInstanceManager();
    private boolean frontCamera = true;
    private Camera mCamera;

    private int mCameraWidth;
    private int mCameraHeight;

    private SurfaceTexture surfaceTexture;

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
        mCamera.setDisplayOrientation(90);

        // setup preview size
        Camera.Parameters parms = mCamera.getParameters();
        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);

        // leave the frame rate set to default
        mCamera.setParameters(parms);

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

        return mCamera;
    }

    public Camera switchCamera() {
        if  (frontCamera) {
            frontCamera = false;
            openCamera(mCameraWidth, mCameraHeight, "BACK");
        }   else {
            frontCamera = true;
            openCamera(mCameraWidth, mCameraHeight, "FRONT");
        }

        restartPreview();
        return mCamera;
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

    // ----- preview -----
    public void startPreview(SurfaceTexture st) {
        surfaceTexture = st;

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

        restartPreview(surfaceTexture);
    }

    public void restartPreview(SurfaceTexture st) {
        if (mCamera != null)
            mCamera.stopPreview();
        startPreview(st);
    }

}
