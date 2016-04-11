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

    // ----- TODO these should move to processor builder -----
    private String outputPath;

    private int inputRawFileId;
    private String inputFilePath;

    private String stickerFilePath;

    private int watermarkId;
    // ----- TODO these should move to processor builder -----

    public ProcessorRunner(VideoProcessor videoProcessor, ProcessorRunnerCallback callback) {
        this.videoProcessor = videoProcessor;
        this.callback = callback;
    }

        // TODO move into video processor
//        this.outputPath = outputPath;
//        this.inputRawFileId = inputRawFileId;
//        this.inputFilePath = inputFilePath;
//        this.stickerFilePath = stickerFilePath;
//        this.watermarkId = watermarkId;

    @Override
    public void run() {
//            videoProcessor.extractDecodeEditEncodeMux(outputPath, inputRawFileId);
        callback.onCompleted();
    }

    // TODO
//        ExtractDecodeEditEncodeMuxWrapper wrapper =
//                new ExtractDecodeEditEncodeMuxWrapper(codecManager, outputPath, inputRawFileId);
//        ExtractDecodeEditEncodeMuxWrapper wrapper =
//                new ExtractDecodeEditEncodeMuxWrapper(videoProcessor, outputPath, inputFilePath, stickerFilePath, watermarkId);

    public static void run(VideoProcessor videoProcessor, String threadName, ProcessorRunnerCallback cb) {
        ProcessorRunner runner = new ProcessorRunner(videoProcessor, cb);
        new Thread(runner, threadName).start();
    }

    public interface ProcessorRunnerCallback {
        void onCompleted();
    }
}
