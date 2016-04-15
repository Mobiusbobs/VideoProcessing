package com.mobiusbobs.videoprocessing.core.codec;

import android.graphics.SurfaceTexture;
import android.media.MediaMuxer;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;

import java.io.File;
import java.io.IOException;

/**
 * android
 * <p>
 * Created by wangalbert on 3/30/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class VideoRecorder {
  public static final String TAG = "VideoRecorder";

  // video
  private static final int VIDEO_BITRATE = 6000000;
  private static final int VIDEO_WIDTH = 720;
  private static final int VIDEO_HEIGHT = 1280;

  // main component
  private AudioRecorder audioRecorder;
  private TextureMovieEncoder textureMovieEncoder;

  // flag
  private boolean isCapturing = false;
  private boolean isRecording = false;

  // File
  private File outputFile;

  // constructor
  public VideoRecorder(File outputFile) throws IOException {
    this.outputFile = outputFile;
    resetRecorder(outputFile);
  }

  // boolean
  private boolean startRecording = false;

  public void resetRecorder(File outputFile) throws IOException {
    // create muxer
    MediaMuxer muxer = new MediaMuxer(
      outputFile.toString(),
      MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
    );

    int numOfEncoder = 2;
    MuxerWrapper muxerWrapper = new MuxerWrapper(muxer, numOfEncoder);

    // create audioRecorder
    audioRecorder = new AudioRecorder(muxerWrapper);

    // create video encoder
    textureMovieEncoder = new TextureMovieEncoder(muxerWrapper);
  }

  public void startRecord(GLSurfaceView glView) {
    isCapturing = true;
    isRecording = true;

    // audioRecorder
    audioRecorder.startRecord();

    // textureMovieEncoder
    glView.queueEvent(new Runnable() {
      @Override
      public void run() {
        TextureMovieEncoder.EncoderConfig config =
          new TextureMovieEncoder.EncoderConfig(
            VIDEO_WIDTH,
            VIDEO_HEIGHT,
            VIDEO_BITRATE,
            EGL14.eglGetCurrentContext());
        textureMovieEncoder.startRecording(config);
      }
    });

  }

  public void resumeRecord(GLSurfaceView glView) {
    if (!isCapturing) {
      startRecord(glView);
    } else {
      isRecording = true;
      audioRecorder.resumeRecord();
      textureMovieEncoder.resumeRecording();
    }
  }

  public void pauseRecord() {
    isRecording = false;
    audioRecorder.pauseRecrod();
    textureMovieEncoder.pauseRecording();
  }

  public void stopRecord() {
    isCapturing = false;
    isRecording = false;
    audioRecorder.stopRecord();
    textureMovieEncoder.stopRecording();
    release();
  }

  private void release() {
    if (audioRecorder != null) {
      audioRecorder.release();
    }
  }

  public boolean isRecording() {
    return isRecording;
  }

  public File getOutputFile() {
    return outputFile;
  }

  // ----- textureMovieEncoder related function -----
  public void onVideoFrameAvailable(int mTextureId, SurfaceTexture surfaceTexture) {
    if (!isRecording) return;

    textureMovieEncoder.setTextureId(mTextureId); // thanks to google suck code
    textureMovieEncoder.frameAvailable(surfaceTexture);
  }

  public void updateSharedContext(EGLContext sharedContext) {
    textureMovieEncoder.updateSharedContext(sharedContext);
  }

}
