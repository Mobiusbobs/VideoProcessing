package com.mobiusbobs.videoprocessing.core.gldrawer;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.mobiusbobs.videoprocessing.core.program.TextureShaderProgram;
import com.mobiusbobs.videoprocessing.core.util.VertexArray;

import java.io.IOException;

import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static com.mobiusbobs.videoprocessing.core.tmp.Constants.BYTES_PER_FLOAT;

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

    /** Store our model data in a float buffer. */


    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT
      + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    private VertexArray vertexArray;
    protected float[] verticesPositionData = {
      // Order of coordinates: X, Y, S, T
      // Triangle Fan
      0f,       0f,     0.5f,  0.5f,
      -1.0f,    1.0f,   0f,    1f,
      -1.0f,    -1.0f,  1f,    1f,  //0f,    0f,
      1.0f,     -1.0f,  1f,    0f,  //1f,    0f,
      1.0f,     1.0f,   0f,    0f,  //1f,    1f,
      -1.0f,    1.0f,   0f,    1f
    };

    // transformation
    //protected int mMVPMatrixHandle;

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

    public BaseDrawer()  {}

    public BaseDrawer(float[] verticesPositionData)  {
        this.verticesPositionData = verticesPositionData;
    }

    @Override
    public void init() throws IOException {
        //initCoordinateBuffer();
        vertexArray = new VertexArray(verticesPositionData);

        // calculate matrix
        setupProjectionMatrix();
        setupViewMatrix();
        setMVPMatrix(0);
    }

    public void init(float[] verticesPositionData) throws IOException {
        this.verticesPositionData = verticesPositionData;
        init();
    }

    public void setVerticesCoordinate(float[] verticesPositionData) {
        this.verticesPositionData = verticesPositionData;
        vertexArray = new VertexArray(verticesPositionData);
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

    public float[] setMVPMatrix(float angleInDegrees)   {
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        return mMVPMatrix;
    }

    public float[] getMVPMatrix() {
        return mMVPMatrix;
    }

    @Override
    public void draw(long timeMs) {
        draw();
    }

    public void bindData(TextureShaderProgram textureProgram) {
        vertexArray.setVertexAttribPointer(
          0,
          textureProgram.getPositionAttributeLocation(),
          POSITION_COMPONENT_COUNT,
          STRIDE
        );

        vertexArray.setVertexAttribPointer(
          POSITION_COMPONENT_COUNT,
          textureProgram.getTextureCoordinatesAttributeLocation(),
          TEXTURE_COORDINATES_COMPONENT_COUNT,
          STRIDE
        );
    }

    public void draw() {
        // blend
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Draw the sticker.
        GLES20.glDrawArrays(GL_TRIANGLE_FAN, 0, 6);
    }
}
