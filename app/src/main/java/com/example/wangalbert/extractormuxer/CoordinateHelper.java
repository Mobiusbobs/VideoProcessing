package com.example.wangalbert.extractormuxer;

import android.util.Log;

/**
 * android
 * <p>
 * Created by wangalbert on 3/23/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class CoordinateHelper {
  private static final String TAG = "CoordinateHelper";

  private static final int GLCoordWidth = 2;
  private static final int GLCoordHeight = 2;

  private int screenWidth;
  private int screenHeight;

  // constructor
  public CoordinateHelper(int screenWidth, int screenHeight) {
    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
  }

  //
  public float[] getCoordinate(int padding) {
    // image [width, height]]
    float[] imgDimenInGLCoord = getImageDimen(); //TODO update input file

    // padding [x, y]
    float[] paddingInGLCoord = getPaddingInGLCoord(padding);

    float z = 0.0f;
    float x1 = 1.0f - imgDimenInGLCoord[0] - paddingInGLCoord[0];
    float x2 = 1.0f - paddingInGLCoord[0];
    float y1 = -1.0f + paddingInGLCoord[1];
    float y2 = -1.0f + imgDimenInGLCoord[1] + paddingInGLCoord[1];

    float[] verticesCoord = {
      x1, y1, z,  //BL
      x2, y1, z,  //BR
      x2, y2, z,  //TR

      x1, y1, z,  //BL
      x2, y2, z,  //TR
      x1, y2, z   //TL
    };

    for(int i=0; i<verticesCoord.length; i=i+3) {
      Log.d(TAG, "verticesCoord(" + verticesCoord[i] + ", " + verticesCoord[i+1] + ", " + verticesCoord[i+2] + ")");
    }

    return verticesCoord;
  }

  // get padding's width and length in GL coord
  private float[] getPaddingInGLCoord(int padding) {
    float paddingRL = (float)padding / screenWidth;
    float paddingTB = (float)padding / screenHeight;
    Log.d(TAG, "padding dimen: (" + paddingRL + ", " + paddingTB + ")");
    return new float[]{paddingRL, paddingTB};
  }

  // get image's width and height
  private float[] getImageDimen() {
    //[width, height]
    int imgWidth = 50;
    int imgHeight = 66;

    // convert it to GL coordinate width/height
    float imgWidthInGLCoord = (float)imgWidth / screenWidth * GLCoordWidth;
    float imgHeightInGLCoord = (float)imgHeight / screenHeight * GLCoordHeight;

    Log.d(TAG, "Image dimen: (" + imgWidthInGLCoord + ", " + imgHeightInGLCoord + ")");
    return new float[]{imgWidthInGLCoord, imgHeightInGLCoord};
  }

}
