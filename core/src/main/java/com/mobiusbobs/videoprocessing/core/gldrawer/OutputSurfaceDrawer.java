package com.mobiusbobs.videoprocessing.core.gldrawer;

import com.mobiusbobs.videoprocessing.core.gles.surface.OutputSurface;

import java.io.IOException;

/**
 * android
 *
 * This is just a wrapper of output surface to fit the drawer chain
 *
 * Created by rayshih on 6/11/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class OutputSurfaceDrawer implements GLDrawable {

    private OutputSurface outputSurface;

    public OutputSurfaceDrawer(OutputSurface outputSurface) {
       this.outputSurface = outputSurface;
    }

    @Override
    public void setRotate(float rotateInDeg) {
        // don't support rotate
    }

    @Override
    public void init(GLDrawable prevDrawer) throws IOException {
        // no need to init
    }

    @Override
    public void draw(long timeMs) {
        outputSurface.drawImage();
    }
}
