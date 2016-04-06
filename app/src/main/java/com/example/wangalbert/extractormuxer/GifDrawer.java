package com.example.wangalbert.extractormuxer;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.example.wangalbert.extractormuxer.gif.GifDecoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * android
 * <p>
 * Created by wangalbert on 3/28/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class GifDrawer {
  private static final String TAG = "GifDrawer";

  // composition
  private StickerDrawer stickerDrawer;

  private int[] textureHandle = new int[30];
  private GifDecoder gifDecoder;
  private long gifLastFrameTime;

  public GifDrawer(Context context, GifDecoder gifDecoder) {
    stickerDrawer = new StickerDrawer(context);
    this.gifDecoder = gifDecoder;
    init();
  }

  public GifDrawer(Context context, GifDecoder gifDecoder, float[] verticesPositionData) {
    stickerDrawer = new StickerDrawer(context, verticesPositionData);
    stickerDrawer.verticesPositionData = verticesPositionData;
    this.gifDecoder = gifDecoder;
    init();
  }

  private void init() {
    stickerDrawer.initCoordinateBuffer();

    // calculate matrix
    stickerDrawer.setupProjectionMatrix();
    stickerDrawer.setupViewMatrix();
    stickerDrawer.calculateMVPMatrix();

    setupGifDecoder(gifDecoder);
    loadTextures(gifDecoder.getFrameCount());
    stickerDrawer.setupShader();
    stickerDrawer.bindTexture();
  }

  public static GifDecoder createGifDecoder(Context context, int rawGifId) {
    // open gif file as inputSource
    InputStream inputStream = context.getResources().openRawResource(rawGifId);

    GifDecoder gifDecoder = new GifDecoder();
    gifDecoder.read(inputStream, 0);

    return gifDecoder;
  }

  public static GifDecoder createGifDecoder(String filePath) {
    try {
      File gifFile = new File(filePath);
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
    // generate n GL textures IDs => textureIDs[0], textureIDs[1]
    //textureHandle = new int[frameCount];
    GLES20.glGenTextures(frameCount, textureHandle, 0);

    Log.d(TAG, "TOTAL frame count = " + frameCount);
    // load bitmap into GL texture of textureHandle[i]
    for (int i=0; i<frameCount; i++) {
      Log.d(TAG, "loadTexture: i= " + i + ", index=" + gifDecoder.getCurrentFrameIndex() + ", delay = " + gifDecoder.getDelay(i));
      Bitmap bitmap = gifDecoder.getNextFrame();
      Log.d(TAG, "bitmap = " + bitmap);
      loadTexture(bitmap, i);
      bitmap.recycle();
      gifDecoder.advance();
    }
  }

  private void loadTexture(Bitmap bitmap, int textureIndex) {
    // tell OpenGL what is the current GL texture
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[textureIndex]);

    // Set up texture filters for current GL texture
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

    // load the bitmap into current GL texture
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
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

    Log.d(TAG, "current index=" + gifDecoder.getCurrentFrameIndex());
    return gifDecoder.getCurrentFrameIndex();
  }

  public void draw(long timeMs) {
    GLES20.glUseProgram(stickerDrawer.shaderProgramHandle);

    // Set the active texture unit to texture unit 0.
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    // TODO this is where we decide which frame to draw
    // Bind the texture to this unit.
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[updateFrameIndex(timeMs)]);
    // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
    GLES20.glUniform1i(stickerDrawer.mTextureUniformHandle, 0);

    // Pass in the position information
    stickerDrawer.verticesPosition.position(0);
    GLES20.glVertexAttribPointer(
      stickerDrawer.mPositionHandle,
      stickerDrawer.POSITION_DATASIZE,
      GLES20.GL_FLOAT,
      false,
      0,
      stickerDrawer.verticesPosition);
    GLES20.glEnableVertexAttribArray(stickerDrawer.mPositionHandle);

    // Pass in the texture coordinate information
    stickerDrawer.texturePosition.position(0);
    GLES20.glVertexAttribPointer(
      stickerDrawer.mTextureCoordinateHandle,
      stickerDrawer.TEXTURE_COORD_DATASIZE,
      GLES20.GL_FLOAT,
      false,
      0,
      stickerDrawer.texturePosition);
    GLES20.glEnableVertexAttribArray(stickerDrawer.mTextureCoordinateHandle);

    // blend
    GLES20.glEnable(GLES20.GL_BLEND);
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

    // set the matrix
    GLES20.glUniformMatrix4fv(stickerDrawer.mMVPMatrixHandle, 1, false, stickerDrawer.mMVPMatrix, 0);

    // Draw the sticker.
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
  }

}
