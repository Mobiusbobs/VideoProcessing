package com.mobiusbobs.videoprocessing.sample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.mobiusbobs.videoprocessing.core.ProcessorRunner;
import com.mobiusbobs.videoprocessing.core.VideoProcessor;
import com.mobiusbobs.videoprocessing.core.gldrawer.BasicDrawer;
import com.mobiusbobs.videoprocessing.core.gldrawer.GLDrawable;
import com.mobiusbobs.videoprocessing.core.util.BitmapHelper;
import com.mobiusbobs.videoprocessing.core.util.CoordConverter;
import com.mobiusbobs.videoprocessing.core.util.Size;

import java.io.IOException;

/**
 * VideoProcessing
 *
 * Resize video from source to target
 * along with following process:
 *
 * extract -> decode -> render into given size -> encode -> mux
 *
 * Created by rayshih on 9/20/16.
 * Copyright (c) 2015 MobiusBobs Inc. All rights reserved.
 */

public class ResizeVideoActivity extends AppCompatActivity {
  public static final String TAG = "ResizeVideoActivity";

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
          "Resize video from " + sourcePath + " to " + targetPath);

        resizeVideo(sourcePath, targetPath);
      }
    }
  }

  private void resizeVideo(String sourcePath, String targetPath) {
    VideoProcessor videoProcessor;
    try {
      Size size = new Size(720, 720);

      GLDrawable sticker = new StickerDrawer(
        this, size, CoordConverter.getVerticesCoord(10, 30, 160, 180));

      videoProcessor = new VideoProcessor.Builder(size)
        .setInputFilePath(sourcePath)
        .addDrawer(sticker)
        .setOutputFilePath(targetPath)
        .build(this);
    } catch (IOException e) {
      Util.toastLong(this, "Failed while building video processor");
      e.printStackTrace();

      return;
    }

    ProcessorRunner.run(
      videoProcessor, "Resize Video",
      new ProcessorRunner.ProcessorRunnerCallback() {
        @Override
        public void onCompleted() {
          Util.toastLong(ResizeVideoActivity.this,
            "Process complete");
        }

        @Override
        public void onError(Throwable e) {
          Util.toastLong(ResizeVideoActivity.this,
            "Process failed");

          Log.e(TAG, e.getMessage());
          e.printStackTrace();
        }
      }
    );
  }

  // TODO refactor this
  class StickerDrawer implements GLDrawable {
    protected BasicDrawer drawer;

    public StickerDrawer(Context context, Size outputVideoSize, float[] verticesPositionData) {
      drawer = new BasicDrawer(context, outputVideoSize, verticesPositionData);
    }

    @Override
    public void setRotate(float rotateInDeg) {

    }

    @Override
    public void init(GLDrawable prevDrawer) throws IOException {
      Bitmap bitmap = BitmapHelper.generateBitmap(ResizeVideoActivity.this, R.drawable.game);
      drawer.init(prevDrawer, bitmap);
      bitmap.recycle();
    }

    @Override
    public void draw(long timeMs) {
      drawer.draw(timeMs);
    }
  }
}
