package com.mobiusbobs.videoprocessing.sample.VideoProcessingTest.videoProcessing;

import android.content.Context;

import com.mobiusbobs.videoprocessing.core.gif.GifDecoder;
import com.mobiusbobs.videoprocessing.core.gldrawer.GifDrawer;

/**
 * android
 * <p/>
 * Created by rayshih on 5/19/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class DurationGifDrawable extends GifDrawer implements DurationGLDrawable {

    private Duration duration;

    public DurationGifDrawable(Context context, GifDecoder gifDecoder, float[] verticesPositionData) {
        super(context, gifDecoder, verticesPositionData);
    }

    @Override
    public void setDuration(Duration d) {
        duration = d;
    }

    @Override
    public void draw(long timeMs) {
        basicDrawer.drawBackground(timeMs);

        Boolean shouldDraw = duration == null || duration.check(timeMs);
        if (shouldDraw) {
            int textureIndex = updateFrameIndex(timeMs);
            basicDrawer.drawThisOnly(textureIndex);
        }
    }
}
