package com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.mobiusbobs.videoprocessing.core.gldrawer.BasicDrawer;
import com.mobiusbobs.videoprocessing.core.gldrawer.GLDrawable;

import java.io.IOException;

/**
 * android
 * <p/>
 * Created by rayshih on 5/18/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class TextDrawer implements DurationGLDrawable {
//    public static String TAG = "TextDrawer";

    private String text;
    private double fontSize;
    private int textColor;
    private int x;
    private int y;

    private float scale;
    private BasicDrawer basicDrawer;
    private CoordConverter coordConverter;
    private Duration duration;

    public TextDrawer(
            Context context,
            CoordConverter coordConverter,
            String text,
            double fontSize,
            String textColorString,
            int x,
            int y
    ) {
        this.basicDrawer = new BasicDrawer(context);
        this.text = text;
        this.fontSize = fontSize;
        this.textColor = Color.parseColor(textColorString);
        this.x = x;
        this.y = y;

        scale = context.getResources().getDisplayMetrics().density;
        this.coordConverter = coordConverter;
    }

    @Override
    public void setRotate(float rotateInDeg) {
        basicDrawer.setRotate(rotateInDeg);
    }

    @Override
    public void init(GLDrawable prevDrawer) throws IOException {
        int fontSizeInPixel = (int) (fontSize * scale + 0.5f);

        // prepare text paint
        Paint paint = new Paint();
        paint.setTextSize(fontSizeInPixel);
        paint.setAntiAlias(true);
        paint.setColor(textColor);

        // get text size
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int width = bounds.width();
        int height = bounds.height();
        int left = bounds.left;
        int top = bounds.top;

        // setup coordinate
        float[] vertices = coordConverter.getVertices(x, y, width, height);
        basicDrawer.setVerticesCoordinate(vertices);

        // setup bitmap
        Bitmap bitmap = generateBitmap(width, height, left, top, paint);
        basicDrawer.init(prevDrawer, bitmap);
        bitmap.recycle();
    }

    @Override
    public void draw(long timeMs) {
        Boolean shouldDraw = duration == null || duration.check(timeMs);
        if (shouldDraw) {
            basicDrawer.draw(timeMs);
        } else {
            basicDrawer.drawBackground(timeMs);
        }
    }

    @Override
    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    private Bitmap generateBitmap(int width, int height, int left, int top, Paint paint) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        bitmap.eraseColor(0);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(text, - left, - top, paint);

        return bitmap;
    }
}
