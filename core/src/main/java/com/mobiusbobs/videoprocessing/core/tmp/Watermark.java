package com.mobiusbobs.videoprocessing.core.tmp;



import com.mobiusbobs.videoprocessing.core.program.TextureShaderProgram;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static com.mobiusbobs.videoprocessing.core.tmp.Constants.*;


/**
 * android
 * <p/>
 * Created by wangalbert on 6/7/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class Watermark {
  private static final String TAG = "Watermark";

  private static final int POSITION_COMPONENT_COUNT = 2;
  private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
  private static final int STRIDE = (POSITION_COMPONENT_COUNT
    + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT;

  private static final float[] VERTEX_DATA = {
    // Order of coordinates: X, Y, S, T
    // Triangle Fan
    0f,    0f,      0.5f, 0.5f,
    -0.55f, -0.15f,   0f, 1.0f,
    0.55f, -0.15f,   1f, 1.0f,
    0.55f,  0.15f,   1f, 0.0f,
    -0.55f,  0.15f,   0f, 0.0f,
    -0.55f, -0.15f,   0f, 1.0f
  };

  private final VertexArray vertexArray;

  public Watermark() {
    vertexArray = new VertexArray(VERTEX_DATA);
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
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glDrawArrays(GL_TRIANGLE_FAN, 0, 6);
  }
}
