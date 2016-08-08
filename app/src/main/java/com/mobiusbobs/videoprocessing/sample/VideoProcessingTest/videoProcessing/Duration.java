package com.mobiusbobs.videoprocessing.sample.VideoProcessingTest.videoProcessing;

/**
 * android
 * <p/>
 * Created by rayshih on 5/19/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class Duration {
    public long from = -1;
    public long to = -1;

    public Duration() {
        
    }

    public Duration (long from, long to) {
        this.from = from;
        this.to = to;
    }

    public Boolean check(long t) {
        return t >= from && t <= to;
    }
}
