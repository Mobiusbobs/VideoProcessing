uniform mat4 u_MVPMatrix;
attribute vec4 a_Position;
attribute vec2 a_TexCoordinate;

varying vec2 v_TexCoordinate;

uniform float u_Blur;
varying vec2 v_BlurTexCoords[14];

void main()
{
    gl_Position = u_MVPMatrix * a_Position;
    v_TexCoordinate = a_TexCoordinate;
    v_BlurTexCoords[ 0] = v_TexCoordinate + vec2(0.0, -0.028*u_Blur);
    v_BlurTexCoords[ 1] = v_TexCoordinate + vec2(0.0, -0.024*u_Blur);
    v_BlurTexCoords[ 2] = v_TexCoordinate + vec2(0.0, -0.020*u_Blur);
    v_BlurTexCoords[ 3] = v_TexCoordinate + vec2(0.0, -0.016*u_Blur);
    v_BlurTexCoords[ 4] = v_TexCoordinate + vec2(0.0, -0.012*u_Blur);
    v_BlurTexCoords[ 5] = v_TexCoordinate + vec2(0.0, -0.008*u_Blur);
    v_BlurTexCoords[ 6] = v_TexCoordinate + vec2(0.0, -0.004*u_Blur);
    v_BlurTexCoords[ 7] = v_TexCoordinate + vec2(0.0,  0.004*u_Blur);
    v_BlurTexCoords[ 8] = v_TexCoordinate + vec2(0.0,  0.008*u_Blur);
    v_BlurTexCoords[ 9] = v_TexCoordinate + vec2(0.0,  0.012*u_Blur);
    v_BlurTexCoords[10] = v_TexCoordinate + vec2(0.0,  0.016*u_Blur);
    v_BlurTexCoords[11] = v_TexCoordinate + vec2(0.0,  0.020*u_Blur);
    v_BlurTexCoords[12] = v_TexCoordinate + vec2(0.0,  0.024*u_Blur);
    v_BlurTexCoords[13] = v_TexCoordinate + vec2(0.0,  0.028*u_Blur);
}
