package com.mobiusbobs.videoprocessing.core.gldrawer;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;


import com.mobiusbobs.videoprocessing.core.gif.GifDecoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * android
 * <p>
 * Created by wangalbert on 3/28/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class GifDrawer implements GLDrawable {
  private static final String TAG = "GifDrawer";

  // composition
  protected BasicDrawer basicDrawer;

  private GifDecoder gifDecoder;
  private long gifLastFrameTime;

  public GifDrawer(Context context, GifDecoder gifDecoder) {
    basicDrawer = new BasicDrawer(context);
    this.gifDecoder = gifDecoder;
  }

  public GifDrawer(Context context, GifDecoder gifDecoder, float[] verticesPositionData) {
    basicDrawer = new BasicDrawer(context, verticesPositionData);
    this.gifDecoder = gifDecoder;
  }

  @Override
  public void setRotate(float rotateInDeg) {
    basicDrawer.setRotate(rotateInDeg);
  }

  @Override
  public void init(GLDrawable prevDrawer) throws IOException {
    basicDrawer.init(prevDrawer);
    setupGifDecoder(gifDecoder);
    loadTextures(gifDecoder.getFrameCount());
  }

  // TODO refactor this
  public static GifDecoder createGifDecoder(Context context, int rawGifId) {
    // open gif file as inputSource
    InputStream inputStream = context.getResources().openRawResource(rawGifId);

    GifDecoder gifDecoder = new GifDecoder();
    gifDecoder.read(inputStream, 0);

    return gifDecoder;
  }

  public static GifDecoder createGifDecoder(Context context, int rawGifId, int sampleSize) {
    // open gif file as inputSource
    InputStream inputStream = context.getResources().openRawResource(rawGifId);

    GifDecoder gifDecoder = new GifDecoder(sampleSize);
    gifDecoder.read(inputStream, 0);

    return gifDecoder;
  }

  public static GifDecoder createGifDecoder(String filePath) {
    return createGifDecoder(filePath, 1);
  }

  public static GifDecoder createGifDecoder(String filePath, int sampleSize) {
    try {
      File gifFile = new File(URI.create(filePath));
      InputStream inputStream = new FileInputStream(gifFile);

      GifDecoder gifDecoder = new GifDecoder(sampleSize);
      gifDecoder.read(inputStream, 0);

      return gifDecoder;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private void setupGifDecoder(GifDecoder gifDecoder) {
    // by default, current frame index is -1, advance it.
    this.gifDecoder = gifDecoder;
    gifDecoder.resetFrameIndex();
    gifDecoder.advance();
  }

  public void loadTextures(int frameCount) {
    basicDrawer.setTextureHandleSize(frameCount);
    Log.d(TAG, "TOTAL frame count = " + frameCount);

    // load bitmap into GL texture of textureHandle[i]
    for (int i=0; i<frameCount; i++) {
      Bitmap bitmap = gifDecoder.getNextFrame();
      basicDrawer.loadBitmapToTexture(bitmap, i);
      bitmap.recycle();
      gifDecoder.advance();
    }
  }

  protected int updateFrameIndex(long currentTimeMs) {
    if (gifLastFrameTime == 0) {
      gifLastFrameTime = currentTimeMs;
    }

    int delay = gifDecoder.getNextDelay();

    while(currentTimeMs >= gifLastFrameTime + delay) {
      gifLastFrameTime += delay;
      gifDecoder.advance();
      delay = gifDecoder.getNextDelay();
    }

    return gifDecoder.getCurrentFrameIndex();
  }

  @Override
  public void draw(long timeMs) {
    basicDrawer.drawBackground(timeMs);

    int textureIndex = updateFrameIndex(timeMs);
    basicDrawer.drawThisOnly(textureIndex);
  }

}
