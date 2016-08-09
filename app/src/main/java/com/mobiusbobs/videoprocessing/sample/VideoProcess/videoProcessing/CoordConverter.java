package com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing;

import android.content.Context;

/**
 * android
 * <p/>
 * Created by rayshih on 4/19/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class CoordConverter extends com.mobiusbobs.videoprocessing.core.util.CoordConverter {
    public static String TAG = "CoordConverter";
    public static int IDEAL_WIDTH = 720;
    public static int IDEAL_HEIGHT = 1280;
    public static float IDEAL_RATIO = (float)IDEAL_HEIGHT / IDEAL_WIDTH;

    // output
    private int width;
    private int height;
    private float ratio;

    public CoordConverter(Context context) {
        this(context, IDEAL_WIDTH, IDEAL_HEIGHT);
    }

    public CoordConverter(Context context, int width, int height) {
        super(context, width, height);
        this.width = width;
        this.height = height;
        ratio = (float)height / width;
    }

    public float[] getVertices(int x, int y, int w, int h) {
        float x1 = ccH(x); // L
        float x2 = ccH(x + w); // R
        float y1 = ccV(IDEAL_HEIGHT - (y + h)); // B
        float y2 = ccV(IDEAL_HEIGHT - y); // T

        return getVerticesCoord(x1, y1, x2, y2);
    }

    // convert coordinate horizontal
    private int ccH(int origin) {
        int targetWidth;
        if (ratio > IDEAL_RATIO) {
            targetWidth = width;
        } else {
            targetWidth = (int)((float) height / IDEAL_RATIO);
        }
        int offset = (width - targetWidth) / 2;

        return (int)((float) origin / IDEAL_WIDTH * targetWidth) + offset;
    }

    // convert coordinate vertical
    private int ccV(int origin) {
        int targetHeight;
        if (ratio > IDEAL_RATIO) {
            targetHeight = (int)((float) width * IDEAL_RATIO);
        } else {
            targetHeight = height;
        }
        int offset = (height - targetHeight) / 2;

        return (int)((float) origin / IDEAL_HEIGHT * targetHeight) + offset;
    }
}
