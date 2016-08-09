precision mediump float;

uniform sampler2D u_Texture;

varying vec2 v_TexCoordinate;

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
    gl_FragColor = vec4(0.0);
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords0)*0.0044299121055113265;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords1)*0.00895781211794;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords2)*0.0215963866053;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords3)*0.0443683338718;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords4)*0.0776744219933;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords5)*0.115876621105;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords6)*0.147308056121;
    gl_FragColor += texture2D(u_Texture, v_TexCoordinate    )*0.159576912161;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords7)*0.147308056121;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords8)*0.115876621105;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords9)*0.0776744219933;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords10)*0.0443683338718;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords11)*0.0215963866053;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords12)*0.00895781211794;
    gl_FragColor += texture2D(u_Texture, v_BlurTexCoords13)*0.0044299121055113265;
}
