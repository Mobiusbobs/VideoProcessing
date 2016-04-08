package com.mobiusbobs.videoprocessing.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;


/**
 * android
 * <p/>
 * Created by wangalbert on 4/8/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class TextDrawer {
  private static final String TAG = "TextDrawer";

  // composition
  private StickerDrawer stickerDrawer;

  private Context context;

  // Constructor
  public TextDrawer(Context context, String text) {
    this.context = context;

    stickerDrawer = new StickerDrawer(context);
    stickerDrawer.init();

    Bitmap bitmap = generateBitmap(text);
    stickerDrawer.loadTexture(bitmap, 1);
    bitmap.recycle();
  }

  private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
    Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
    Canvas canvas = new Canvas(bmOverlay);
    canvas.drawBitmap(bmp1, new Matrix(), null);
    canvas.drawBitmap(bmp2, new Matrix(), null);
    return bmOverlay;
  }

  public Bitmap generateBitmap(String text) {
    Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);
    Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
      R.drawable.icon_paw);

    // get a canvas to paint over the bitmap
    Canvas canvas = new Canvas(bitmap);
    bitmap.eraseColor(0);

    canvas.drawBitmap(icon, new Matrix(), null);

    // Draw the text
    Paint textPaint = new Paint();
    textPaint.setTextSize(32);
    textPaint.setAntiAlias(true);
    textPaint.setARGB(0xff, 0x00, 0x00, 0x00);
    // draw the text centered
    canvas.drawText(text, 16, 112, textPaint);

    return bitmap;
  }

  public void draw()  {
    stickerDrawer.draw();
  }
  
}
