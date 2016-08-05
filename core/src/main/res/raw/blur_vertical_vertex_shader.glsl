uniform mat4 u_MVPMatrix;
attribute vec4 a_Position;
attribute vec2 a_TexCoordinate;

varying vec2 v_TexCoordinate;

uniform float u_Blur;

varying vec2 v_BlurTexCoords0;
varying vec2 v_BlurTexCoords1;
varying vec2 v_BlurTexCoords2;
varying vec2 v_BlurTexCoords3;
varying vec2 v_BlurTexCoords4;
varying vec2 v_BlurTexCoords5;
varying vec2 v_BlurTexCoords6;
varying vec2 v_BlurTexCoords7;
varying vec2 v_BlurTexCoords8;
varying vec2 v_BlurTexCoords9;
varying vec2 v_BlurTexCoords10;
varying vec2 v_BlurTexCoords11;
varying vec2 v_BlurTexCoords12;
varying vec2 v_BlurTexCoords13;

void main()
{
    gl_Position = u_MVPMatrix * a_Position;
    v_TexCoordinate = a_TexCoordinate;
    v_BlurTexCoords0  = v_TexCoordinate + vec2(0.0, -0.028*u_Blur);
    v_BlurTexCoords1  = v_TexCoordinate + vec2(0.0, -0.024*u_Blur);
    v_BlurTexCoords2  = v_TexCoordinate + vec2(0.0, -0.020*u_Blur);
    v_BlurTexCoords3  = v_TexCoordinate + vec2(0.0, -0.016*u_Blur);
    v_BlurTexCoords4  = v_TexCoordinate + vec2(0.0, -0.012*u_Blur);
    v_BlurTexCoords5  = v_TexCoordinate + vec2(0.0, -0.008*u_Blur);
    v_BlurTexCoords6  = v_TexCoordinate + vec2(0.0, -0.004*u_Blur);
    v_BlurTexCoords7  = v_TexCoordinate + vec2(0.0,  0.004*u_Blur);
    v_BlurTexCoords8  = v_TexCoordinate + vec2(0.0,  0.008*u_Blur);
    v_BlurTexCoords9  = v_TexCoordinate + vec2(0.0,  0.012*u_Blur);
    v_BlurTexCoords10 = v_TexCoordinate + vec2(0.0,  0.016*u_Blur);
    v_BlurTexCoords11 = v_TexCoordinate + vec2(0.0,  0.020*u_Blur);
    v_BlurTexCoords12 = v_TexCoordinate + vec2(0.0,  0.024*u_Blur);
    v_BlurTexCoords13 = v_TexCoordinate + vec2(0.0,  0.028*u_Blur);
}
