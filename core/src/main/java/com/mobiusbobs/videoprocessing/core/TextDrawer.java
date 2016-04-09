package com.mobiusbobs.videoprocessing.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

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
  private int screenWidth;

  // Constructor
  public TextDrawer(Context context, CoordConverter converter, int screenWidth, String text) {
    this.context = context;
    this.screenWidth = screenWidth;

    Bitmap bitmap = generateBitmap(text);
    stickerDrawer = new StickerDrawer(context);
    stickerDrawer.setVerticesCoordinate(converter.getAlignTopVertices(bitmap, 56));
    stickerDrawer.init();
    stickerDrawer.loadTexture(bitmap, 1);
    bitmap.recycle();
  }

  public Bitmap generateBitmap(String text) {
    float rightMargin = 15;
    int textShift = 5;

    // create paw bitmap
    Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
      R.drawable.icon_paw);

    // create the bitmap to draw on
    int iconHeight = icon.getHeight();
    int containerHeight = iconHeight * 2;
    Bitmap bitmap = Bitmap.createBitmap(screenWidth, containerHeight, Bitmap.Config.ARGB_4444);

    // get a canvas to paint over the bitmap
    Canvas canvas = new Canvas(bitmap);
    bitmap.eraseColor(0);

    // setup paint
    Rect textBounds = new Rect();
    Paint textPaint = new Paint();
    textPaint.setTextSize(50);
    textPaint.setAntiAlias(true);
    textPaint.setARGB(0x7C, 0x00, 0x00, 0x00);
    textPaint.getTextBounds(text, 0, text.length(), textBounds);

    // draw text
    float textLength = textBounds.width();
    float textHeight = textBounds.height();
    canvas.drawText(text, screenWidth - (textLength + rightMargin), (containerHeight + textHeight) / 2 - textShift, textPaint);

    // draw paw image
    float left = screenWidth - (icon.getWidth() + textLength + rightMargin);
    canvas.drawBitmap(icon, left, (containerHeight-icon.getHeight())/2, null);

    return bitmap;
  }

  public void draw()  {
    stickerDrawer.draw();
  }
}
