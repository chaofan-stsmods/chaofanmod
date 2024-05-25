//SpriteBatch will use texture unit 0
uniform sampler2D u_diffuseTexture;
uniform sampler2D u_highlightTexture;
uniform vec2 u_highLightOffset;

//"in" varyings from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoord;

float rand(vec2 co){
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

#define CAP_RATE 0.3
#define PI 3.1415926
#define MOIRE_RATE 0.2
#define MOIRE_FACTOR (MOIRE_RATE / (0.5 + CAP_RATE / 2.0))

float cos2(float rad) {
    while (rad > PI) {
        rad -= 2.0 * PI;
    }
    while (rad < -PI) {
        rad += 2.0 * PI;
    }

    if (abs(rad) <= PI * CAP_RATE) {
        return 1.0;
    }

    if (rad > 0.0) {
        return (cos((rad - PI * CAP_RATE) * (1.0 / (1.0 - CAP_RATE))) + 1.0) / 2.0;
    } else {
        return (cos((rad + PI * CAP_RATE) * (1.0 / (1.0 - CAP_RATE))) + 1.0) / 2.0;
    }
}

void main() {
    vec2 highlightCoord = (u_highLightOffset + 1.0) / 2.0;
    highlightCoord = vec2(highlightCoord.x, 1.0 - highlightCoord.y);

    //sample the texture
    vec4 texColor = texture2D(u_diffuseTexture, vec2(v_texCoord.x, 1.0 - v_texCoord.y));
    vec4 highlightColor = texture2D(u_highlightTexture, v_texCoord + highlightCoord * 0.1);

    float r = texColor.r;
    float g = texColor.g;
    float b = texColor.b;

    vec2 tex2 = v_texCoord * vec2(1920.0, 1080.0) + 3000.0;
    vec2 inPixel = tex2 - floor(tex2);
    r = MOIRE_FACTOR * r * cos2((inPixel.y - 0.5) * 2.0 * PI) + (1.0 - MOIRE_RATE) * r;
    g = MOIRE_FACTOR * g * cos2((inPixel.y - 0.5) * 2.0 * PI) + (1.0 - MOIRE_RATE) * g;
    b = MOIRE_FACTOR * b * cos2((inPixel.y - 0.5) * 2.0 * PI) + (1.0 - MOIRE_RATE) * b;

    r = MOIRE_FACTOR * r * cos2((inPixel.x - 0.16666) * 2.0 * PI) + (1.0 - MOIRE_RATE) * r;
    g = MOIRE_FACTOR * g * cos2((inPixel.x - 0.5) * 2.0 * PI) + (1.0 - MOIRE_RATE) * g;
    b = MOIRE_FACTOR * b * cos2((inPixel.x - 0.83333) * 2.0 * PI) + (1.0 - MOIRE_RATE) * b;

    r = r * 0.85 + 0.16;
    g = g * 0.85 + 0.16;
    b = b * 0.85 + 0.16;

    r = r * (0.9 + rand(vec2(gl_FragCoord.x, gl_FragCoord.y)) * 0.2);
    g = g * (0.9 + rand(vec2(gl_FragCoord.x, gl_FragCoord.y + 2000.0)) * 0.2);
    b = b * (0.9 + rand(vec2(gl_FragCoord.x, gl_FragCoord.y + 4000.0)) * 0.2);
    if (r > 0.6) {
        r *= 1.2;
    }

    //final color
    gl_FragColor = vec4(r, g, b, texColor.a) + vec4(highlightColor.rgb * highlightColor.a, 0.0);
}
