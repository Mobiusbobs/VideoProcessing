package com.mobiusbobs.videoprocessing.core.program;

import android.content.Context;
import android.opengl.GLES20;

import com.mobiusbobs.videoprocessing.core.R;

import java.nio.FloatBuffer;

/**
 * android
 * <p/>
 * Created by rayshih on 6/13/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
// TODO
public class BlurHShaderProgram extends ShaderProgram {

    public final int POSITION_DATASIZE = 3;
    public final int TEXTURE_COORD_DATASIZE = 2;

    private int mvpMatrixHandle;
    private int textureUniformHandle;
    private int positionHandle;
    private int textureCoordinateHandle;
    private int opacityHandle;

    public BlurHShaderProgram(Context context) {
        super(context,
                R.raw.basic_vertex_shader,
                R.raw.basic_fragment_shader);

        setupHandles();
    }

    private void setupHandles() {
        mvpMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        textureUniformHandle = GLES20.glGetUniformLocation(programHandle, "u_Texture");
        positionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
        textureCoordinateHandle = GLES20.glGetAttribLocation(programHandle, "a_TexCoordinate");
        opacityHandle = GLES20.glGetUniformLocation(programHandle, "u_Opacity");
    }

    public void bindData(
            float[] mvpMatrix,
            FloatBuffer verticesPosition,

            int textureHandle,
            FloatBuffer texturePosition,

            float opacity
    ) {
        // --- mvp matrix ---
        // set the matrix
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // --- vertices position
        // Pass in the position information
        verticesPosition.position(0);
        GLES20.glVertexAttribPointer(positionHandle, POSITION_DATASIZE, GLES20.GL_FLOAT, false,
                0, verticesPosition);
        GLES20.glEnableVertexAttribArray(positionHandle);

        // --- texture ---
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(textureUniformHandle, 0);

        // --- texture position ---
        // Pass in the texture coordinate information
        texturePosition.position(0);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, TEXTURE_COORD_DATASIZE, GLES20.GL_FLOAT, false,
                0, texturePosition);
        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);

        // --- opacity ---
        // Pass opacity info
        GLES20.glUniform1f(opacityHandle, opacity);
    }

}
