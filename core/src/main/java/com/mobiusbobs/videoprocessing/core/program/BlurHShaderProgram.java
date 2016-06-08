package com.mobiusbobs.videoprocessing.core.program;

import android.content.Context;

import com.mobiusbobs.videoprocessing.core.R;

import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;

/**
 * android
 * <p>
 * Created by wangalbert on 6/6/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class BlurHShaderProgram extends TextureShaderProgram {
  private final static String TAG = "BlurHShaderProgram";

  // Uniform locations
  private final int uBlurLocation;


  // Constructor
  public BlurHShaderProgram(Context context) {
    super(context, R.raw.blur_vertex_horizontal_shader, R.raw.blur_fragment_shader);

    uBlurLocation = glGetUniformLocation(program, "u_Blur");
  }

  public void setUniforms(float[] matrix, int textureId, float blur) {
    super.setUniforms(matrix, textureId);
    // Pass in the blur information
    glUniform1f(uBlurLocation, blur);
  }

}