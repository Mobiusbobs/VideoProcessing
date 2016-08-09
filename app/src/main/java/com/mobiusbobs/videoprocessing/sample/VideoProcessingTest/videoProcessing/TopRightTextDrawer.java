package com.mobiusbobs.videoprocessing.sample.VideoProcessingTest.videoProcessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.mobiusbobs.videoprocessing.core.gldrawer.BasicDrawer;
import com.mobiusbobs.videoprocessing.core.gldrawer.GLDrawable;
import com.mobiusbobs.videoprocessing.core.util.CoordConverter;

import java.io.IOException;

/**
 * android
 * <p/>
 * Created by wangalbert on 4/8/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class TopRightTextDrawer implements GLDrawable {
  private static final String TAG = "TopRightTextDrawer";

  // composition
  private BasicDrawer stickerDrawer;

  private Context context;
  private int videoWidth;
  private CoordConverter converter;
  private String text;
  private int resId;
  private float scale;

  // Constructor
  public TopRightTextDrawer(Context context, CoordConverter converter, int videoWidth, String text, int resId) {
    this.context = context;
    this.videoWidth = videoWidth;
    this.converter = converter;
    this.text = text;
    this.resId = resId;

    scale = context.getResources().getDisplayMetrics().density;
    stickerDrawer = new BasicDrawer(context);
  }

  @Override
  public void setRotate(float rotateInDeg) {
    stickerDrawer.setRotate(rotateInDeg);
  }

  @Override
  public void init(GLDrawable prevDrawer) throws IOException {
    int marginTop = (int) (10 * scale);

    Bitmap bitmap = generateBitmap(text, resId);
    stickerDrawer.setVerticesCoordinate(converter.getAlignTopVertices(bitmap, marginTop));
    stickerDrawer.init(prevDrawer, bitmap);
    bitmap.recycle();
  }

  public Bitmap generateBitmap(String text, int resId) {
    float spaceRightMargin = 10.0f;
    float iconRightMargin = 2.0f;
    float textFontSize = 13.0f;

    // convert dp to pixel
    int spaceRightMarginPx = (int) (spaceRightMargin * scale + 0.5f);
    int iconRightMarginPx = (int) (iconRightMargin * scale + 0.5f);
    int textSizeInPixel = (int) (textFontSize * scale + 0.5f);

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
    textPaint.setTextSize(textSizeInPixel);
    textPaint.setAntiAlias(true);
    textPaint.setARGB(0xFF, 0xFF, 0xFF, 0xFF);
    textPaint.getTextBounds(text, 0, text.length(), textBounds);

    // draw text
    float textLength = textBounds.width();
    float textHeight = textBounds.height();
    canvas.drawText(
        text,
        videoWidth - (textLength + spaceRightMarginPx),
        (containerHeight + textHeight) / 2,
        textPaint
    );

    // draw paw image
    if (text.length() > 0) {
      float left = videoWidth - (icon.getWidth() + textLength + spaceRightMarginPx + iconRightMarginPx);
      canvas.drawBitmap(icon, left, (containerHeight - icon.getHeight()) / 2, null);
    }

    return bitmap;
  }

  public void draw(long timeMs) {
    stickerDrawer.draw(timeMs);
  }
}