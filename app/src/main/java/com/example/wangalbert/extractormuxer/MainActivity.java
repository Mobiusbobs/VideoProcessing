package com.example.wangalbert.extractormuxer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

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
  public static final String FILE_OUTPUT_MP4 = "/sdcard/Download/Test.mp4";
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
  Extractor extractor;
  Muxer muxer;
  AudioRecorder audioRecorder;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    verifyStoragePermissions(this);

    initView();

    initComponent();
    testComponent();

  }

  private void initView() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    if (fab != null) {
      fab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {

          // --- AudioRecord ---
          if (audioRecorder.isRecording())
            audioRecorder.stopRecroding();
          else
            audioRecorder.startRecroding();

          Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
        }
      });
    }
  }

  private void initComponent() {
    // init main component
    extractor = new Extractor();
    muxer = new Muxer();
    audioRecorder = new AudioRecorder(FILE_OUTPUT_PCM);
  }

  private void testComponent() {
    // ----- test all the component here -----

    // --- Extractor ---
    //extractor.extractVideoFile(FILE_INPUT_MP4);

    // --- Extractor -> Muxer ---
    try {
      Log.d(TAG, "-------------- test Extractor / Muxer -----------------");
      muxer.cloneMediaUsingMuxer(FILE_INPUT_MP4, FILE_OUTPUT_MP4, 180);
    } catch(IOException e) {
      e.printStackTrace();
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
