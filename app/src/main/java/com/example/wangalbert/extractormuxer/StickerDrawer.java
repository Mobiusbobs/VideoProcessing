package com.example.wangalbert.extractormuxer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

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
public class StickerDrawer {

    private Context context;

    /** Store our model data in a float buffer. */
    private final float[] mCubeTextureCoordinates =
            {
                    // Front face
                    0.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f
            };

    // texture
    /** This will be used to pass in the texture. */
    private int mTextureUniformHandle;

    /** This will be used to pass in model texture coordinate information. */
    private int mTextureCoordinateHandle;

    /** Size of the texture coordinate data in elements. */
    private final int mTextureCoordinateDataSize = 2;

    /** This is a handle to our texture data. */
    private int mTextureDataHandle;

    // shader
    final String vertexShader =
            "uniform mat4 u_MVPMatrix;      \n"     // A constant representing the combined model/view/projection matrix.
                    + "attribute vec4 a_Position;     \n"     // Per-vertex position information we will pass in.
                    + "attribute vec4 a_Color;        \n"     // Per-vertex color information we will pass in.
                    + "attribute vec2 a_TexCoordinate; \n"

                    + "varying vec2 v_TexCoordinate;  \n"

                    + "void main()                    \n"     // The entry point for our vertex shader.
                    + "{                              \n"
                    + "   v_TexCoordinate = a_TexCoordinate; \n"
                    // It will be interpolated across the triangle.
                    + "   gl_Position = u_MVPMatrix   \n"     // gl_Position is a special variable used to store the final position.
                    + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
                    + "}                              \n";    // normalized screen coordinates.

    final String fragmentShader =
            "precision mediump float;       \n"     // Set the default precision to medium. We don't need as high of a
                    // precision in the fragment shader.
                    + "uniform sampler2D u_Texture;   \n"
                    + "varying vec2 v_TexCoordinate;  \n"
                    // triangle per fragment.
                    + "void main()                    \n"     // The entry point for our fragment shader.
                    + "{                              \n"
                    + "   gl_FragColor = texture2D(u_Texture, v_TexCoordinate);     \n"     // Pass the color directly through the pipeline.
                    + "}                              \n";

    private int shaderProgramHandle;

    public StickerDrawer(Context context) {
        this.context = context;
        loadTexture();
        setupShader();
        bindTexture();
    }

    // TODO error handle
    private void loadTexture() {
        // alloc texture
        int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);

        // get bitmap
        int resId = R.drawable.frames_hungry;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);

        // bind texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // Recycle the bitmap, since its data has been loaded into OpenGL.
        bitmap.recycle();

        mTextureDataHandle = textureHandle[0];
    }

    // TODO shader compile flow should be reused
    private void setupShader() {
        int vertexShaderHandle = compileShader(vertexShader);
        int fragmentShaderHandle = compileShader(fragmentShader);
        shaderProgramHandle = createShaderProgram(vertexShaderHandle, fragmentShaderHandle);
    }

    private int compileShader(String shaderProgram) {
        // Load in the vertex shader.
        int shaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);

        if (shaderHandle != 0)
        {
            // Pass in the shader source.
            GLES20.glShaderSource(shaderHandle, shaderProgram);

            // Compile the shader.
            GLES20.glCompileShader(shaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        if (shaderHandle == 0)
        {
            throw new RuntimeException("Error creating vertex shader.");
        }

        return shaderHandle;
    }

    private int createShaderProgram(int vertexShaderHandle, int fragmentShaderHandle) {
        // Create a program object and store the handle to it.
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0)
        {
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0)
            {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0)
        {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }

    private void bindTexture() {
        mTextureUniformHandle = GLES20.glGetUniformLocation(shaderProgramHandle, "u_Texture");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(shaderProgramHandle, "a_TexCoordinate");
    }

    public void draw() {

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

    }
}
