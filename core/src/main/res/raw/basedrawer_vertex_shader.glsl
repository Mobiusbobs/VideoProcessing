uniform mat4 u_MVPMatrix;           // A constant representing the combined model/view/projection matrix.

attribute vec4 a_Position;          // Per-vertex position information we will pass in.
attribute vec2 a_TexCoordinate;

varying vec2 v_TexCoordinate;

void main() {
    v_TexCoordinate = a_TexCoordinate;
    gl_Position = u_MVPMatrix  * a_Position;
}