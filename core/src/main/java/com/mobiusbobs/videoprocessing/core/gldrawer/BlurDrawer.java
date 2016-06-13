package com.mobiusbobs.videoprocessing.core.gldrawer;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.mobiusbobs.videoprocessing.core.program.BasicShaderProgram;
import com.mobiusbobs.videoprocessing.core.program.BlurShaderProgram;
import com.mobiusbobs.videoprocessing.core.util.CoordConverter;

import java.io.IOException;
import java.util.Arrays;

/**
 * android
 * <p/>
 * Created by rayshih on 6/13/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class BlurDrawer implements GLDrawable {
    public static final String TAG = "BlurDrawer";

    private Context context;

    private int width;
    private int height;

    private GLPosition glPosition = new GLPosition(false);

    private float[] mMVPMatrix;

    private int textureHandle;
    private float blur = 0.0f;

    private boolean isVertical;

    private BlurShaderProgram shaderProgram;

    public BlurDrawer(Context context, int width, int height, boolean isVertical) {
        this.context = context;
        this.width = width;
        this.height = height;
        float[] vertices = CoordConverter.getVerticesCoord(0, 0, width, height);
        glPosition.setVerticesPositionData(vertices);
        this.isVertical = isVertical;
    }

    @Override
    public void init(GLDrawable prevDrawer) throws IOException {
        setupMVPMatrix();
        shaderProgram = new BlurShaderProgram(context, isVertical);
    }

    // ----- matrix -----
    private void setupMVPMatrix()   {
        float x1 = 0;
        float y1 = 0;
        float x2 = width;
        float y2 = height;

        mMVPMatrix = MatrixHelper.calculateMVPMatrix(
                MatrixHelper.createModelMatrix(x1, y1, x2, y2, 0),
                MatrixHelper.createViewMatrix(),
                MatrixHelper.createProjectionMatrix(width, height)
        );
    }

    @Override
    public void draw(long timeMs) {
        shaderProgram.useProgram();
        shaderProgram.bindData(
                mMVPMatrix,
                glPosition.getVerticesPosition(),

                textureHandle,
                glPosition.getTexturePosition(),

                blur
        );

        // blend
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Draw the sticker.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    // ----- setters -----
    @Override
    public void setRotate(float rotateInDeg) {
        // TODO remove this from GLDrawable
    }

    public void setTextureHandle(int textureHandle) {
        this.textureHandle = textureHandle;
    }

    public void setBlur(float blur) {
        this.blur = blur;
    }
}
