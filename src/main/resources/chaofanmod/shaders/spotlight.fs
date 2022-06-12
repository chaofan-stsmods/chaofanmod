uniform sampler2D u_texture;
uniform vec2 u_mouse;

varying vec4 v_color;
varying vec2 v_texCoord;

float rand(vec2 co){
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoord);
    float dmin = 200.0 * 200.0;
    float dmax = 240.0 * 240.0;
    vec2 screenCoord = gl_FragCoord.xy;
    vec2 dCoord = screenCoord - u_mouse;
    float d = dot(dCoord, dCoord);
    if (d < dmin) {
        gl_FragColor = vec4(texColor.rgb * 1.2f, 1.0);
    } else if (d < dmax) {
        gl_FragColor = vec4((texColor * (((dmax - d) * 1.2f + (d - dmin) * 0.1f) / (dmax - dmin))).rgb, 1.0);
    } else {
        vec2 texDiff = vec2(rand(screenCoord), rand(screenCoord + vec2(2000.0, 2000.0)));
        texColor = texture2D(u_texture, v_texCoord + texDiff / vec2(1920.0, 1080.0) * 3);
        gl_FragColor = vec4(texColor.rgb * 0.1f, 1.0);
    }
}
