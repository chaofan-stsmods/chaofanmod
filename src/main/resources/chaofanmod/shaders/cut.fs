//SpriteBatch will use texture unit 0
uniform sampler2D u_texture;

uniform float u_scale;
uniform vec2 u_screenSize;

uniform vec2 u_point;
uniform float u_direction;
uniform float u_progress;

//"in" varyings from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoord;

#define THRESHOLD 0.5
#define CUT_TIME 0.05
#define CUT_THRESHOLD (2.5 * u_scale)
#define CUT_THRESHOLD_SMALL (1.5 * u_scale)

#define MOVE_X_BASE 30.0
#define MOVE_X_VAR 18.0
#define MOVE_X_RAND(n) (MOVE_X_BASE + MOVE_X_VAR * (rand(n) - 0.5))
#define MOVE_Y_VAR 40.0
#define MOVE_Y_RAND(n) (MOVE_Y_VAR * (rand(n) - 0.5))

#define SCALE_I_RAND(n) (0.1 * (rand(n) - 0.6))
#define SCALE_D_RAND(n) (0.06 * (rand(n) - 0.5))

float rand(float v) {
    vec2 co = u_point + u_direction + v;
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 screen = v_texCoord * u_screenSize;
    vec2 cutDirection = vec2(cos(u_direction), sin(u_direction));
    vec2 perpendicularDirection = vec2(-sin(u_direction), cos(u_direction));
    mat2 cutMat = mat2(cutDirection, perpendicularDirection);
    vec2 cut = (screen - u_point) * cutMat;

    vec2 move1 = vec2(MOVE_X_RAND(0.0), MOVE_Y_RAND(1.0));
    mat2 scale1 = mat2(SCALE_I_RAND(2.0), SCALE_D_RAND(3.0), SCALE_D_RAND(4.0), SCALE_I_RAND(5.0));
    vec2 move2 = vec2(-MOVE_X_RAND(6.0), MOVE_Y_RAND(7.0));
    mat2 scale2 = mat2(SCALE_I_RAND(8.0), SCALE_D_RAND(9.0), SCALE_D_RAND(10.0), SCALE_I_RAND(11.0));

    float progress = u_progress < 0.5 ? 2.0 * u_progress * u_progress : 1.0 - 2.0 * (u_progress - 1) * (u_progress - 1);

    vec4 bgColor;
    if (abs(cut.y) < THRESHOLD) {
        vec2 screen1 = cutMat * (cut + (move1 + cut * scale1) * u_scale * progress) + u_point;
        vec2 screen2 = cutMat * (cut + (move2 + cut * scale2) * u_scale * progress) + u_point;

        vec4 texColor1 = texture2D(u_texture, screen1 / u_screenSize);
        vec4 texColor2 = texture2D(u_texture, screen2 / u_screenSize);
        float d = (cut.y + THRESHOLD) / (2.0 * THRESHOLD);
        bgColor = texColor1 * (1.0 - d) + texColor2 * d;
    } else {
        if (cut.y < 0.0) {
            screen = cutMat * (cut + (move1 + cut * scale1) * u_scale * progress) + u_point;
        } else {
            screen = cutMat * (cut + (move2 + cut * scale2) * u_scale * progress) + u_point;
        }
        bgColor = texture2D(u_texture, screen / u_screenSize);
    }

    if (u_progress < CUT_TIME && abs(cut.y) < CUT_THRESHOLD) {
        vec4 white = vec4(1.0, 1.0, 1.0, 1.0);
        float time = u_progress / CUT_TIME;
        float timeFactor = 1.0 - time * time;
        float spaceFactor = abs(cut.y) < CUT_THRESHOLD_SMALL ? 1.0 : (CUT_THRESHOLD - abs(cut.y)) / (CUT_THRESHOLD - CUT_THRESHOLD_SMALL);
        float d = timeFactor * spaceFactor;
        gl_FragColor = bgColor * (1.0 - d) + white * d;
    } else {
        gl_FragColor = bgColor;
    }
}
