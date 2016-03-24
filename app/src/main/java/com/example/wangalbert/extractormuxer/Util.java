package com.example.wangalbert.extractormuxer;

import android.util.Log;

import java.io.File;


public class Util {
    private static final String TAG = "Util";

    private static long startTime = 0;

    public static void fileSize(String filePath) {
        File file = new File(filePath);
        long fileSize = file.length();
        Log.d(TAG, "fileSize=" + fileSize + ", " + fileSize / 1024 + "KB, " + fileSize / 1024 / 1024 + "MB");
    }

    public static void startTimer() {
        startTime = System.currentTimeMillis();
        Log.d(TAG, "TEST: start timer!");
    }

    public static void endTimer(String msg) {
        long endTime = System.currentTimeMillis();
        Log.d(TAG, "TEST: " + msg + ": time elapse = " + (endTime - startTime));
    }

}
