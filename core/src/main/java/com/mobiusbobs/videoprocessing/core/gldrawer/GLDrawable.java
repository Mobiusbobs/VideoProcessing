package com.mobiusbobs.videoprocessing.core.gldrawer;

import java.io.IOException;

/**
 * VideoProcessing
 * <p>
 * Created by rayshih on 4/11/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public interface GLDrawable {
    // TODO remove this
    void setRotate(float rotateInDeg);

    /**
     * init any OpenGL related things here
     * @throws IOException
     */
    void init(GLDrawable prevDrawer) throws IOException;
    void draw(long timeMs);
}
