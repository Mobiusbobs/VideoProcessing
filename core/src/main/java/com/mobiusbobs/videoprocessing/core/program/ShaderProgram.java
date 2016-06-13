package com.mobiusbobs.videoprocessing.core.program;

import android.content.Context;


import com.mobiusbobs.videoprocessing.core.util.ShaderHelper;
import com.mobiusbobs.videoprocessing.core.util.TextResourceReader;

import static android.opengl.GLES20.glUseProgram;

/**
 * android
 * <p>
 * Created by wangalbert on 6/6/16.
 * Copyright (c) 2016 MobiusBobs Inc. All rights reserved.
 */
public class ShaderProgram {
    private final static String TAG = "ShaderProgram";

    // Shader program
    protected final int programHandle;

    // Constructor
    protected ShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {

        String vertexShader = TextResourceReader.readTextFileFromResource(context, vertexShaderResourceId);
        String fragmentShader = TextResourceReader.readTextFileFromResource(context, fragmentShaderResourceId);

        // Compile the shaders and link the program.
        programHandle = ShaderHelper.buildProgram(vertexShader, fragmentShader);
    }

    public void useProgram() {
        // Set the current OpenGL shader program to this program.
        glUseProgram(programHandle);
    }
}
