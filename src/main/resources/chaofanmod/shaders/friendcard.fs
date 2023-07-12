//SpriteBatch will use texture unit 0
uniform sampler2D u_texture;
uniform sampler2D u_mask;

//"in" varyings from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoord;

void main() {
    //sample the texture
    vec4 texColor = texture2D(u_texture, v_texCoord);
    vec4 maskColor = texture2D(u_mask, (v_texCoord - vec2(0.0, 0.12)) / vec2(1.0, 0.76) );

    float alpha = maskColor.r * v_color.a;

    //final color
    if (v_texCoord.y < 0.12 || v_texCoord.y > 0.88) {
        gl_FragColor = vec4(texColor.rgb, 0.0);
    } else {
        gl_FragColor = vec4(texColor.rgb * v_color.rgb, alpha * texColor.a);
    }
}
