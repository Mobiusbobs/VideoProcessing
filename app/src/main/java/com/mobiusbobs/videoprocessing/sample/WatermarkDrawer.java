package com.mobiusbobs.videoprocessing.sample;

import android.content.Context;
import android.graphics.Bitmap;

import com.mobiusbobs.videoprocessing.core.util.CoordConverter;
import com.mobiusbobs.videoprocessing.core.gldrawer.GLDrawable;
import com.mobiusbobs.videoprocessing.core.gldrawer.BasicDrawer;
import com.mobiusbobs.videoprocessing.core.gldrawer.GLDrawable;
import com.mobiusbobs.videoprocessing.core.util.BitmapHelper;

import java.io.IOException;

/**
 * VideoProcessing
 * <p/>
 * Created by rayshih on 4/13/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class WatermarkDrawer implements GLDrawable {
    private Context context;
    private BasicDrawer drawer;
    private static int WATERMARK_ID = R.drawable.logo_watermark;

    public WatermarkDrawer(Context context, CoordConverter coordConverter) {
        this.context = context;
//        drawer = new BasicDrawer(
//                coordConverter.getAlignBtmRightVertices(WATERMARK_ID, 30)
//        );
    }

    public void init() throws IOException {
        Bitmap bitmap = BitmapHelper.generateBitmap(context, WATERMARK_ID);
//        drawer.init(bitmap);
        bitmap.recycle();
    }

    @Override
    public void setRotate(float rotateInDeg) {

    }

    @Override
    public void init(GLDrawable prevDrawer) throws IOException {

    }

    @Override
    public void draw(long timeMs) {
        drawer.draw(timeMs);
    }
}
