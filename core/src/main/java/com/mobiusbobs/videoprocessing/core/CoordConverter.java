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
 *
 *   Rect Coordinate                    GL Coordinate
 *
 * (0,h) --------- (w,h)          (-1, 1) --------- ( 1, 1)
 *      |         |                      |         |
 *      |         |                      |         |
 *      |         |                      |         |
 *      |         |          =>          |         |
 *      |         |                      |         |
 *      |         |                      |         |
 *      |         |                      |         |
 *      |         |                      |         |
 * (0,0) --------- (w,0)          (-1,-1) --------- ( 1,-1)
 *
 * Created by wangalbert on 3/23/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class CoordConverter {
  private static final String TAG = "CoordConverter";

  private int screenWidth;
  private int screenHeight;

  private Context context;

  // constructor
  public CoordConverter(Context context, int screenWidth, int screenHeight) {
    this.context = context;
    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
    Log.d(TAG, "screenWidth="+screenWidth + ", screenHeight="+screenHeight);
  }

  public float[] getAlignTopVertices(Bitmap bitmap, int marginTop) {
    int height = bitmap.getHeight();

    float x1 = rectCoordToGLCoord(0, screenWidth); // L
    float x2 = rectCoordToGLCoord(screenWidth, screenWidth); // R
    float y1 = rectCoordToGLCoord(screenHeight - (marginTop + height), screenHeight); //B
    float y2 = rectCoordToGLCoord(screenHeight - marginTop, screenHeight); //T

    float[] verticesCoord = getVerticesCoord(x1, y1, x2, y2);
    printVertices(verticesCoord);
    return verticesCoord;
  }

  // returns vertices that is centered, and scale to match width while keeping aspect ratio
  public float[] getAlignBtmRightVertices(int drawableId, int margin) {
    int[] imgDimen = getImageDimen(drawableId);
    return calcAlignBtmRightVertices(imgDimen, margin);
  }

  // align bottom right corner
  private float[] calcAlignBtmRightVertices(int[] imgDimen, int margin) {
    float x1 = rectCoordToGLCoord(screenWidth - imgDimen[0] - margin, screenWidth); // L
    float x2 = rectCoordToGLCoord(screenWidth - margin, screenWidth); // R
    float y1 = rectCoordToGLCoord(margin, screenHeight); //B
    float y2 = rectCoordToGLCoord(imgDimen[1] + margin, screenHeight); //T

    float[] verticesCoord = getVerticesCoord(x1, y1, x2, y2);
    printVertices(verticesCoord);
    return verticesCoord;
  }

  // returns vertices that is centered, and scale to match width while keeping aspect ratio
  public float[] getAlignCenterVertices(int drawableId) {
    // image [width, height]
    int[] imgDimen = getImageDimen(drawableId);
    return calcAlignCenterVertices(imgDimen[0], imgDimen[1]);
  }

  public float[] getAlignCenterVertices(String imgPath) {
    // image [width, height]
    int[] imgDimen = getImageDimen(imgPath);
    return calcAlignCenterVertices(imgDimen[0], imgDimen[1]);
  }

  // align center with width stretch to side while maintain aspect ratio
  private float[] calcAlignCenterVertices(int imageWidth, int imageHeight) {
    // init height coordinate in GL coordinate
    float initHeightCoordinate = (float)imageHeight / screenHeight;

    // screen width to image width ratio  //measure the amount of width we need to shrink/stretch
    float imgScreenWidthRatio = (float)screenWidth / imageWidth;

    Log.d(TAG, "imgScreenWidthRatio="+imgScreenWidthRatio+", initHeightCoordinate="+initHeightCoordinate);

    float x1 = -1.0f;  // left
    float x2 = 1.0f;  // right
    float y1 = -1.0f * initHeightCoordinate * imgScreenWidthRatio;  // bottom
    float y2 = 1.0f * initHeightCoordinate * imgScreenWidthRatio;   // top

    float[] verticesCoord = getVerticesCoord(x1,y1,x2,y2);
    printVertices(verticesCoord);
    return verticesCoord;
  }

  // convert a single rect coordinate to GL vertices
  // ex: convert x=0 to GL_vertices = -1
  private float rectCoordToGLCoord(int rectCoord, int length) {
    return (float)rectCoord / length * 2 - 1;  //length is either Width or Height
  }

  private void printVertices(float[] vertices)  {
    for (int i=0; i<vertices.length; i=i+3) {
      Log.d(TAG, "verticesCoord(" + vertices[i] + ", " + vertices[i+1] + ", " + vertices[i+2] + ")");
    }
  }

  private float[] getVerticesCoord(float x1, float y1, float x2, float y2) {
    float z = 0.0f;
    return new float[]{
      x1, y1, z,  //BL
      x2, y1, z,  //BR
      x2, y2, z,  //TR

      x1, y1, z,  //BL
      x2, y2, z,  //TR
      x1, y2, z   //TL
    };
  }

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
