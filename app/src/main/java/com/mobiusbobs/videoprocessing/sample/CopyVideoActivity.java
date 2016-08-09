package com.mobiusbobs.videoprocessing.sample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * VideoProcessing
 *
 * CopyVideoActivity copy video from source to target
 * along with following process:
 *
 * extract -> decode -> encode -> mux
 *
 * Created by rayshih on 8/9/16.
 * Copyright (c) 2015 MobiusBobs Inc. All rights reserved.
 */

public class CopyVideoActivity extends AppCompatActivity {
  public static final String TAG = "CopyVideoActivity";

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
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_OK) {
      if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
        Uri videoUri = data.getData();

        String sourcePath = Util.getRealPathFromURI(this, videoUri);
        String targetPath = sourcePath.replace("\\.(\\w*)$", "_processed.$1");
        Util.toastLong(this,
          "Copy video from " + sourcePath + " to " + targetPath);

        copyVideo(sourcePath, targetPath);
      }
    }
  }

  private void copyVideo(String sourcePath, String targetPath) {
  }
}
