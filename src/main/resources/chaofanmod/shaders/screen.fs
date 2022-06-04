//SpriteBatch will use texture unit 0
uniform sampler2D u_texture;

//"in" varyings from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoord;

float rand(vec2 co){
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    //sample the texture
    vec4 texColor = texture2D(u_texture, v_texCoord);

    float r = texColor.r * (0.8 + rand(vec2(gl_FragCoord.x, gl_FragCoord.y)) * 0.4);
    float g = texColor.g * 1.12 * (0.9 + rand(vec2(gl_FragCoord.x, gl_FragCoord.y + 2000)) * 0.2);
    float b = texColor.b * 0.95 * (0.9 + rand(vec2(gl_FragCoord.x, gl_FragCoord.y + 4000)) * 0.2);
    if (r > 0.6) {
        r *= 1.2;
    }

    r = r * 0.85 + 0.16;
    g = g * 0.85 + 0.16;
    b = b * 0.85 + 0.16;

    //final color
    gl_FragColor = vec4(r, g, b, texColor.a);
}
