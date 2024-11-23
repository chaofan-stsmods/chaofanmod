//SpriteBatch will use texture unit 0
uniform sampler2D u_texture;
uniform vec2 resolution;

//"in" varyings from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoord;

void main()
{
    float pi = 6.28318530718; // pi*2

    // GAUSSIAN BLUR SETTINGS {{{
    float directions = 16.0; // BLUR DIRECTIONS (Default 16.0 - More is better but slower)
    float quality = 3.0; // BLUR QUALITY (Default 4.0 - More is better but slower)
    float size = 6.0 * resolution.x / 1600.0; // BLUR SIZE (radius)
    // GAUSSIAN BLUR SETTINGS }}}

    vec2 radius = size/resolution;

    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = v_texCoord;
    // Pixel colour
    vec4 color = texture(u_texture, uv);
    float luminanceFloat = color.r * 0.3 + color.g * 0.6 + color.b * 0.1;
    vec4 luminance = vec4(luminanceFloat, luminanceFloat, luminanceFloat, luminanceFloat);
    vec4 sumColor = color * luminance * luminance;
    float count = 1.0;

    // Blur calculations
    for( float d=0.0; d<pi; d+=pi/directions)
    {
		for(float i=1.0/quality; i<=1.0; i+=1.0/quality)
        {
			color = texture( u_texture, uv+vec2(cos(d),sin(d))*radius*i);
            luminanceFloat = color.r * 0.2 + color.g * 0.7 + color.b * 0.1;
            luminance = vec4(luminanceFloat, luminanceFloat, luminanceFloat, luminanceFloat);
			sumColor += color * luminance * luminance;
			count += 1.0;
        }
    }

    // Output to screen
    sumColor /= count;
    gl_FragColor = v_color * sumColor;
}