package com.mobiusbobs.videoprocessing.core;

/**
 * VideoProcessing
 * <p/>
 * Created by rayshih on 4/8/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class ProcessorRunner implements Runnable {
    private VideoProcessor videoProcessor;
    private ProcessorRunnerCallback callback;

    public ProcessorRunner(VideoProcessor videoProcessor, ProcessorRunnerCallback callback) {
        this.videoProcessor = videoProcessor;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            videoProcessor.process();
            callback.onCompleted();
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public static void run(VideoProcessor videoProcessor, String threadName, ProcessorRunnerCallback cb) {
        ProcessorRunner runner = new ProcessorRunner(videoProcessor, cb);
        new Thread(runner, threadName).start();
    }

    public interface ProcessorRunnerCallback {
        void onCompleted();
        void onError(Throwable e);
    }
}
