#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform sampler2D u_texture;
uniform vec2 u_viewportInverse;
uniform vec3 u_color;
uniform float u_offset;
uniform float u_step;

varying vec4 v_color;
varying vec2 v_texCoord;

#define ALPHA_VALUE_BORDER 0.5

void main() {
   vec2 T = v_texCoord.xy;

   float alpha = 0.0;
   bool allin = true;
   for( float ix = -u_offset; ix < u_offset; ix += u_step )
   {
      for( float iy = -u_offset; iy < u_offset; iy += u_step )
       {
          float newAlpha = texture2D(u_texture, T + vec2(ix, iy) * u_viewportInverse).a;
          allin = allin && newAlpha > ALPHA_VALUE_BORDER;
          if (newAlpha > ALPHA_VALUE_BORDER && newAlpha >= alpha)
          {
             alpha = newAlpha;
          }
      }
   }
   if (allin)
   {
      alpha = 0.0;
   }

   gl_FragColor = vec4(u_color,alpha);
}