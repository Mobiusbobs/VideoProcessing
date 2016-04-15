package com.mobiusbobs.videoprocessing.sample;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mobiusbobs.videoprocessing.core.CameraView;
import com.mobiusbobs.videoprocessing.core.codec.VideoRecorder;
import com.mobiusbobs.videoprocessing.core.hardware_control.CameraController;

import java.io.File;
import java.io.IOException;

/**
 * VideoRecorder
 * <p/>
 * Created by rayshih on 3/15/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
@SuppressWarnings("ALL")
public class VideoRecorderActivity extends AppCompatActivity implements CameraView.Callback {
    private static final String TAG = "VideoRecorderActivity";
    // TODO make output file configurable
    private static final String FILE_OUTPUT_NAME = "/gl_video_record_test1.mp4";

    // camera
    private CameraController cameraController = new CameraController();

    // ui
    private CameraView cameraView;
    private Button recordBtn;
    private Button switchBtn;
    private Button publishBtn;

    // recorder and its state
    private VideoRecorder videoRecorder;
    private Boolean isNewRecordSession = true;
    private Boolean isRecording = false;

    // output file
    private File outputFile;

    // check permission
    private boolean functionDisabled = true;

    // activity permissions
    private static boolean permissionGranted = false;
    private static final int REQUEST_PERMISSION_CODE = 1;
    private static String[] PERMISSIONS = {
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.RECORD_AUDIO,
      Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set permission
        permissionGranted = verifyPermissions(this);

        // set views
        setContentView(R.layout.activity_video_recorder);
        cameraView = (CameraView)findViewById(R.id.gl_view);
        cameraView.init(cameraController, this);
        setupButtons();

        initPermissionRequiredComponent();
    }

    private void initPermissionRequiredComponent() {
        if (permissionGranted) {
            // videorecorder
            outputFile = new File(getOutputFilePath());
            initVideoRecorder(outputFile);

            functionDisabled = false;
        }   else {
            Log.e(TAG, "permission not granted");

            functionDisabled = true;
        }
    }

    private void initVideoRecorder(File outputFile) {
        try {
            videoRecorder = new VideoRecorder(outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupButtons()  {
        setupRecordBtn();
        setupSwitchBtn();
        setupPublishBtn();
    }

    private void setupRecordBtn() {
        recordBtn = (Button)findViewById(R.id.record_btn);
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (functionDisabled) {
                    printFunctionDisable();
                    return;
                }

                isRecording = !isRecording;
                notifyRecorder();
                updateControls();
            }
        });
    }

    private void setupSwitchBtn() {
        switchBtn = (Button)findViewById(R.id.switch_btn);
        switchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (functionDisabled) {
                    printFunctionDisable();
                    return;
                }

                cameraController.switchCamera();
            }
        });
    }

    private void setupPublishBtn() {
        publishBtn = (Button)findViewById(R.id.publish_btn);
        publishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (functionDisabled) {
                    printFunctionDisable();
                    return;
                }

                videoRecorder.stopRecord();
                isRecording = false;
                isNewRecordSession = true;
            }
        });
    }

    private void notifyRecorder() {
        if (!isRecording) {
            videoRecorder.pauseRecord();
            return;
        }

        // isRecording
        if (isNewRecordSession) {
            isNewRecordSession = false;

            try {
                outputFile = new File(getOutputFilePath());
                videoRecorder.resetRecorder(outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            videoRecorder.startRecord(cameraView);
            return;
        }

        // isRecoding and not new session
        videoRecorder.resumeRecord();
    }

    private void updateControls() {
        if (isRecording) {
            recordBtn.setText("Stop");
        } else {
            recordBtn.setText("Start");
        }
    }

    private void printFunctionDisable() {
        String msg = "permission is not granted. Function is disabled";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (functionDisabled) return;
        cameraView.onResume();
    }

    @Override
    protected void onPause() {
        cameraView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        cameraView.onDestory();
        super.onDestroy();
    }

    // ----- camera view callbacks -----
    @Override
    public void onSurfaceCreated() {
        if (videoRecorder.isStartRecording())
            videoRecorder.updateSharedContext(EGL14.eglGetCurrentContext());
    }

    @Override
    public void onFrameAvailable(int textureId, SurfaceTexture surfaceTexture) {
        if (isRecording) {
            videoRecorder.onVideoFrameAvailable(textureId, surfaceTexture);
        }
    }

    // ----- utils -----
    private String getOutputFilePath() {
        File outpuFileDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        return outpuFileDir.getAbsolutePath() + FILE_OUTPUT_NAME;
    }


    // ----- permssion related for 6.0+ -----
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];
                Log.d(TAG, "permission = " + permission + ", grantResults = " + grantResult);
                // permission denied
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = false;
                    return;
                }
            }
            permissionGranted = true;
            initPermissionRequiredComponent();
        }
    }

    /**
     * Checks if the app has permission
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    private boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
              activity,
              PERMISSIONS,
              REQUEST_PERMISSION_CODE
            );
            return false;
        }
        return true;
    }
}
