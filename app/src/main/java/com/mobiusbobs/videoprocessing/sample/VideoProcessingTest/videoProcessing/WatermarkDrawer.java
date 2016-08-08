package com.mobiusbobs.videoprocessing.sample.VideoProcessingTest.videoProcessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import com.mobiusbobs.videoprocessing.core.VideoProcessor;
import com.mobiusbobs.videoprocessing.core.gldrawer.BasicDrawer;
import com.mobiusbobs.videoprocessing.core.gldrawer.BlurDrawer;
import com.mobiusbobs.videoprocessing.core.gldrawer.GLDrawable;
import com.mobiusbobs.videoprocessing.core.util.BitmapHelper;
import com.mobiusbobs.videoprocessing.core.util.FrameBufferHelper;
import com.mobiusbobs.videoprocessing.sample.R;

import java.io.IOException;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glViewport;

/**
 * VideoProcessing
 * <p/>
 * Created by rayshih on 4/13/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class WatermarkDrawer implements DurationGLDrawable {
    public static int WATERMARK_ID = R.drawable.logo_watermark;

    public static int FADE_IN_DURATION = 1000;
    private Context context;

    private BasicDrawer drawer;
    private Duration duration;

    public static final String TAG = "WatermarkDrawer";

    // ---- for blur ----

    // FBO (Frame Buffer Object)
    // should be power of 2 (POT)
    private int fboWidth = 1024;
    private int fboHeight = 1024;

    // TODO refactor: rename
    // first FBO that stores all the render and apply first blur
    private int fboId;
    private int fboTextureId;

    // second FBO that take the first FBO and apply second blur
    private int fboBlurId;
    private int fboBlurTextureId;

    private BlurDrawer blurVDrawer;
    private BlurDrawer blurHDrawer;

    public WatermarkDrawer(Context context, float[] verticesPositionData) {
        this.context = context;

        blurVDrawer = new BlurDrawer(
                context,
                VideoProcessor.OUTPUT_VIDEO_WIDTH,
                VideoProcessor.OUTPUT_VIDEO_HEIGHT,
                true
        );

        blurHDrawer = new BlurDrawer(
                context,
                VideoProcessor.OUTPUT_VIDEO_WIDTH,
                VideoProcessor.OUTPUT_VIDEO_HEIGHT,
                false
        );

        drawer = new BasicDrawer(context, verticesPositionData);
    }

    @Override
    public void init(GLDrawable prevDrawer) throws IOException {

        // --- create fbo to render blur for watermark animation ---
        checkGlError("before create fbo");
        // create fbo
        int[] tmp = FrameBufferHelper.createFrameBuffer(fboWidth, fboHeight);
        fboId = tmp[0];
        fboTextureId = tmp[1];

        checkGlError("before create fbo2");
        // create fboBlur
        tmp = FrameBufferHelper.createFrameBuffer(fboWidth, fboHeight);
        fboBlurId = tmp[0];
        fboBlurTextureId = tmp[1];

        checkGlError("before init blurVDrawer");
        blurVDrawer.init(null);
        checkGlError("before init blurHDrawer");
        blurHDrawer.init(null);

        // --- watermark ---
        Bitmap bitmap = BitmapHelper.generateBitmap(context, WATERMARK_ID);
        checkGlError("before init watermark drawer with watermark");
        drawer.init(prevDrawer, bitmap);
        checkGlError("after init watermark drawer with watermark");
        bitmap.recycle();
    }

    // TODO make target frame buffer configurable
    @Override
    public void draw(long timeMs) {
        long startTime = duration == null ? timeMs + 1 : duration.from;
        long diff = timeMs - startTime;

        if (diff < 0) {
            // not yet, skip
            drawer.drawBackground(timeMs);
            return;
        }

        if (diff > FADE_IN_DURATION) {
            diff = FADE_IN_DURATION;
        }

        // process 0 ~ 1
        float process = (float) diff / FADE_IN_DURATION;

        // -------- render to fbo ---------
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glViewport(0, 0, fboWidth, fboHeight);
        // TODO don't need to clean I think
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // draw to texture
        drawer.drawBackground(timeMs);

        // -------------------------------

        // -------- render to fbo 2 ---------
        glBindFramebuffer(GL_FRAMEBUFFER, fboBlurId);
        glViewport(0, 0, fboWidth, fboHeight);
        // TODO don't need to clean I think
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // blur texture from first fbo and render to second fbo
        drawHorizontalBlur(fboTextureId, process);

        // -------- render to the screen ---------
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, 720, 1280);
        // TODO don't need to clean I think
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // blur texture from second fbo and render to screen
        drawVerticalBlur(fboBlurTextureId, process);

        Boolean shouldDraw = duration == null || duration.check(timeMs);

        if (shouldDraw) {
            drawer.setOpacity(process);
            drawer.drawThisOnly();
        }
    }

    private void drawVerticalBlur(int textureHandle, float process) {
        blurVDrawer.setBlur(process);
        blurVDrawer.setTextureHandle(textureHandle);
        blurVDrawer.draw(0); // Time doesn't matter
    }

    private void drawHorizontalBlur(int textureHandle, float process) {
        blurHDrawer.setBlur(process);
        blurHDrawer.setTextureHandle(textureHandle);
        blurHDrawer.draw(0); // Time doesn't matter
    }

    @Override
    public void setDuration(Duration d) {
        duration = d;
    }

    @Override
    public void setRotate(float rotateInDeg) {
        drawer.setRotate(rotateInDeg);
    }


    public void checkGlError(String op) {
        int error;
        Log.d(TAG, "checkEglError: " + op);
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
}
