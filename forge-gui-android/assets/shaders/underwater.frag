#ifdef GL_ES
#define PRECISION mediump
precision PRECISION float;
precision PRECISION int;
#else
#define PRECISION
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_amount;
uniform float u_speed;
uniform float u_time;
uniform float u_bias;

void main () {
    vec2 uv = v_texCoords;

    uv.y += (cos((uv.y + (u_time * 0.04 * u_speed)) * 45.0) * 0.0019 * u_amount) + (cos((uv.y + (u_time * 0.1 * u_speed)) * 10.0) * 0.002 * u_amount);

    uv.x += (sin((uv.y + (u_time * 0.07 * u_speed)) * 15.0) * 0.0029 * u_amount) + (sin((uv.y + (u_time * 0.1 * u_speed)) * 15.0) * 0.002 * u_amount);

	vec4 texColor = texture2D(u_texture, uv);

    gl_FragColor = mix(vec4(0.0, 0.0, 0.0, 1.0), texColor, u_bias);
}