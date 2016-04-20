package com.mobiusbobs.videoprocessing.core;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;

import com.mobiusbobs.videoprocessing.core.gles.FullFrameRect;
import com.mobiusbobs.videoprocessing.core.gles.Texture2dProgram;
import com.mobiusbobs.videoprocessing.core.hardware_control.CameraController;

import java.lang.ref.WeakReference;

/**
 * VideoRecorder
 * <p/>
 * Created by rayshih on 4/14/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
@SuppressWarnings("deprecation")
public class CameraView
        extends GLSurfaceView
        implements
        CameraPreviewRenderer.Callback,
        SurfaceTexture.OnFrameAvailableListener
{
    public static String TAG = "CameraView";

    // Camera
    private CameraController cameraController;
    private CameraHandler mCameraHandler;

    // camera input
    protected FullFrameRect mFullScreen;        // preview renderer (shader program and binding
    private int mTextureId;                     // texture id for camera preview input
    private SurfaceTexture mSurfaceTexture;     // image stream adapter from camera to texture
    private final float[] mSTMatrix = new float[16];

    // view itself
    private int width;
    private int height;

    // other
    private Callback callback;

    public CameraView(Context context) {
        super(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(CameraController cameraController, Callback callback) {
        this.cameraController = cameraController;
        this.callback = callback;
        setupGL();
        setupThreadHandler();
    }

    private void setupGL() {
        CameraPreviewRenderer renderer = new CameraPreviewRenderer(this);
        setEGLContextClientVersion(2);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private void setupThreadHandler() {
        mCameraHandler = new CameraHandler(this);
    }

    // ----- view lifecycle -----

    @Override
    public void onPause() {
        if (cameraController != null) {
            cameraController.releaseCamera();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cameraController != null) {
            cameraController.openCamera(width, height, "FRONT");
            notifyPreviewChanged();
        }
    }

    public void onDestory() {
        mCameraHandler.invalidateHandler();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;

        if (cameraController != null) {
            cameraController.openCamera(w, h, "FRONT");
            notifyPreviewChanged();
        }
    }

    private void notifyPreviewChanged() {
        callback.onPreviewSizeChanged(
                cameraController.getPreviewWidth(),
                cameraController.getPreviewHeight()
        );
    }

    // ----- gl related function -----
    private void handleSetSurfaceTexture(SurfaceTexture st) {
        st.setOnFrameAvailableListener(this);
        cameraController.startPreview(st);
    }

    // SurfaceTexture for camera onFrameAvailable
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
        this.callback.onFrameAvailable(mTextureId, surfaceTexture);
    }

    // ----- gl renderer callbacks -----
    // this method will be called in gl thread
    // generate gl texture and wrap it then hand it to camera as video input
    @Override
    public void onSurfaceCreated() {
        // Set up the texture blitter that will be used for on-screen display.  This
        // is *not* applied to the recording, because that uses a separate shader.
        mFullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));

        mTextureId = mFullScreen.createTextureObject();

        // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
        // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
        // available messages will arrive on the main thread.
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        // Tell the UI thread to enable the camera preview.
        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(
                CameraHandler.MSG_SET_SURFACE_TEXTURE, mSurfaceTexture));

        this.callback.onSurfaceCreated();

    }

    // called in gl thread
    @Override
    public void onDrawFrame() {
        mSurfaceTexture.updateTexImage(); // latch the last frame
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        mFullScreen.drawFrame(mTextureId, mSTMatrix);
    }

    // ----- camera handler -----
    /**
     * This is only for threading
     * Handles camera operation requests from other threads.  Necessary because the Camera
     * must only be accessed from one thread.
     * <p>
     * The object is created on the UI thread, and all handlers run there.  Messages are
     * sent from other threads, using sendMessage().
     */
    static class CameraHandler extends Handler {
        public static final int MSG_SET_SURFACE_TEXTURE = 0;

        // Weak reference to the Activity; only access this from the UI thread.
        private WeakReference<CameraView> mWeakView;

        public CameraHandler(CameraView view) {
            mWeakView = new WeakReference<>(view);
        }

        /**
         * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
         * attempts to access a stale Activity through a handler are caught.
         */
        public void invalidateHandler() {
            mWeakView.clear();
        }

        @Override  // runs on UI thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);

            CameraView view = mWeakView.get();
            if (view == null) {
                Log.w(TAG, "CameraHandler.handleMessage: activity is null");
                return;
            }

            switch (what) {
                case MSG_SET_SURFACE_TEXTURE:
                    view.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }
    }

    // ----- callback -----
    public interface Callback {
        void onSurfaceCreated();
        void onPreviewSizeChanged(int w, int h);
        void onFrameAvailable(int textureId, SurfaceTexture surfaceTexture);
    }
}
