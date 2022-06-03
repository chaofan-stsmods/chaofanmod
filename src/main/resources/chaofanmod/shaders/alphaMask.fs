//SpriteBatch will use texture unit 0
uniform sampler2D u_texture;
uniform sampler2D u_mask;

//"in" varyings from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoord;

void main() {
    float pi = 3.1415926;

    //sample the texture
    vec4 texColor = texture2D(u_texture, v_texCoord);
    vec4 maskColor = texture2D(u_mask, v_texCoord);

    float alpha = maskColor.r + v_color.a;
    if (alpha > 1) {
        alpha -= 1;
    }

    alpha = (cos(2.0 * pi * alpha) + 1) / 2.0;

    //final color
    gl_FragColor = vec4(texColor.rgb, alpha * texColor.a);
}
