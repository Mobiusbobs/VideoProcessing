package com.mobiusbobs.videoprocessing.core.gldrawer;

import com.mobiusbobs.videoprocessing.core.util.CoordConverter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * android
 * <p/>
 * Created by rayshih on 6/13/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class GLPosition {

    // Start from Left Bottom
    // counter clockwise
    // lower right then upper left

    // LB -> RB -> RT
    // LB -> RT -> LT

    /** Store our model data in a float buffer. */
    private float[] verticesPositionData = {
            // X, Y, Z,

            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,

            -1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f
    };

    private float[] textureCoordinateData = {
            // Front face
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,

            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
    };

    private float[] upsideDownTextureCoordinateData = {
            // Front face
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,

            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f
    };

    public final int BYTES_PER_FLOAT = 4;

    private FloatBuffer verticesPosition;
    private FloatBuffer texturePosition;

    public GLPosition(boolean upsideDown) {
        initCoordinateBuffer(upsideDown);
    }

    private void initCoordinateBuffer(boolean upsideDown) {
        // Initialize the buffers.
        verticesPosition = ByteBuffer.allocateDirect(verticesPositionData.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesPosition.put(verticesPositionData).position(0);

        float[] tc = upsideDown ? upsideDownTextureCoordinateData : textureCoordinateData;
        texturePosition = ByteBuffer.allocateDirect(tc.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texturePosition.put(tc).position(0);
    }

    public void setVerticesPositionData(float[] verticesPositionData) {
        this.verticesPositionData = verticesPositionData;
        if (verticesPosition != null) {
            verticesPosition.clear();
            verticesPosition.put(verticesPositionData).position(0);
        }
    }

    public FloatBuffer getVerticesPosition() {
        return verticesPosition;
    }

    public FloatBuffer getTexturePosition() {
        return texturePosition;
    }

}
