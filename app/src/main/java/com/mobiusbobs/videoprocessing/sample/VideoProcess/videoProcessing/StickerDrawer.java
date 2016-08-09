package com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing;

import android.content.Context;
import android.graphics.Bitmap;

import com.mobiusbobs.videoprocessing.core.gldrawer.BasicDrawer;
import com.mobiusbobs.videoprocessing.core.gldrawer.GLDrawable;
import com.mobiusbobs.videoprocessing.core.util.BitmapHelper;

import java.io.IOException;

/**
 * android
 * <p>
 * Created by wangalbert on 4/18/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class StickerDrawer implements DurationGLDrawable {
    protected BasicDrawer drawer;
    String stickerFilePath;
    protected Duration duration;
    private int sampleSize = 1;

    public StickerDrawer(Context context, float[] verticesPositionData) {
        drawer = new BasicDrawer(context, verticesPositionData);
    }

    public StickerDrawer(Context context, String stickerFilePath, float[] verticesPositionData) {
      this(context, stickerFilePath, verticesPositionData, 1);
    }

    public StickerDrawer(Context context, String stickerFilePath, float[] verticesPositionData, int sampleSize) {
        this(context, verticesPositionData);
        this.stickerFilePath = stickerFilePath;
        this.sampleSize = sampleSize;
    }

    @Override
    public void setRotate(float rotateInDeg) {
        drawer.setRotate(rotateInDeg);
    }

    public void setOpacity(float opacity) {
        drawer.setOpacity(opacity);
    }

    @Override
    public void init(GLDrawable prevDrawer) throws IOException {
        Bitmap bitmap = BitmapHelper.generateBitmap(stickerFilePath, sampleSize);
        drawer.init(prevDrawer, bitmap);
        bitmap.recycle();
    }

    @Override
    public void draw(long timeMs) {
        Boolean shouldDraw = duration == null || duration.check(timeMs);
        if (shouldDraw) {
            drawer.draw(timeMs);
        } else {
            drawer.drawBackground(timeMs);
        }
    }

    @Override
    public void setDuration(Duration d) {
        duration = d;
    }
}
