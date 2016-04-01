package com.example.wangalbert.extractormuxer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

/*
 * Sample:
 * https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/ExtractDecodeEditEncodeMuxTest.java
 *
 */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = "TEST";

  // File Path
  public static final String FILE_INPUT_MP4 = "/sdcard/Download/tmp2.mp4";
  public static final String FILE_INPUT_AVC = "/sdcard/Download/TestAVC.mp4";
  public static final String FILE_INPUT_AAC = "/sdcard/Download/TestAAC.aac";
  public static final String FILE_INPUT_RAW = "/sdcard/Download/TestRAW.mp4";
  public static final String FILE_INPUT_WAV = "/sdcard/Download/TestWAV.wav";
  public static final String FILE_OUTPUT_RAW = "/sdcard/Download/TestRAW.mp4";
  public static final String FILE_OUTPUT_AAC = "/sdcard/Download/TestAAC.aac";
  public static final String FILE_OUTPUT_AVC = "/sdcard/Download/tmp2AVC.mp4";
  public static final String FILE_OUTPUT_MP4 = "/sdcard/Download/TestCodec7.mp4";
  public static final String FILE_OUTPUT_WATERMARK = "/sdcard/Download/Watermark7.mp4";
  public static final String FILE_OUTPUT_WAV = "/sdcard/Download/TestWAV.wav";
  public static final String FILE_OUTPUT_PCM = "/sdcard/Download/audioRaw.pcm";

  // Storage Permissions
  private static final int REQUEST_EXTERNAL_STORAGE = 1;
  private static String[] PERMISSIONS_STORAGE = {
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.RECORD_AUDIO
  };

  // MainComponent

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    verifyStoragePermissions(this);

    initView();
  }

  private void initView() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    if (fab != null) {
      fab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {

          runExtractDecodeEditEncodeMux();

          Snackbar.make(view, "Do Extract decode edit encode Mux action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
        }
      });
    }
  }

  private int resultCounter = 0;
  private void runExtractDecodeEditEncodeMux() {
    final Timer timer = new Timer();
    final Timer timerW = new Timer();
    timer.startTimer();
    timerW.startTimer();

    resultCounter = 0;

    try {
      /*
      CodecManager codec = new CodecManager(this, false, Util.getScreenDimen(this));
      codec.setOnMuxerDone(new CodecManager.OnMuxerDone() {
        @Override
        public void onDone() {
          timer.endTimer("mux(no watermark) is done");
          resultCounter++;
          if(resultCounter==2) Log.d(TAG, "result done! call callback!!!");
        }
      });
      CodecManager.ExtractDecodeEditEncodeMuxWrapper.run(codec, FILE_OUTPUT_MP4, R.raw.test_21);
      */

      CodecManager codecWatermark = new CodecManager(this, true, Util.getScreenDimen(this));
      codecWatermark.setOnMuxerDone(new CodecManager.OnMuxerDone() {
        @Override
        public void onDone() {
          timerW.endTimer("mux(with watermark) is done");
          resultCounter++;
          if(resultCounter==2) Log.d(TAG, "result done! call callback!!!");
        }
      });
      CodecManager.ExtractDecodeEditEncodeMuxWrapper.run(codecWatermark, FILE_OUTPUT_WATERMARK, R.raw.test_21);
    } catch (Throwable throwable) {
      throwable.printStackTrace();
    }
  }

  /**
   * Checks if the app has permission to write to device storage
   *
   * If the app does not has permission then the user will be prompted to grant permissions
   *
   * @param activity
   */
  public static void verifyStoragePermissions(Activity activity) {
    // Check if we have write permission
    int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    if (permission != PackageManager.PERMISSION_GRANTED) {
      // We don't have permission so prompt the user
      ActivityCompat.requestPermissions(
        activity,
        PERMISSIONS_STORAGE,
        REQUEST_EXTERNAL_STORAGE
      );
    }
  }
}
