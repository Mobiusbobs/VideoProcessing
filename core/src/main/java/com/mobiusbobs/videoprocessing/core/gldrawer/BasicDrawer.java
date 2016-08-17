package com.mobiusbobs.videoprocessing.core.gldrawer;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.mobiusbobs.videoprocessing.core.VideoProcessor;
import com.mobiusbobs.videoprocessing.core.program.BasicShaderProgram;

import java.io.IOException;

/**
 * VideoProcessing
 *
 * refs:
 * gen texture: http://www.learnopengles.com/android-lesson-four-introducing-basic-texturing/
 * shader setting: http://www.learnopengles.com/android-lesson-one-getting-started/
 * also see the TextureRender.java in this project
 *
 * <p/>
 * Created by rayshih on 3/22/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class BasicDrawer implements GLDrawable {
    public static final String TAG = "BasicDrawer";

    private Context context;

    protected GLDrawable prevDrawer;

    private float[] verticesPositionData;
    private GLPosition glPosition = new GLPosition(true);

    private float[] mMVPMatrix;

    /** This is a handle to our texture data. */
    private int[] textureHandle;

    private BasicShaderProgram shaderProgram;

    private float rotateInDeg = 0.0f;
    private float opacity = 1.0f;

    public BasicDrawer(Context context)  {
        this.context = context;
    }

    public BasicDrawer(Context context, float[] verticesPositionData)  {
        this.context = context;

        // TODO refactor this
        this.verticesPositionData = verticesPositionData;
        glPosition.setVerticesPositionData(verticesPositionData);
    }

    @Override
    public void init(GLDrawable prevDrawer) throws IOException {
        this.prevDrawer = prevDrawer;
        setupMVPMatrix();
        shaderProgram = new BasicShaderProgram(context);
    }

    public void init(GLDrawable prevDrawer, Bitmap bitmap) throws IOException {
        init(prevDrawer);
        loadTexture(bitmap);
    }

    public void init(GLDrawable prevDrawer, Bitmap bitmap, float[] verticesPositionData) throws IOException {
        setVerticesCoordinate(verticesPositionData);
        init(prevDrawer, bitmap);
    }

    public void setVerticesCoordinate(float[] verticesPositionData) {
        this.verticesPositionData = verticesPositionData;
        glPosition.setVerticesPositionData(verticesPositionData);
    }

    @Override
    public void setRotate(float rotateInDeg) {
        this.rotateInDeg = rotateInDeg;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    // ----- matrix -----
    private void setupMVPMatrix()   {
        // those index is come from util/CoordConverter
        float x1 = verticesPositionData[0];
        float x2 = verticesPositionData[3];
        float y1 = verticesPositionData[1];
        float y2 = verticesPositionData[7];

        mMVPMatrix = MatrixHelper.calculateMVPMatrix(
                MatrixHelper.createModelMatrix(x1, y1, x2, y2, rotateInDeg),
                MatrixHelper.createViewMatrix(),
                MatrixHelper.createProjectionMatrix(
                        VideoProcessor.OUTPUT_VIDEO_WIDTH,
                        VideoProcessor.OUTPUT_VIDEO_HEIGHT
                )
        );
    }

    // ----- texture -----
    public void setTextureHandleSize(int textureCount) {
        releaseTextures();

        // alloc texture
        textureHandle = new int[textureCount];
        GLES20.glGenTextures(textureCount, textureHandle, 0);
    }

    public void loadBitmapToTexture(Bitmap bitmap, int textureIndex) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[textureIndex]);

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
    }

    private void loadTexture(Bitmap bitmap) {
        setTextureHandleSize(1);
        loadBitmapToTexture(bitmap, 0);
    }

    @Override
    public void draw(long timeMs) {
        drawBackground(timeMs);
        drawThisOnly();
    }

    public void drawBackground(long timeMs) {
        if (prevDrawer != null) {
            prevDrawer.draw(timeMs);
        }
    }

    public void drawThisOnly() {
        drawThisOnly(0);
    }

    public void drawThisOnly(int textureIndex) {
        shaderProgram.useProgram();
        shaderProgram.bindData(
                mMVPMatrix,
                glPosition.getVerticesPosition(),

                textureHandle[textureIndex],
                glPosition.getTexturePosition(),

                opacity
        );

        // blend
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Draw the sticker.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    private void releaseTextures() {
        if (textureHandle == null) return;

        GLES20.glDeleteTextures(textureHandle.length, textureHandle, 0);
    }

    @Override
    protected void finalize() throws Throwable {
        releaseTextures();
        super.finalize();
    }
}
