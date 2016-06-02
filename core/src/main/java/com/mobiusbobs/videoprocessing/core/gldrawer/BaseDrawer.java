package com.mobiusbobs.videoprocessing.core.gldrawer;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.mobiusbobs.videoprocessing.core.R;
import com.mobiusbobs.videoprocessing.core.gles.GlUtil;
import com.mobiusbobs.videoprocessing.core.gles.RawResourceReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

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
public class BaseDrawer implements GLDrawable {
    private static final String TAG = "BaseDrawer";

    public static final float OPAQUE = 1.0f;
    public static final float TRANSPARENT = 0.0f;

    /** Store our model data in a float buffer. */
    protected float[] verticesPositionData = {
        // X, Y, Z,
        -1.0f, -1.0f, 0.0f,
        1.0f, -1.0f, 0.0f,
        1.0f, 1.0f, 0.0f,

        -1.0f, -1.0f, 0.0f,
        1.0f, 1.0f, 0.0f,
        -1.0f, 1.0f, 0.0f
    };

    float[] textureCoordinateData = {
        // Front face
        0.0f, 1.0f,
        1.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 0.0f
    };

    protected FloatBuffer verticesPosition;
    protected FloatBuffer texturePosition;
    protected final int TEXTURE_COORD_DATASIZE = 2;
    protected final int POSITION_DATASIZE = 3;
    protected final int BYTES_PER_FLOAT = 4;

    // transformation
    protected int mMVPMatrixHandle;

    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
    protected float[] mMVPMatrix = new float[16];

    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    protected float[] mModelMatrix = new float[16];

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    protected float[] mViewMatrix = new float[16];

    /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
    protected float[] mProjectionMatrix = new float[16];

    // texture
    /** This will be used to pass in the texture. */
    protected int mTextureUniformHandle;
    /** This will be used to pass in model texture coordinate information. */
    protected int mTextureCoordinateHandle;
    /** This will be used to pass in the position coordinate of the sticker. */
    protected int mPositionHandle;
    /** This will be used to pass in the opcaity of the sticker. */
    protected int mOpacity;

    /** This is a handle to our texture data. */
    private int[] textureHandle;

    protected int shaderProgramHandle;
    private Context context;

    public BaseDrawer(Context context)  {
        this.context = context;
    }

    public BaseDrawer(Context context, float[] verticesPositionData)  {
        this.context = context;
        this.verticesPositionData = verticesPositionData;
    }

    @Override
    public void init() throws IOException {
        initCoordinateBuffer();

        // calculate matrix
        setupProjectionMatrix();
        setupViewMatrix();
        calculateMVPMatrix();

        compileShaderProgram();
        getVariableLocation();
    }

    public void init(Bitmap bitmap, float[] verticesPositionData) throws IOException {
        setVerticesCoordinate(verticesPositionData);
        init(bitmap);
    }

    public void init(Bitmap bitmap) throws IOException {init();
        loadTexture(bitmap, 1);
    }

    public void setVerticesCoordinate(float[] verticesPositionData) {
        this.verticesPositionData = verticesPositionData;
        if (verticesPosition != null) {
            verticesPosition.clear();
            verticesPosition.put(verticesPositionData).position(0);
        }
    }

    protected void initCoordinateBuffer() {
        // Initialize the buffers.
      verticesPosition = ByteBuffer.allocateDirect(verticesPositionData.length * BYTES_PER_FLOAT)
          .order(ByteOrder.nativeOrder()).asFloatBuffer();
      verticesPosition.put(verticesPositionData).position(0);

      texturePosition = ByteBuffer.allocateDirect(textureCoordinateData.length * BYTES_PER_FLOAT)
          .order(ByteOrder.nativeOrder()).asFloatBuffer();
      texturePosition.put(textureCoordinateData).position(0);
    }

    protected void setupProjectionMatrix() {
        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float left = -1.0f;   //-ratio;
        final float right = 1.0f;   //ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = -1.0f;   //1.0f;
        final float far = 1.0f;     //10.0f;

        Matrix.orthoM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    protected void setupViewMatrix() {
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
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
    }

    protected void calculateMVPMatrix()   {
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
    }

    public void setTextureHandleSize(int textureCount) {
        textureHandle = new int[textureCount];
        // alloc texture
        GLES20.glGenTextures(textureCount, textureHandle, 0);
    }

    private void loadTexture(Bitmap bitmap, int textureCount) {
        setTextureHandleSize(textureCount);
        loadBitmapToTexture(bitmap, 0);
    }

    public void loadBitmapToTexture(Bitmap bitmap, int textureIndex) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[textureIndex]);

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
    }

    protected void compileShaderProgram() {
        String vertexShader = RawResourceReader.readTextFileFromRawResource(
            context,
            R.raw.basedrawer_vertex_shader
        );
        String fragmentShader = RawResourceReader.readTextFileFromRawResource(
            context,
            R.raw.basedrawer_fragment_shader
        );
        shaderProgramHandle = GlUtil.createProgram(vertexShader, fragmentShader);
    }

    protected void getVariableLocation() {
        // uniform
        mMVPMatrixHandle = GLES20.glGetUniformLocation(shaderProgramHandle, "u_MVPMatrix");
        mTextureUniformHandle = GLES20.glGetUniformLocation(shaderProgramHandle, "u_Texture");
        mOpacity = GLES20.glGetUniformLocation(shaderProgramHandle, "u_Opacity");

        // attribute
        mPositionHandle = GLES20.glGetAttribLocation(shaderProgramHandle, "a_Position");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(shaderProgramHandle, "a_TexCoordinate");
    }

    @Override
    public void draw(long timeMs) {
        draw(0, 1);
    }

    public void draw(int textureIndex, float opacity) {
        GLES20.glUseProgram(shaderProgramHandle);

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[textureIndex]);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        // Pass in the position information
        verticesPosition.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATASIZE, GLES20.GL_FLOAT, false,
          0, verticesPosition);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the texture coordinate information
        texturePosition.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORD_DATASIZE, GLES20.GL_FLOAT, false,
          0, texturePosition);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        // Pass in the opacity information
        GLES20.glUniform1f(mOpacity, opacity);

        // blend
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // set the matrix
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the sticker.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }
}
