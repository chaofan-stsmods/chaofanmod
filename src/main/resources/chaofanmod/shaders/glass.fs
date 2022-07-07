//SpriteBatch will use texture unit 0
uniform sampler2D u_texture;
uniform sampler2D u_background;

uniform vec2 u_screenSize;

//"in" varyings from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoord;
varying vec4 v_position;

void main() {
    vec4 glassColor = texture2D(u_texture, v_texCoord);
    vec4 texColor = texture2D(u_background, (v_position.xy / v_position.w + 1.0) / 2.0 + ((glassColor.rg - 0.5) * 100.0) / u_screenSize);

    gl_FragColor = vec4(texColor.rgb, glassColor.a);
}
