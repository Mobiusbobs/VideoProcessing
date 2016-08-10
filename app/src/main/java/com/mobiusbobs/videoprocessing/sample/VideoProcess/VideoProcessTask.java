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
import com.mobiusbobs.videoprocessing.core.gif.GifDecoder;
import com.mobiusbobs.videoprocessing.core.gldrawer.GLDrawable;
import com.mobiusbobs.videoprocessing.core.gldrawer.GifDrawer;
import com.mobiusbobs.videoprocessing.sample.R;
import com.mobiusbobs.videoprocessing.sample.Util.Logger;
import com.mobiusbobs.videoprocessing.sample.Util.MediaFileHelper;
import com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing.CoordConverter;
import com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing.Duration;
import com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing.DurationGLDrawable;
import com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing.DurationGifDrawable;
import com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing.MediaMetaHelper;
import com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing.StickerDrawer;
import com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing.TextDrawer;
import com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing.PetTagDrawer;
import com.mobiusbobs.videoprocessing.sample.VideoProcess.videoProcessing.WatermarkDrawer;


import java.io.File;
import java.io.IOException;

/**
 * android
 * <p/>
 * Created by wangalbert on 8/9/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 *
 * The output video is stored inside externalStorage/Movie/Test/ directory
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

  // --- device info related --- //
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

  // --- open movie directory --- //
  public void openMovieDirectory(Activity activity) {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    Uri uri = Uri.parse(MediaFileHelper.getOutputDirectoryPath().getAbsolutePath());
    Log.d(TAG, "openMovieDirectory: uri = " + uri.getPath());

    intent.setAction(Intent.ACTION_GET_CONTENT);
    intent.setDataAndType(uri, "video/*");
    activity.startActivity(Intent.createChooser(intent, "Open folder"));
  }


  // --- import file --- //
  public void requestImportFile(int requestCode) {
    //http://stackoverflow.com/questions/31044591/how-to-select-a-video-from-the-gallery-and-get-its-real-path
    Intent intent = new Intent();
    intent.setType("video/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    activity.startActivityForResult(Intent.createChooser(intent, "Select Video"), requestCode);
  }

  // --- encoder / decoder test --- //


  // --- video processing test --- //
  public void runVideoDecodeEncode(String inputFilePath) {
    // get output file path
    final String fileOutputPath = createNewFileOutput();
    if (fileOutputPath == null) return;

    // video processing - empty
    try {
      VideoProcessor.Builder builder = new VideoProcessor.Builder()
        .setInputFilePath(inputFilePath)
        .setOutputPath(fileOutputPath);

      runVideoProcessor(builder);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void runVideoProcessWithWatermark(String inputFilePath) {
    // get output file path
    final String fileOutputPath = createNewFileOutput();
    if (fileOutputPath == null) return;

    // init coordconverter
    CoordConverter coordConverter = new CoordConverter(context);
    final long videoDuration = MediaMetaHelper.getMediaDuration(context, Uri.parse(inputFilePath));

    // watermark
    GLDrawable watermarkDrawer = createWatermarkDrawer(coordConverter, videoDuration);

    // video processing
    try {
      VideoProcessor.Builder builder = new VideoProcessor.Builder()
        .setInputFilePath(inputFilePath)
        .addDrawer(watermarkDrawer)
        .setOutputPath(fileOutputPath);

      runVideoProcessor(builder);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void runVideoProcessWithTextSticker(String inputFilePath) {
    // get output file path
    final String fileOutputPath = createNewFileOutput();
    if (fileOutputPath == null) return;

    // init coordconverter
    CoordConverter coordConverter = new CoordConverter(context);
    final long videoDuration = MediaMetaHelper.getMediaDuration(context, Uri.parse(inputFilePath));

    // textSticker
    DurationGLDrawable textDrawer =  createTextDrawer(coordConverter, videoDuration);

    // video processing
    try {
      VideoProcessor.Builder builder = new VideoProcessor.Builder()
        .setInputFilePath(inputFilePath)
        .addDrawer(textDrawer)
        .setOutputPath(fileOutputPath);

      runVideoProcessor(builder);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // TODO implement sampleSize
  public void runVideoProcessWithImageSticker(String inputFilePath) {
    // get output file path
    final String fileOutputPath = createNewFileOutput();
    if (fileOutputPath == null) return;

    // init coordconverter
    CoordConverter coordConverter = new CoordConverter(context);
    final long videoDuration = MediaMetaHelper.getMediaDuration(context, Uri.parse(inputFilePath));

    // image sticker
    DurationGLDrawable stickerDrawer = createImageDrawer(coordConverter, videoDuration);

    // video processing
    try {
      VideoProcessor.Builder builder = new VideoProcessor.Builder()
        .setInputFilePath(inputFilePath)
        .addDrawer(stickerDrawer)
        .setOutputPath(fileOutputPath);

      runVideoProcessor(builder);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // TODO implement sampleSize
  public void runVideoProcessWithGifSticker(String inputFilePath) {
    // get output file path
    final String fileOutputPath = createNewFileOutput();
    if (fileOutputPath == null) return;

    // init coordconverter
    CoordConverter coordConverter = new CoordConverter(context);
    final long videoDuration = MediaMetaHelper.getMediaDuration(context, Uri.parse(inputFilePath));

    // gif Sticker
    DurationGLDrawable gifDrawer =  createGifDrawer(coordConverter, videoDuration);

    // video processing
    try {
      VideoProcessor.Builder builder = new VideoProcessor.Builder()
        .setInputFilePath(inputFilePath)
        .addDrawer(gifDrawer)
        .setOutputPath(fileOutputPath);

      runVideoProcessor(builder);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void runVideoProcessWithPetTag(String inputFilePath) {
    // get output file path
    final String fileOutputPath = createNewFileOutput();
    if (fileOutputPath == null) return;

    // init coordconverter
    CoordConverter coordConverter = new CoordConverter(context);

    //  petTag drawer
    PetTagDrawer petTagDrawer = createPetTagDrawer(coordConverter);

    // video processing
    try {
      VideoProcessor.Builder builder = new VideoProcessor.Builder()
        .setInputFilePath(inputFilePath)
        .addDrawer(petTagDrawer)
        .setOutputPath(fileOutputPath);

      runVideoProcessor(builder);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void runVideoProcessWithAllFeature(String inputFilePath) {
    // get output file path
    final String fileOutputPath = createNewFileOutput();
    if (fileOutputPath == null) return;

    // init coordconverter
    CoordConverter coordConverter = new CoordConverter(context);
    final long videoDuration = MediaMetaHelper.getMediaDuration(context, Uri.parse(inputFilePath));

    // watermark
    GLDrawable watermarkDrawer = createWatermarkDrawer(coordConverter, videoDuration);
    // textSticker
    DurationGLDrawable textDrawer =  createTextDrawer(coordConverter, videoDuration);
    // image sticker
    DurationGLDrawable stickerDrawer = createImageDrawer(coordConverter, videoDuration);
    // gif Sticker
    DurationGLDrawable gifDrawer =  createGifDrawer(coordConverter, videoDuration);
    //  petTag drawer
    PetTagDrawer petTagDrawer = createPetTagDrawer(coordConverter);

    // video processing
    try {
      VideoProcessor.Builder builder = new VideoProcessor.Builder()
        .setInputFilePath(inputFilePath)
        .addDrawer(watermarkDrawer)
        .addDrawer(textDrawer)
        .addDrawer(stickerDrawer)
        .addDrawer(gifDrawer)
        .addDrawer(petTagDrawer)
        .setOutputPath(fileOutputPath);

      runVideoProcessor(builder);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // --- helper method for video process --- //
  private String createNewFileOutput() {
    File fileOutput = MediaFileHelper.getOutputMediaFile(MediaFileHelper.MEDIA_TYPE_VIDEO);
    if (fileOutput == null) {
      Log.e(TAG, "Can not get output media file path");
      return null;
    }
    return fileOutput.toString();
  }

  // --- create different drawer --- //
  private GLDrawable createWatermarkDrawer(CoordConverter coordConverter, long videoDuration) {
    int watermarkWidth = 223 * 2;
    int watermarkHeight = 52 * 2;
    float[] watermarkVertices = coordConverter.getVertices(
      (VideoProcessor.OUTPUT_VIDEO_WIDTH - watermarkWidth) / 2,
      (VideoProcessor.OUTPUT_VIDEO_HEIGHT - watermarkHeight) / 2,
      watermarkWidth,
      watermarkHeight
    );
    WatermarkDrawer watermarkDrawer = new WatermarkDrawer(context, watermarkVertices);
    watermarkDrawer.setDuration(new Duration(videoDuration - 100, videoDuration + 1000));
    return watermarkDrawer;
  }

  private PetTagDrawer createPetTagDrawer(CoordConverter coordConverter) {
    int pawId = R.drawable.icon_paw;
    return new PetTagDrawer(
      context,
      coordConverter,
      CoordConverter.IDEAL_WIDTH,
      "PetTagName",
      pawId
    );
  }

  private DurationGLDrawable createGifDrawer(CoordConverter coordConverter, long videoDuration) {
    int sampleSize = 1;
    int gifResId = R.drawable.gif_happiness;
    float[] vertices = coordConverter.getVertices(200, 200, 400, 300); //(x, y, width, height)
    GifDecoder gifDecoder = GifDrawer.createGifDecoder(context, gifResId, sampleSize);
    DurationGLDrawable gifDrawer =  new DurationGifDrawable(context, gifDecoder, vertices);
    gifDrawer.setDuration(new Duration(0, videoDuration));
    return gifDrawer;
  }

  private DurationGLDrawable createImageDrawer(CoordConverter coordConverter, long videoDuration) {
    // image sticker
    int sampleSize = 1;
    float[] vertices = coordConverter.getVertices(200, 200, 300, 300); //(x, y, width, height)
    DurationGLDrawable stickerDrawer = new StickerDrawer(context, R.drawable.sticker_hungry, vertices, sampleSize);
    stickerDrawer.setDuration(new Duration(0, videoDuration));
    return stickerDrawer;
  }

  private DurationGLDrawable createTextDrawer(CoordConverter coordConverter, long videoDuration) {
    DurationGLDrawable textDrawer =  new TextDrawer(context, coordConverter, "textSticker", 30, "#00FFFF", 100, 200);
    textDrawer.setDuration(new Duration(0, videoDuration));
    return textDrawer;
  }

  private void runVideoProcessor(VideoProcessor.Builder builder) throws IOException {
    VideoProcessor videoProcessor = builder.build(context);

    ProcessorRunner.run(videoProcessor, "Process video", new ProcessorRunner.ProcessorRunnerCallback() {
      @Override
      public void onCompleted() {
        Logger.toastLong(activity, "video processing complete!");
      }

      @Override
      public void onError(Throwable e) {
        Logger.toastLong(activity, "video processing fail! Please check log for more detail!");
        Log.e(TAG, "VideoProcess... fail... OnError = " + e);
      }
    });
  }
}
