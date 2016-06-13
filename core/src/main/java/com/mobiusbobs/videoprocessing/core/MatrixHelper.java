package com.mobiusbobs.videoprocessing.core;

import android.opengl.Matrix;

/**
 * android
 * <p/>
 * Created by rayshih on 6/13/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class MatrixHelper {

    public static float[] createModelMatrix(float x1, float y1, float x2, float y2, float rotateInDeg) {
        float centerX = (x1 + x2)/2;
        float centerY = (y1 + y2)/2;

        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.translateM(matrix, 0, centerX, centerY, 0);
        Matrix.rotateM(matrix, 0, rotateInDeg, 0.0f, 0.0f, 1.0f);
        Matrix.translateM(matrix, 0, -centerX, -centerY, 0);
        return matrix;
    }

    public static float[] createProjectionMatrix(float width, float height) {
        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float left = 0;
        final float right = width;
        final float bottom = 0;
        final float top = height;
        final float near = -1.0f;
        final float far = 1.0f;

        float[] matrix = new float[16];
        Matrix.orthoM(matrix, 0, left, right, bottom, top, near, far);
        return matrix;
    }

    public static float[] createViewMatrix() {
        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 1.0f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = 0.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        float[] matrix = new float[16];
        Matrix.setLookAtM(matrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
        return matrix;
    }

    public static float[] calculateMVPMatrix(
            float[] modelMatrix,
            float[] viewMatrix,
            float[] projectionMatrix
    ) {
        float[] matrix = new float[16];
        Matrix.multiplyMM(matrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(matrix, 0, projectionMatrix, 0, matrix, 0);
        return matrix;
    }
}
