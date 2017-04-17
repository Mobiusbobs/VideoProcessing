package com.mobiusbobs.videoprocessing.core.hardware_control;

import android.hardware.Camera;

/**
 *
 * This class is in charge of handling camera instance management
 *
 * It can help client to open and hold one camera.
 *
 * Features:
 *
 *   1. hold last open camera
 *   2. can get current camera
 *   3. can open camera by given camera type (FRONT, BACK)
 *
 * ref: https://github.com/googlesamples/android-MediaRecorder/blob/master/Application/src/main/java/com/example/android/common/media/CameraHelper.java
 */

// TODO deal with camera deprecated problem
@SuppressWarnings("deprecation")
public class CameraInstanceManager {

    public enum CameraType {
        FRONT, BACK
    }

    private Camera currentCamera = null;
    private int currentCameraId = -1;

    // ----- current camera getter -----

    @SuppressWarnings("unused")
    public Camera getCurrentCamera() {
        return currentCamera;
    }

    public int getCurrentCameraId() {
        return currentCameraId;
    }

    // ----- camera lifecycle -----

    public Camera getCamera(CameraType cameraType) {
        int cameraId = -1;

        if (cameraType == CameraType.FRONT)
            cameraId = getDefaultFrontFacingCameraId();

        if (cameraType == CameraType.BACK)
            cameraId = getDefaultBackFacingCameraId();

        if (cameraId < 0) return null;


        if (currentCamera != null) {
            releaseCamera(currentCamera);
        }

        Camera camera = null;
        try {
            camera = Camera.open(cameraId);
        } catch (RuntimeException e) {
            // ignore error it is impossible to handle by ourselves
            // TODO create and emit error
        }

        if (camera == null) return null;

        currentCamera = camera;
        currentCameraId = cameraId;

        return camera;
    }

    public void releaseCamera(Camera camera) {
        if (camera == null) return;
        camera.release();
        currentCamera = null;
        // currentCameraId = -1;     // HOTFIX: this is causing blank camera when onResume
        // TODO: assign it to lastCameraId
    }

    public void reopenLastCamera() {
        if (currentCamera != null) return;
        if (currentCameraId < 0) return;
        currentCamera = Camera.open(currentCameraId);
    }

    // ----- camera id getters -----

    private static int getDefaultBackFacingCameraId() {
        return getDefaultCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    private static int getDefaultFrontFacingCameraId() {
        return getDefaultCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    private static int getDefaultCameraId(int position) {
        // Find the total number of cameras available
        int  mNumberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the back-facing ("default") camera
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < mNumberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == position) {
                return i;
            }
        }

        return -1;
    }
}
