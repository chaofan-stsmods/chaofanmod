//SpriteBatch will use texture unit 0
uniform sampler2D u_texture;

uniform float u_scale;
uniform vec2 u_screenSize;

#define BATCH_SIZE 10

uniform vec2 u_point[BATCH_SIZE];
uniform float u_direction[BATCH_SIZE];
uniform float u_progress[BATCH_SIZE];

//"in" varyings from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoord;

#define THRESHOLD 0.5
#define CUT_TIME 0.05
#define CUT_THRESHOLD (2.5 * u_scale)
#define CUT_THRESHOLD_SMALL (1.5 * u_scale)

#define MOVE_X_BASE 30.0
#define MOVE_X_VAR 18.0
#define MOVE_X_RAND(n) (MOVE_X_BASE + MOVE_X_VAR * (rand(n, i) - 0.5))
#define MOVE_Y_VAR 40.0
#define MOVE_Y_RAND(n) (MOVE_Y_VAR * (rand(n, i) - 0.5))

#define SCALE_I_RAND(n) (0.1 * (rand(n, i) - 0.6))
#define SCALE_D_RAND(n) (0.06 * (rand(n, i) - 0.5))

#define TWP_SIZE 20

struct TexCoordWithPower {
    vec2 texCoord;
    float power;
} twp[TWP_SIZE];
int twpSize = 0;

float rand(float v, int i) {
    vec2 co = u_point[i] + u_direction[i] + v;
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    twpSize = 1;
    twp[0].texCoord = v_texCoord;
    twp[0].power = 1.0;

    for (int i = BATCH_SIZE - 1; i >= 0; i--) {
        if (u_progress[i] < 0) {
            continue;
        }

        int currentSize = twpSize;
        for (int j = 0; j < currentSize; j++) {
            if (twp[j].power == 0.0) {
                continue;
            }

            vec2 screen = twp[j].texCoord * u_screenSize;
            vec2 cutDirection = vec2(cos(u_direction[i]), sin(u_direction[i]));
            vec2 perpendicularDirection = vec2(-sin(u_direction[i]), cos(u_direction[i]));
            mat2 cutMat = mat2(cutDirection, perpendicularDirection);
            vec2 cut = (screen - u_point[i]) * cutMat;

            vec2 move1 = vec2(MOVE_X_RAND(0.0), MOVE_Y_RAND(1.0));
            mat2 scale1 = mat2(SCALE_I_RAND(2.0), SCALE_D_RAND(3.0), SCALE_D_RAND(4.0), SCALE_I_RAND(5.0));
            vec2 move2 = vec2(-MOVE_X_RAND(6.0), MOVE_Y_RAND(7.0));
            mat2 scale2 = mat2(SCALE_I_RAND(8.0), SCALE_D_RAND(9.0), SCALE_D_RAND(10.0), SCALE_I_RAND(11.0));

            float progress = u_progress[i] < 0.5 ? 2.0 * u_progress[i] * u_progress[i] : 1.0 - 2.0 * (u_progress[i] - 1.0) * (u_progress[i] - 1.0);

            if (abs(cut.y) < THRESHOLD && twpSize < TWP_SIZE) {
                vec2 screen1 = cutMat * (cut + (move1 + cut * scale1) * u_scale * progress) + u_point[i];
                vec2 screen2 = cutMat * (cut + (move2 + cut * scale2) * u_scale * progress) + u_point[i];

                float d = (cut.y + THRESHOLD) / (2.0 * THRESHOLD);
                twp[twpSize].texCoord = screen2 / u_screenSize;
                twp[twpSize].power = d * twp[j].power;
                twpSize++;
                twp[j].texCoord = screen1 / u_screenSize;
                twp[j].power = (1.0 - d) * twp[j].power;
            } else {
                if (cut.y < 0.0) {
                    screen = cutMat * (cut + (move1 + cut * scale1) * u_scale * progress) + u_point[i];
                } else {
                    screen = cutMat * (cut + (move2 + cut * scale2) * u_scale * progress) + u_point[i];
                }
                twp[j].texCoord = screen / u_screenSize;
            }
        }
    }
    
    vec4 bgColor = vec4(0.0, 0.0, 0.0, 0.0);
    for (int i = 0; i < twpSize; i++) {
        bgColor += texture2D(u_texture, twp[i].texCoord) * twp[i].power;
    }

    for (int i = 0; i < BATCH_SIZE; i++) {
        if (u_progress[i] < 0.0) {
            continue;
        }

        vec2 screen = v_texCoord * u_screenSize;
        vec2 cutDirection = vec2(cos(u_direction[i]), sin(u_direction[i]));
        vec2 perpendicularDirection = vec2(-sin(u_direction[i]), cos(u_direction[i]));
        mat2 cutMat = mat2(cutDirection, perpendicularDirection);
        vec2 cut = (screen - u_point[i]) * cutMat;

        if (u_progress[i] < CUT_TIME && abs(cut.y) < CUT_THRESHOLD) {
            vec4 white = vec4(1.0, 1.0, 1.0, 1.0);
            float time = u_progress[i] / CUT_TIME;
            float timeFactor = 1.0 - time * time;
            float spaceFactor = abs(cut.y) < CUT_THRESHOLD_SMALL ? 1.0 : (CUT_THRESHOLD - abs(cut.y)) / (CUT_THRESHOLD - CUT_THRESHOLD_SMALL);
            float d = timeFactor * spaceFactor;
            bgColor = bgColor * (1.0 - d) + white * d;
        }
    }

    gl_FragColor = bgColor;
}
