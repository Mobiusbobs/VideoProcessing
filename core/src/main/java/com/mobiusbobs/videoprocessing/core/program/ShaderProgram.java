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

//    // Uniform constants
//    public static final String U_MATRIX = "u_Matrix";
//    public static final String U_TEXTURE_UNIT = "u_TextureUnit";
//    public static final String U_OPACITY = "u_Opacity";
//    public static final String U_BLUR = "u_Blur";
//
//    // Attribute constants
//    public static final String A_POSITION = "a_Position";
//    public static final String A_COLOR = "a_Color";
//    public static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";

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

//    public int getProgramHandle() {
//        return programHandle;
//    }

}
