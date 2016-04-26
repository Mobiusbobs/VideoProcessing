package com.mobiusbobs.videoprocessing.core.gldrawer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.mobiusbobs.videoprocessing.core.util.CoordConverter;

import java.io.IOException;

/**
 * android
 * <p/>
 * Created by wangalbert on 4/8/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class TextDrawer implements GLDrawable {
  private static final String TAG = "TextDrawer";

  // composition
  private BaseDrawer stickerDrawer;

  private Context context;
  private int videoWidth;
  private CoordConverter converter;
  private String text;
  private int resId;

  // Constructor
  public TextDrawer(Context context, CoordConverter converter, int videoWidth, String text, int resId) {
    this.context = context;
    this.videoWidth = videoWidth;
    this.converter = converter;
    this.text = text;
    this.resId = resId;

    stickerDrawer = new BaseDrawer();
  }

  public void init() throws IOException {
    Bitmap bitmap = generateBitmap(text, resId);
    stickerDrawer.setVerticesCoordinate(converter.getAlignTopRightVertices(bitmap));
    stickerDrawer.init(bitmap);
    bitmap.recycle();
  }

  public Bitmap generateBitmap(String text, int resId) {
    float rightMargin = 15;
    int textShift = 5;

    // create paw bitmap
    Bitmap icon = BitmapFactory.decodeResource(context.getResources(), resId);

    // create the bitmap to draw on
    int iconHeight = icon.getHeight();
    int containerHeight = iconHeight * 2;
    Bitmap bitmap = Bitmap.createBitmap(videoWidth, containerHeight, Bitmap.Config.ARGB_4444);

    // get a canvas to paint over the bitmap
    Canvas canvas = new Canvas(bitmap);
    bitmap.eraseColor(0);

    // setup paint
    Rect textBounds = new Rect();
    Paint textPaint = new Paint();
    textPaint.setTextSize(50);
    textPaint.setAntiAlias(true);
    textPaint.setARGB(0xFF, 0xFF, 0xFF, 0xFF);
    textPaint.getTextBounds(text, 0, text.length(), textBounds);

    // draw text
    float textLength = textBounds.width();
    float textHeight = textBounds.height();
    canvas.drawText(
        text,
        videoWidth - (textLength + rightMargin),
        (containerHeight + textHeight) / 2 - textShift,
        textPaint
    );

    // draw paw image
    if (text.length() > 0) {
      float left = videoWidth - (icon.getWidth() + textLength + rightMargin);
      canvas.drawBitmap(icon, left, (containerHeight - icon.getHeight()) / 2, null);
    }

    return bitmap;
  }

  public void draw(long timeMs) {
    stickerDrawer.draw(0);
  }
}
