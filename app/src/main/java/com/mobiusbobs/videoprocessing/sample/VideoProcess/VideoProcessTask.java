package com.mobiusbobs.videoprocessing.sample.VideoProcess;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.mobiusbobs.videoprocessing.core.ProcessorRunner;
import com.mobiusbobs.videoprocessing.core.VideoProcessor;
import com.mobiusbobs.videoprocessing.core.codec.Extractor;
import com.mobiusbobs.videoprocessing.core.gldrawer.GLDrawable;
import com.mobiusbobs.videoprocessing.sample.Util.MediaFileHelper;
import com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing.CoordConverter;
import com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing.MediaMetaHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * android
 * <p/>
 * Created by wangalbert on 8/9/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class VideoProcessTask {
  public static final String TAG = "VideoProcessTask";

  private static final String OUTPUT_VIDEO_MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding

  private Activity activity;
  private Context context;

  public VideoProcessTask(Activity activity, Context context) {
    this.activity = activity;
    this.context = context;
  }

  // --- printing test --- //
  public void runPrintDeviceInfo() {
    Log.d(TAG, "runPrintDeviceInfo... ");
    MediaCodecChecker.dumpDeviceInfo();
  }

  public void runPrintMediaCondecInfo(String filePath) {
    Log.d(TAG, "runPrintMediaCodecInfo... filePath = " + filePath);

    // Extract file
    MediaExtractor videoExtractor = Extractor.createExtractor(filePath);
    int videoTrackIndex = Extractor.getAndSelectVideoTrackIndex(videoExtractor);
    MediaFormat videoFormat = videoExtractor.getTrackFormat(videoTrackIndex);

    // get mediaCodecInfo
    MediaCodecInfo videoCodecInfo = VideoProcessor.selectCodec(OUTPUT_VIDEO_MIME_TYPE);

    // check Media Codec for video capability with the given video format
    boolean pass = MediaCodecChecker.checkVideoCapabilitySupported(videoCodecInfo, videoFormat);
    if (pass)   {
      Toast.makeText(context, "PASS: \n" + filePath + " is supported", Toast.LENGTH_LONG).show();
    }   else    {
      String msg = "FAIL: \n" + filePath + " is not supported\nPlease check log for more detail";
      Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }
  }

  public void requestImportFile(int requestCode) {
    //http://stackoverflow.com/questions/31044591/how-to-select-a-video-from-the-gallery-and-get-its-real-path
    Intent intent = new Intent();
    intent.setType("video/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    activity.startActivityForResult(Intent.createChooser(intent, "Select Video"), requestCode);
  }

  // --- encoder / decoder test --- //


  // --- video processing test --- //
  public void runVideoProcess(String filePath) {
    Log.d(TAG, "runVideoProcess: filePath = ");

    // file output
    File fileOutput = MediaFileHelper.getOutputMediaFile(MediaFileHelper.MEDIA_TYPE_VIDEO, true);
    if (fileOutput == null) {
      Log.e(TAG, "Can not get output media file path");
      return;
    }
    Log.d(TAG, "fileOutput = " + fileOutput);

    // video info
    final String fileOutputPath = fileOutput.toString();

    try {
      VideoProcessor.Builder builder = new VideoProcessor.Builder()
        .setInputFilePath(filePath)
        .setOutputPath(fileOutputPath);

      VideoProcessor videoProcessor = builder.build(context);

      ProcessorRunner.run(videoProcessor, "Process video", new ProcessorRunner.ProcessorRunnerCallback() {
        @Override
        public void onCompleted() {
          Log.d(TAG, "VideoProcess... complete... success...");
        }

        @Override
        public void onError(Throwable e) {
          Log.e(TAG, "VideoProcess... fail... OnError = " + e);
        }
      });

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /*
  public void runVideoProcess(String filePath, String[] stickerList, String petname) {
    //String inputVideoFilePath = "";
    //String[] stickerList = {"Text Sticker", "ABCDE"};
    //String petName = "myPetName";
    Log.d(TAG, "runVideoProcess... filePath = ");

    // file output
    File fileOutput = MediaFileHelper.getOutputMediaFile(MediaFileHelper.MEDIA_TYPE_VIDEO, true);
    if (fileOutput == null) {
      Log.e(TAG, "Can not get output media file path");
      return;
    }
    Log.d(TAG, "fileOutput = " + fileOutput);

    // video info
    final String fileOutputPath = fileOutput.toString();
    final long videoDuration = MediaMetaHelper.getMediaDuration(this, Uri.parse(filePath));

    // coordinate converter
    CoordConverter coordConverter = new CoordConverter(this);

    // --- gl drawable (sticker / gif / text / watermark / etc.)
    // watermark

        int watermarkWidth = 223 * 2;
        int watermarkHeight = 52 * 2;
        float[] watermarkVertices = coordConverter.getVertices(
          (VideoProcessor.OUTPUT_VIDEO_WIDTH - watermarkWidth) / 2,
          (VideoProcessor.OUTPUT_VIDEO_HEIGHT - watermarkHeight) / 2,
          watermarkWidth,
          watermarkHeight
        );
        WatermarkDrawer watermarkDrawer = new WatermarkDrawer(this, watermarkVertices);
        watermarkDrawer.setDuration(new Duration(videoDuration - 100, videoDuration + 1000));


    List<GLDrawable> drawerList = new ArrayList<>();
    //drawerList.addAll(getStickerDrawerList(stickerList, coordConverter));
    //drawerList.add(topRightTextDrawer);
    //drawerList.add(watermarkDrawer);

    try {
      VideoProcessor.Builder builder = new VideoProcessor.Builder()
        .setInputFilePath(filePath)
        .setOutputPath(fileOutputPath);

      for (GLDrawable drawer: drawerList) {
        //builder.addDrawer(drawer);
      }

      VideoProcessor videoProcessor = builder.build(this);

      ProcessorRunner.run(videoProcessor, "Process video", new ProcessorRunner.ProcessorRunnerCallback() {
        @Override
        public void onCompleted() {
          Log.d(TAG, "VideoProcess... complete... success...");
        }

        @Override
        public void onError(Throwable e) {
          Log.e(TAG, "VideoProcess... fail... OnError = " + e);
        }
      });

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  */
}
