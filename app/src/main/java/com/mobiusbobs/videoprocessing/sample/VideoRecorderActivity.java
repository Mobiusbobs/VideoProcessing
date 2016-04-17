package com.mobiusbobs.videoprocessing.sample;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initVideoRecorder(new File(getOutputFilePath()));

        // set views
        setContentView(R.layout.activity_video_recorder);
        cameraView = (CameraView)findViewById(R.id.gl_view);
        cameraView.init(cameraController, this); // must init right after onCreate
        setupButtons();
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
                cameraController.switchCamera();
            }
        });
    }

    private void setupPublishBtn() {
        publishBtn = (Button)findViewById(R.id.publish_btn);
        publishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoRecorder.stopRecord();
                isRecording = false;
                isNewRecordSession = true;
                updateControls();
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
        videoRecorder.resumeRecord(cameraView);
    }

    private void updateControls() {
        if (isRecording) {
            recordBtn.setText("Stop");
        } else {
            recordBtn.setText("Start");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        if (isRecording) {
            videoRecorder.updateSharedContext(EGL14.eglGetCurrentContext());
        }
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
}
