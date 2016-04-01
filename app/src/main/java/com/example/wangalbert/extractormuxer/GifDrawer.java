package com.example.wangalbert.extractormuxer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.example.wangalbert.extractormuxer.gif.GifDecoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * android
 * <p>
 * Created by wangalbert on 3/28/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class GifDrawer extends StickerDrawer {
  private static final String TAG = "GifDrawer";

  private int[] textureHandle = new int[30];
  private GifDecoder gifDecoder;
  private long gifStartTime;
  private int[] gifDimen = new int[2];

  public GifDrawer(Context context, GifDecoder gifDecoder) {
    super(context);

    setupGifDecoder(gifDecoder);

    initCoordinateBuffer();
    setupProjectionMatrix();
    setupViewMatrix();

    loadTextures(gifDecoder.getFrameCount());
    setupShader();
    bindTexture();
  }

  public GifDrawer(Context context, GifDecoder gifDecoder, float[] verticesPositionData) {
    super(context, verticesPositionData);

    initCoordinateBuffer();
    setupProjectionMatrix();
    setupViewMatrix();

    setupGifDecoder(gifDecoder);
    loadTextures(gifDecoder.getFrameCount());
    setupShader();
    bindTexture();
  }

  public static GifDecoder createGifDecoder(Context context, int rawGifId) {
    // open gif file as inputSource
    InputStream inputStream = context.getResources().openRawResource(rawGifId);

    GifDecoder gifDecoder = new GifDecoder();
    gifDecoder.read(inputStream, 0);

    return gifDecoder;
  }

  public static GifDecoder createGifDecoder(Context context, String filePath) {
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
      gifDecoder.advance();
      //bitmap.recycle();
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

    // destroy the bitmap
    bitmap.recycle();
  }

  private int updateFrameIndex(long timeMs) {
    long now = timeMs;

    if (gifStartTime == 0) {
      gifStartTime = now;
    }

    int delay = gifDecoder.getNextDelay();
    long relTime = now - gifStartTime;
    while(relTime >= delay)  {
      gifStartTime = now - (relTime - delay);
      gifDecoder.advance();
      delay = gifDecoder.getNextDelay();
      relTime = now - gifStartTime;
    }

    Log.d(TAG, "current index=" + gifDecoder.getCurrentFrameIndex());
    return gifDecoder.getCurrentFrameIndex();
  }

  public void drawGif(long timeMs) {
    GLES20.glUseProgram(shaderProgramHandle);

    // Set the active texture unit to texture unit 0.
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    // TODO this is where we decide which frame to draw
    // Bind the texture to this unit.
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[updateFrameIndex(timeMs)]);
    // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
    GLES20.glUniform1i(mTextureUniformHandle, 0);

    // Pass in the position information
    verticesPosition.position(0);
    GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATASIZE, GLES20.GL_FLOAT, false,
      0, verticesPosition);
    GLES20.glEnableVertexAttribArray(mPositionHandle);

    // Pass in the texture coordinate information
    texturePosition.position(0);
    GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEXTURE_COORD_DATASIZE, GLES20.GL_FLOAT, false,
      0, texturePosition);
    GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

    // blend
    GLES20.glEnable(GLES20.GL_BLEND);
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

    // set the matrix
    Matrix.setIdentityM(mModelMatrix, 0);
    Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
    Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

    // Draw the sticker.
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
  }

}
