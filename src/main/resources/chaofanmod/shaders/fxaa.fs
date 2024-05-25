//SpriteBatch will use texture unit 0
uniform sampler2D u_texture;
uniform vec2 resolution;

//"in" varyings from our vertex shader
varying vec4 v_color;
varying vec2 v_texCoord;


#define FXAA_SPAN_MAX	8.0
#define FXAA_REDUCE_MUL 1.0/8.0
#define FXAA_REDUCE_MIN 1.0/128.0

vec3 Box(vec2 p)
{
    return texture(u_texture, p).rgb;
}

void main()
{
	vec2 uv = v_texCoord;

	vec2 add = vec2(1.0) / resolution.xy;

	vec3 rgbNW = Box(uv+vec2(-add.x, -add.y));
	vec3 rgbNE = Box(uv+vec2( add.x, -add.y));
	vec3 rgbSW = Box(uv+vec2(-add.x,  add.y));
	vec3 rgbSE = Box(uv+vec2( add.x,  add.y));
	vec3 rgbM  = Box(uv);

	vec3 luma	 = vec3(0.299, 0.587, 0.114);
	float lumaNW = dot(rgbNW, luma);
	float lumaNE = dot(rgbNE, luma);
	float lumaSW = dot(rgbSW, luma);
	float lumaSE = dot(rgbSE, luma);
	float lumaM  = dot(rgbM,  luma);

	float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
	float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));

	vec2 dir;
	dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
	dir.y =  ((lumaNW + lumaSW) - (lumaNE + lumaSE));

	float dirReduce = max(
		(lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * FXAA_REDUCE_MUL), FXAA_REDUCE_MIN);

	float rcpDirMin = 1.0/(min(abs(dir.x), abs(dir.y)) + dirReduce);

	dir = min(vec2( FXAA_SPAN_MAX,  FXAA_SPAN_MAX),
		  max(vec2(-FXAA_SPAN_MAX, -FXAA_SPAN_MAX),
		  dir * rcpDirMin)) * add;

	vec3 rgbA = (1.0/2.0) * (Box(uv + dir * (1.0/3.0 - 0.5)) + Box(uv + dir * (2.0/2.0 - 0.5)));

	vec3 rgbB = rgbA * (1.0/2.0) + (1.0/4.0) *
		(Box(uv + dir * (0.0/3.0 - 0.5)) +
		 Box(uv + dir * (3.0/3.0 - 0.5)));

	float lumaB = dot(rgbB, luma);
	vec4 color = vec4(1.0, 1.0, 1.0, 1.0);
	if((lumaB < lumaMin) || (lumaB > lumaMax))
	{
		color.rgb=rgbA;
	}
	else
	{
		color.rgb=rgbB;
	}

	//color.rgb = abs(color.rgb - Box(uv));
	gl_FragColor = color;
}