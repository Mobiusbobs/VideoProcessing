package com.mobiusbobs.videoprocessing.core.program;

import android.content.Context;

import com.mobiusbobs.videoprocessing.core.R;

import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;

/**
 * android
 * <p/>
 * Created by wangalbert on 6/7/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class TextureOpacityShaderProgram extends TextureShaderProgram {
  private final static String TAG = "TextureOpacityShaderProgram";

  // Uniform locations
  private final int uOpcaityLocation;

  public TextureOpacityShaderProgram(Context context) {
    super(context,  R.raw.texture_vertex_shader, R.raw.texture_fragment_opacity_shader);

    // Retrieve uniform locations for the shader program.
    uOpcaityLocation = glGetUniformLocation(program, "u_Opacity"); //u_Opacity
  }

  public void setUniforms(float[] matrix, int textureId, float opacity) {
    super.setUniforms(matrix, textureId);
    // Pass in the blur information
    glUniform1f(uOpcaityLocation, opacity);
  }

}
