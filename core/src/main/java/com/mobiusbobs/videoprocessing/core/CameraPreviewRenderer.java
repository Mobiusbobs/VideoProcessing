package com.mobiusbobs.videoprocessing.core;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * VideoRecorder
 * <p/>
 * Created by rayshih on 3/21/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class CameraPreviewRenderer implements GLSurfaceView.Renderer {
    private Callback mCallback;

    public CameraPreviewRenderer(Callback callback) {
        super();
        mCallback = callback;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCallback.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mCallback.onDrawFrame();
    }

    public interface Callback {
        void onSurfaceCreated();
        void onDrawFrame();
    }
}
