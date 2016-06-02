precision mediump float;    // Set the default precision to medium. We don't need as high of a

uniform sampler2D u_Texture;
uniform float u_Opacity;

varying vec2 v_TexCoordinate;

void main() {
    //gl_FragColor = vec4(1.0,0.0,0.0,1.0);                 // FOR DEBUG PURPOSE
    gl_FragColor = texture2D(u_Texture, v_TexCoordinate);   // Pass the color directly through the pipeline.
    gl_FragColor *= u_Opacity;
}