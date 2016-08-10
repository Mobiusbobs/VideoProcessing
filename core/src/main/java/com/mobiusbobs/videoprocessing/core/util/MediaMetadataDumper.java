package com.mobiusbobs.videoprocessing.core.util;

import java.lang.Exception;

import android.util.Log;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;

public class MediaMetadataDumper {

    public static final String MEDIA_FORMAT_TAG = "DUMP_MEDIA_FORMAT";
    public static final String MEDIA_METADATA_TAG = "DUMP_MEDIA_METADATA";

    public static void dumpMediaFormat(MediaFormat mediaFormat) {
        Log.d(MEDIA_FORMAT_TAG, "Start");
        printMediaFormat(mediaFormat, "KEY_AAC_PROFILE", MediaFormat.KEY_AAC_PROFILE, "Integer");
        printMediaFormat(mediaFormat, "KEY_BIT_RATE", MediaFormat.KEY_BIT_RATE, "Integer");
        printMediaFormat(mediaFormat, "KEY_CHANNEL_COUNT", MediaFormat.KEY_CHANNEL_COUNT, "Integer");
        printMediaFormat(mediaFormat, "KEY_CHANNEL_MASK", MediaFormat.KEY_CHANNEL_MASK, "Integer");
        printMediaFormat(mediaFormat, "KEY_COLOR_FORMAT", MediaFormat.KEY_COLOR_FORMAT, "Integer");
        printMediaFormat(mediaFormat, "KEY_DURATION", MediaFormat.KEY_DURATION, "Long");
        printMediaFormat(mediaFormat, "KEY_FLAC_COMPRESSION_LEVEL", MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, "Integer");
        printMediaFormat(mediaFormat, "KEY_FRAME_RATE", MediaFormat.KEY_FRAME_RATE, "Integer");
        printMediaFormat(mediaFormat, "KEY_HEIGHT", MediaFormat.KEY_HEIGHT, "Integer");
        printMediaFormat(mediaFormat, "KEY_IS_ADTS", MediaFormat.KEY_IS_ADTS, "Integer");
        printMediaFormat(mediaFormat, "KEY_I_FRAME_INTERVAL", MediaFormat.KEY_I_FRAME_INTERVAL, "Integer");
        printMediaFormat(mediaFormat, "KEY_MAX_INPUT_SIZE", MediaFormat.KEY_MAX_INPUT_SIZE, "Integer");
        printMediaFormat(mediaFormat, "KEY_MIME", MediaFormat.KEY_MIME, "String");
        printMediaFormat(mediaFormat, "KEY_SAMPLE_RATE", MediaFormat.KEY_SAMPLE_RATE, "Integer");
        printMediaFormat(mediaFormat, "KEY_WIDTH", MediaFormat.KEY_WIDTH, "Integer");
        printMediaFormat(mediaFormat, "KEY_ROTATION", MediaFormat.KEY_ROTATION, "Integer");
        Log.d(MEDIA_FORMAT_TAG, "End");
    }

    public static void printMediaFormat(MediaFormat mediaFormat, String tag, String key, String type) {
        if (mediaFormat.containsKey(key)) {
            if (type.equals("Integer")) {
                Log.d(MEDIA_FORMAT_TAG, tag + ": " + mediaFormat.getInteger(key));
            } else if (type.equals("Long")) {
                Log.d(MEDIA_FORMAT_TAG, tag + ": " + mediaFormat.getLong(key));
            } else if (type.equals("String")) {
                Log.d(MEDIA_FORMAT_TAG, tag + ": " + mediaFormat.getString(key));
            }
            return;
        }
        Log.d(MEDIA_FORMAT_TAG, tag + ": Not Found");
    }

    public static void dumpMediaMetadata(String filePath) {
        Log.d(MEDIA_METADATA_TAG, "Start");
        Log.d(MEDIA_METADATA_TAG, "FilePath: " + filePath);

        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(filePath);

            printMediaMetadata(
                mmr,
                "METADATA_KEY_ALBUM",
                MediaMetadataRetriever.METADATA_KEY_ALBUM
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_ALBUMARTIST",
                MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_ARTIST",
                MediaMetadataRetriever.METADATA_KEY_ARTIST
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_AUTHOR",
                MediaMetadataRetriever.METADATA_KEY_AUTHOR
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_BITRATE",
                MediaMetadataRetriever.METADATA_KEY_BITRATE
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_CAPTURE_FRAMERATE",
                MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_CD_TRACK_NUMBER",
                MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_COMPILATION",
                MediaMetadataRetriever.METADATA_KEY_COMPILATION
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_COMPOSER",
                MediaMetadataRetriever.METADATA_KEY_COMPOSER
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_DATE",
                MediaMetadataRetriever.METADATA_KEY_DATE
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_DISC_NUMBER",
                MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_DURATION",
                MediaMetadataRetriever.METADATA_KEY_DURATION
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_GENRE",
                MediaMetadataRetriever.METADATA_KEY_GENRE
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_HAS_AUDIO",
                MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_HAS_VIDEO",
                MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_LOCATION",
                MediaMetadataRetriever.METADATA_KEY_LOCATION
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_MIMETYPE",
                MediaMetadataRetriever.METADATA_KEY_MIMETYPE
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_NUM_TRACKS",
                MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_TITLE",
                MediaMetadataRetriever.METADATA_KEY_TITLE
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_VIDEO_HEIGHT",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_VIDEO_ROTATION",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_VIDEO_WIDTH",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_WRITER",
                MediaMetadataRetriever.METADATA_KEY_WRITER
            );

            printMediaMetadata(
                mmr,
                "METADATA_KEY_YEAR",
                MediaMetadataRetriever.METADATA_KEY_YEAR
            );

            Log.d(MEDIA_METADATA_TAG, "End");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void printMediaMetadata(MediaMetadataRetriever mmr, String tag, int key) {
        String value = mmr.extractMetadata(key);
        if (value != null) {
            Log.d(MEDIA_METADATA_TAG, tag + ": " + value);
        } else {
            Log.d(MEDIA_METADATA_TAG, tag + ": " + "not found");
        }
    }

}
