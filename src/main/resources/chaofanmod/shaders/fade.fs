//SpriteBatch will use texture unit 0
uniform sampler2D u_texture;
uniform sampler2D u_noise;
uniform vec2 u_screenSize;
uniform float u_progress;

//"in" varyings from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoord;

void main() {
    //sample the texture
    vec4 texColor = texture2D(u_texture, v_texCoord);
    vec4 noiseColor = texture2D(u_noise, v_texCoord * u_screenSize / 512.0);

    float noiseFactor = 0.3;
    float volume = (texColor.r * 0.299 + texColor.g * 0.587 + texColor.b * 0.114 + (noiseColor.r - 0.5) * noiseFactor * 2.0);
    volume = (volume + noiseFactor) / (1.0 + noiseFactor * 2.0);

    float edgeWidth = 1.0;
    float progress2 = u_progress * (1.0 + edgeWidth);
    vec2 pCenter = (v_texCoord - vec2(0.5, 0.5)) * 1.4143;
    float d = sqrt(dot(pCenter, pCenter));
    float threshold;
    if (d > progress2) {
        threshold = 0.0;
    } else if (d < progress2 - edgeWidth) {
        threshold = 1.0;
    } else {
        threshold = (progress2 - d) / edgeWidth;
    }

/*
    threshold = u_progress
*/

    volume = 1.0 - volume;
    if (volume < threshold - 0.01) {
        gl_FragColor = texColor;
    } else if (volume > threshold + 0.01) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
    } else {
        float f = (threshold + 0.01 - volume) / 0.02;
        gl_FragColor = mix(vec4(0.0, 0.0, 0.0, 1.0), texColor, f);
    }
}
