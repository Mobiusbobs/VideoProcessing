package com.mobiusbobs.videoprocessing.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.InputStream;
import java.net.URL;

/**
 * android
 * <p>
 * Created by wangalbert on 3/23/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class VerticesHelper {
  private static final String TAG = "CoordinateHelper";

  private static final int GLCoordWidth = 2;
  private static final int GLCoordHeight = 2;

  private int screenWidth;
  private int screenHeight;

  private Context context;

  // constructor
  public VerticesHelper(Context context, int screenWidth, int screenHeight) {
    this.context = context;
    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
  }

  private float convertToGLCoord(int input, int length) {
    if (input == 0) return -1;
    return (float)input / length * 2 - 1;  //length is either screenWidth or Height
  }

  // currently this is used for adding watermark at the bottom right corner
  public float[] getWatermarkVertices(int watermarkId, int padding) {
    // image [width, height]
    //int imgWidth = 100; //50
    //int imgHeight = 132; //66
    // image [width, height]
    int[] imgDimen = getImageDimen(watermarkId);

    float z = 0.0f;
    float x1 = convertToGLCoord(screenWidth - imgDimen[0] - padding, screenWidth); // L
    float x2 = convertToGLCoord(screenWidth - padding, screenWidth); // R
    float y1 = convertToGLCoord(padding, screenHeight); //B
    float y2 = convertToGLCoord(imgDimen[1] + padding, screenHeight); //T

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



  // returns vertices that is centered, and scale to match width while keeping aspect ratio
  public float[] getAlignCenterVertices(int drawableId) {
    // image [width, height]
    int[] imgDimen = getImageDimen(drawableId);
    return calculateAlignCenterVertices(imgDimen[0], imgDimen[1]);
  }

  public float[] getAlignCenterVertices(String imgPath) {
    // image [width, height]
    int[] imgDimen = getImageDimen(imgPath);
    return calculateAlignCenterVertices(imgDimen[0], imgDimen[1]);
  }

  private float[] calculateAlignCenterVertices(int imageWidth, int imageHeight) {
    // init height coordinate in GL coordinate
    float initHeightCoordinate = (float)imageHeight / screenHeight;

    // screen width to image width ratio  //measure the amount of width we need to shrink/stretch
    float imgScreenWidthRatio = (float)screenWidth/imageWidth;

    Log.d(TAG, "imgScreenWidthRatio="+imgScreenWidthRatio+", initHeightCoordinate="+initHeightCoordinate);

    float z = 0.0f;
    float x1 = -1.0f;  // left
    float x2 = 1.0f;  // right
    float y1 = -1.0f * initHeightCoordinate * imgScreenWidthRatio;  // bottom
    float y2 = 1.0f * initHeightCoordinate * imgScreenWidthRatio;   // top

    float[] verticesCoord = {
      x1, y1, z,  //BL
      x2, y1, z,  //BR
      x2, y2, z,  //TR

      x1, y1, z,  //BL
      x2, y2, z,  //TR
      x1, y2, z   //TL
    };

    for(int i=0; i<verticesCoord.length; i=i+3) {
      Log.d(TAG, "center verticesCoord(" + verticesCoord[i] + ", " + verticesCoord[i+1] + ", " + verticesCoord[i+2] + ")");
    }

    return verticesCoord;
  }

  /*
  // get padding's width and length in GL coord
  private float[] getPaddingInGLCoord(int padding) {
    float paddingRL = (float)padding / screenWidth;
    float paddingTB = (float)padding / screenHeight;
    Log.d(TAG, "padding dimen: (" + paddingRL + ", " + paddingTB + ")");
    return new float[]{paddingRL, paddingTB};
  }

  // get image's width and height
  private float[] getImageDimenInGLCoord() {
    //[width, height]
    int imgWidth = 100;
    int imgHeight = 100;

    // convert it to GL coordinate width/height
    float imgWidthInGLCoord = (float)imgWidth / screenWidth * GLCoordWidth;
    float imgHeightInGLCoord = (float)imgHeight / screenHeight * GLCoordHeight;

    Log.d(TAG, "Image dimen: (" + imgWidthInGLCoord + ", " + imgHeightInGLCoord + ")");
    return new float[]{imgWidthInGLCoord, imgHeightInGLCoord};
  }
  */

  //
  private int[] getImageDimen(int drawableId) {
    Drawable d = context.getResources().getDrawable(drawableId);
    int h = d.getIntrinsicHeight();
    int w = d.getIntrinsicWidth();
    Log.d(TAG, "getImageDimen: width=" + w + ", height=" + h);
    return new int[]{w,h};
  }


  private int[] getImageDimen(String imgPath){
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(imgPath, options);

    int h = options.outHeight;
    int w = options.outWidth;
    Log.d(TAG, "getImageDimen: width=" + w + ", height=" + h);

    if (h != 0) return new int[]{w,h};

    try {
      InputStream is = new URL(imgPath).openStream();
      Bitmap bitmap = BitmapFactory.decodeStream(is);
      w = bitmap.getWidth();
      h = bitmap.getHeight();
      Log.d(TAG, "getImageDimen: bitmap = " + bitmap);
      Log.d(TAG, "getImageDimen: bitmap.width=" + bitmap.getWidth()+ ", bitmap.height=" + bitmap.getHeight());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return new int[]{w,h};
  }

}
