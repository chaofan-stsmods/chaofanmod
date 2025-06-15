//SpriteBatch will use texture unit 0
uniform sampler2D u_texture;
uniform sampler2D u_ditheringPattern;
uniform vec2 u_screenSize;

//"in" varyings from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoord;

void main() {
    //sample the texture
    vec4 texColor = texture2D(u_texture, v_texCoord);
    vec4 ditheringColor = texture2D(u_ditheringPattern, v_texCoord * u_screenSize / 8.0);

    float v = 0.2 * texColor.r + 0.7 * texColor.g + 0.1 * texColor.b;

/*
    float threshold0 = 0.8;
    float threshold1 = 0.55;
    float threshold2 = 0.35;
    float threshold3 = 0.10;
    if (v > threshold0) {
        gl_FragColor = vec4(0.608, 0.737, 0.059, 1.0);
    } else if (v > threshold1) {
        v = (v - threshold1) / (1.0 - threshold1);
        gl_FragColor = v > ditheringColor.r ? vec4(0.608, 0.737, 0.059, 1.0) : vec4(0.545, 0.6745, 0.059, 1.0);
    } else if (v > threshold2) {
        v = (v - threshold2) / (threshold1 - threshold2);
        gl_FragColor = v > ditheringColor.r ? vec4(0.545, 0.6745, 0.059, 1.0) : vec4(0.188, 0.384, 0.188, 1.0);
    } else if (v > threshold3) {
        v = (v - threshold3) / (threshold2 - threshold3);
        gl_FragColor = v > ditheringColor.r ? vec4(0.188, 0.384, 0.188, 1.0) : vec4(0.059, 0.2196, 0.059, 1.0);
    } else {
        gl_FragColor = vec4(0.059, 0.2196, 0.059, 1.0);
    }
    */

    gl_FragColor = v > ditheringColor.r ? vec4(1.0, 1.0, 1.0, 1.0) : vec4(0.0, 0.0, 0.0, 1.0);
}
