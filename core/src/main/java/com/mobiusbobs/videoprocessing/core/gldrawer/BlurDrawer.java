package com.mobiusbobs.videoprocessing.core.gldrawer;

import android.content.Context;

import com.mobiusbobs.videoprocessing.core.program.BlurHShaderProgram;
import com.mobiusbobs.videoprocessing.core.program.BlurVShaderProgram;

import java.io.IOException;

/**
 * android
 * <p>
 * Created by wangalbert on 6/9/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class BlurDrawer {
  private Context context;
  private BaseDrawer drawer;


  private BlurHShaderProgram blurHShaderProgram;
  private BlurVShaderProgram blurVShaderProgram;
  private float [] matrix;

  public BlurDrawer(Context context) {
    this.context = context;
    drawer = new BaseDrawer();
  }

  public void init() throws IOException {
    drawer.init();
    matrix = drawer.getMVPMatrix();

    blurHShaderProgram = new BlurHShaderProgram(context);
    blurVShaderProgram = new BlurVShaderProgram(context);
  }

  public void drawHorizontalBlur(int textureId, float blur) {
    blurHShaderProgram.useProgram();
    blurHShaderProgram.setUniforms(matrix, textureId, blur);
    drawer.bindData(blurHShaderProgram);
    drawer.draw();
  }

  public void drawVerticalBlur(int textureId, float blur) {
    blurVShaderProgram.useProgram();
    blurVShaderProgram.setUniforms(matrix, textureId, blur);
    drawer.bindData(blurVShaderProgram);
    drawer.draw();
  }

}
