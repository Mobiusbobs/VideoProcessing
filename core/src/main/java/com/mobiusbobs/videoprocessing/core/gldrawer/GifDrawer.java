package com.mobiusbobs.videoprocessing.core.gldrawer;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;


import com.mobiusbobs.videoprocessing.core.gif.GifDecoder;
import com.mobiusbobs.videoprocessing.core.program.TextureShaderProgram;

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
  private BaseDrawer stickerDrawer;

  // --- handler ---
  // textures
  private int[] textureHandle;

  private GifDecoder gifDecoder;
  private long gifLastFrameTime;

  public GifDrawer(Context context, GifDecoder gifDecoder) {
    stickerDrawer = new BaseDrawer();
    this.gifDecoder = gifDecoder;
  }

  public GifDrawer(Context context, GifDecoder gifDecoder, float[] verticesPositionData) {
    stickerDrawer = new BaseDrawer(verticesPositionData);
    this.gifDecoder = gifDecoder;
  }

  public void init() throws IOException {
    stickerDrawer.init();
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

  public static GifDecoder createGifDecoder(String filePath) {
    try {
      File gifFile = new File(URI.create(filePath));
      InputStream inputStream = new FileInputStream(gifFile);

      GifDecoder gifDecoder = new GifDecoder();
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
    textureHandle = new int[frameCount];
    GLES20.glGenTextures(frameCount, textureHandle, 0);

    Log.d(TAG, "TOTAL frame count = " + frameCount);

    // load bitmap into GL texture of textureHandle[i]
    for (int i=0; i<frameCount; i++) {
      Bitmap bitmap = gifDecoder.getNextFrame();
      loadBitmapToTexture(bitmap, i);
      bitmap.recycle();
      gifDecoder.advance();
    }
  }

  private void loadBitmapToTexture(Bitmap bitmap, int textureIndex) {
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[textureIndex]);

    // Set filtering
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

    // Load the bitmap into the bound texture.
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
  }

  private int updateFrameIndex(long currentTimeMs) {
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

  public int getTextureHandle(long timeMs) {
    int textureIndex = updateFrameIndex(timeMs);
    int textureHandleId = textureHandle[textureIndex];
    return textureHandleId;
  }

  public float[] getMVPMatrix() {
    return stickerDrawer.getMVPMatrix();
  }

  public void bindData(TextureShaderProgram textureShaderProgram) {
    stickerDrawer.bindData(textureShaderProgram);
  }

  public void draw(long timeMs) {
    stickerDrawer.draw();
  }

}
