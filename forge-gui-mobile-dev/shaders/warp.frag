#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;

uniform float u_time;
uniform float u_speed;
uniform float u_amount;
uniform vec2 u_viewport;
uniform vec2 u_position;

float random2d(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

float randomRange (in vec2 seed, in float min, in float max) {
    return min + random2d(seed) * (max - min);
}

float insideRange(float v, float bottom, float top) {
   return step(bottom, v) - step(top, v);
}

void main()
{
    float time = floor(u_time * u_speed * 60.0);

    vec3 outCol = texture2D(u_texture, v_texCoords).rgb;

    float maxOffset = u_amount/2.0;
    for (float i = 0.0; i < 2.0; i += 1.0) {
        float sliceY = random2d(vec2(time, 2345.0 + float(i)));
        float sliceH = random2d(vec2(time, 9035.0 + float(i))) * 0.25;
        float hOffset = randomRange(vec2(time, 9625.0 + float(i)), -maxOffset, maxOffset);
        vec2 uvOff = v_texCoords;
        uvOff.x += hOffset;
        if (insideRange(v_texCoords.y, sliceY, fract(sliceY+sliceH)) == 1.0){
            outCol = texture2D(u_texture, uvOff).rgb;
        }
    }

    float maxColOffset = u_amount / 6.0;
    float rnd = random2d(vec2(time , 9545.0));
    vec2 colOffset = vec2(randomRange(vec2(time , 9545.0), -maxColOffset, maxColOffset),
                       randomRange(vec2(time , 7205.0), -maxColOffset, maxColOffset));
    if (rnd < 0.33) {
        outCol.r = texture2D(u_texture, v_texCoords + colOffset).r;
    } else if (rnd < 0.66) {
        outCol.g = texture2D(u_texture, v_texCoords + colOffset).g;
    } else {
        outCol.b = texture2D(u_texture, v_texCoords + colOffset).b;
    }

    gl_FragColor = vec4(outCol, 1.0);
}