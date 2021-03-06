package com.mobiusbobs.videoprocessing.sample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.mobiusbobs.videoprocessing.core.ProcessorRunner;
import com.mobiusbobs.videoprocessing.core.VideoProcessor;
import com.mobiusbobs.videoprocessing.core.util.Size;

import java.io.IOException;

/**
 * VideoProcessing
 * <p/>
 * Created by rayshih on 9/21/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class CutVideoActivity extends AppCompatActivity {
  public static final String TAG = "CutVideoActivity";

  public static final int REQUEST_TAKE_GALLERY_VIDEO = 1;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.actiivty_video_processing_test);
    initView();
  }

  private void initView() {
    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    if (fab != null) {
      fab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          selectVideo();
        }
      });
    }
  }

  private void selectVideo() {
    Intent intent = new Intent();
    intent.setType("video/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    startActivityForResult(Intent.createChooser(
      intent,"Select Video"),
      REQUEST_TAKE_GALLERY_VIDEO
    );
  }

  @Override
  protected void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_OK) {
      if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
        Uri videoUri = data.getData();

        String sourcePath = Util.getRealPathFromURI(this, videoUri);
        String targetPath = sourcePath.replaceAll(
          "\\.([^.]*)$", "_processed.$1");

        Util.toastLong(this,
          "Cut video from " + sourcePath + " to " + targetPath);

        cutVideo(sourcePath, targetPath);
      }
    }
  }

  private void cutVideo(String sourcePath, String targetPath) {
    VideoProcessor videoProcessor;
    try {
      Size size = new Size(720, 1280);

      videoProcessor = new VideoProcessor.Builder(size)
        .setInputFilePath(sourcePath)
//        .setVideoExtendDurationUs(5 * 1000 * 1000)
        .setCutRange(5 * 1000 * 1000, 11 * 1000 * 1000)
        .setOutputFilePath(targetPath)
        .build(this);
    } catch (IOException e) {
      Util.toastLong(this, "Failed while building video processor");
      e.printStackTrace();

      return;
    }

    ProcessorRunner.run(
      videoProcessor, "Cut Video",
      new ProcessorRunner.ProcessorRunnerCallback() {
        @Override
        public void onCompleted() {
          Util.toastLong(CutVideoActivity.this,
            "Process complete");
        }

        @Override
        public void onError(Throwable e) {
          Util.toastLong(CutVideoActivity.this,
            "Process failed");

          Log.e(TAG, e.getMessage());
          e.printStackTrace();
        }
      }
    );
  }
}
