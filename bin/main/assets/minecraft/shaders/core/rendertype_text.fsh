#version 150

#moj_import <fog.glsl>

#define PI 3.14159265359
#define TAU 6.28318530718

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float GameTime;
uniform vec4 FogColor;

uniform vec2 ScreenSize;

uniform vec4 RainbowCol = vec4(25, 26, 36, 1.0);
uniform vec4 ShinyColor = vec4(156, 216, 210, 1.0);
uniform vec4 OwnerShinyColor = vec4(249, 4, 2, 1.0);
uniform vec4 MvpShinyColor = vec4(178, 28, 28, 1.0); // <-- HOZZÁADVA
uniform vec4 AraAraShinyColor = vec4(223, 48, 200, 1.0);
uniform vec4 ItemShinyColor = vec4(154, 170, 50, 1.0);

uniform vec4 PulseColorA = vec4(0.95, 0.2, 0.2, 1.0);
uniform vec4 PulseColorB = vec4(1.0, 0.85, 0.3, 1.0);
uniform vec4 GradientColorA = vec4(0.96, 0.38, 0.09, 1.0);
uniform vec4 GradientColorB = vec4(0.04, 0.28, 0.42, 1.0);
uniform vec4 ShineBase = vec4(0.15, 0.55, 0.95, 1.0);
uniform vec4 ShineHighlight = vec4(0.85, 0.98, 1.0, 1.0);

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec4 Color;
flat in float isGui;
flat in float isRainbowColor;
flat in float isPulseEffect;
flat in float isGradientEffect;
flat in float isShineEffect;
flat in float isFadeEffect;
flat in float isEmberEffect;
flat in vec3 vPos;
flat in float isEmoji;
flat in float isShiny;
flat in float isFixedWhiteColor;
//flat in vec2 pos;
flat in vec2 UV0;
flat in vec4 sentColor;
flat in int fxLook;
flat in int fxShadow;
flat in float fxShade;
flat in float fxDrip;
flat in float fxAnchor;
in vec2 fxPos;
in vec2 fxLocal;

out vec4 fragColor;

bool isRed(vec4 a) {
return a.r == 0.988235294 && a.g == 0.329411765 && a.b == 0.329411765 && a.a == 1.0;
}

bool compareColor(vec4 a, vec4 target) {
return round(a.r * 255) == target.r
&& round(a.g * 255) == target.g
&& round(a.b * 255) == target.b;
}

float modulate(float a, float b) {
return a - b * floor(a / b);
}

vec3 hsvToRgb(vec3 c) {
vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float fxHash(float n) { return fract(sin(n * 91.3458) * 47453.5453); }
float fxHash2(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
float fxNoise2(vec2 p) {
vec2 i = floor(p), f = fract(p);
f = f * f * (3.0 - 2.0 * f);
return mix(mix(fxHash2(i), fxHash2(i + vec2(1.0, 0.0)), f.x),
mix(fxHash2(i + vec2(0.0, 1.0)), fxHash2(i + vec2(1.0, 1.0)), f.x), f.y);
}
vec3 fxHsv(vec3 c) {
vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
if (fxLook >= 0) {
// ---- fx: new chat effect family (&w codes): sampling ----
vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
vec4 tex;
if (fxDrip > 0.0) {
// Rows 0-7 stay as drawn; below the cell the baseline row repeats, cut per pixel column.
float y = fxLocal.y * (8.0 + fxDrip);
float v0 = texCoord0.y - fxLocal.y * 8.0 * texel.y;
vec4 r7 = texture(Sampler0, vec2(texCoord0.x, v0 + 7.5 * texel.y));
if (y < 7.0) tex = texture(Sampler0, vec2(texCoord0.x, v0 + y * texel.y));
else if (y < 8.0 && r7.a > 0.1) tex = r7;
else {
tex = texture(Sampler0, vec2(texCoord0.x, v0 + 6.5 * texel.y));
float len = fxDrip * fxHash2(vec2(floor(texCoord0.x / texel.x), floor(fxAnchor)));
if (y - 7.0 > len) tex.a = 0.0;
}
} else if (fxLook == 2 && fxShadow == 1) {
// Neon glow: 3x3 max of the glyph alpha, only inside the glyph's own 8x8 cell, because the
// font atlas packs glyphs edge to edge and the neighbours would bleed in.
vec2 cell0 = texCoord0 + texel - fxLocal * 10.0 * texel;
float m = 0.0;
for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
vec2 q = texCoord0 + vec2(dx, dy) * texel;
if (all(greaterThanEqual(q, cell0)) && all(lessThan(q, cell0 + 8.0 * texel)))
m = max(m, texture(Sampler0, q).a);
}
tex = vec4(1.0, 1.0, 1.0, m);
} else {
tex = texture(Sampler0, texCoord0);
}
if (tex.a < 0.1) discard;
vec4 color = tex * ColorModulator * vertexColor;

// ---- fx: looks ----
if (fxLook >= 0) {
float t = GameTime * 24000.0;
vec2 px = floor(fxPos);                          // the text pixel (1 GUI px)
vec3 rgb = vec3(1.0);
float a = 1.0;
if (fxLook == 1) {          // gold
float y = fxLocal.y;                                   // 0 top .. 1 bottom of the glyph cell
rgb = y < 0.45 ? mix(vec3(1.0, 0.95, 0.62), vec3(1.0, 0.78, 0.20), y / 0.45)
: mix(vec3(1.0, 0.78, 0.20), vec3(0.66, 0.38, 0.05), (y - 0.45) / 0.55);
float sweep = fract(t / 70.0) * 420.0 - 60.0;            // GUI px, once every 3.5 s
float glint = smoothstep(5.0, 0.0, abs(fxPos.x + fxPos.y * 0.6 - sweep));
rgb = mix(rgb, vec3(1.0, 1.0, 0.9), glint * 0.85);
} else if (fxLook == 2) {          // neon sign
float flick = fxHash(floor(t * 0.5)) > 0.92 ? 0.35 : 1.0;   // the tube stutters
float hum = 0.9 + 0.1 * sin(t * 2.7);
if (fxShadow == 1) { rgb = vec3(1.0, 0.18, 0.72); a = 0.8 * flick; }
else rgb = vec3(1.0, 0.75, 0.94) * hum * mix(0.55, 1.0, flick);
} else if (fxLook == 3) {          // galaxy
vec2 p = fxPos * 0.06 + vec2(t * 0.004, t * 0.0015);
float n = fxNoise2(p) * 0.6 + fxNoise2(p * 2.3 + 5.0) * 0.4;
rgb = mix(vec3(0.45, 0.25, 0.95), vec3(0.95, 0.45, 0.95), smoothstep(0.3, 0.75, n));
rgb = mix(rgb, vec3(0.35, 0.85, 1.0), smoothstep(0.62, 0.9, fxNoise2(p * 1.7 + 11.0)));
float h = fxHash2(px);
if (h > 0.86) rgb = mix(rgb, vec3(1.0), pow(0.5 + 0.5 * sin(t * 0.3 + h * 40.0), 6.0));
} else if (fxLook == 4) {          // hologram
rgb = vec3(0.35, 0.95, 1.0);
float scan = mod(gl_FragCoord.y, 3.0) < 1.0 ? 0.55 : 1.0;
float band = smoothstep(4.0, 0.0, abs(mod(fxPos.y - t * 0.6, 40.0) - 20.0));
rgb += band * 0.5;
a = (0.72 + 0.28 * band) * scan * (fxHash(floor(t / 2.0)) > 0.9 ? 0.45 : 1.0);
} else if (fxLook == 5) {          // frost glitter
rgb = mix(vec3(0.62, 0.88, 1.0), vec3(0.30, 0.52, 0.98), clamp(fxLocal.y * 1.15, 0.0, 1.0));
float h = fxHash2(px + 0.5);
float tw = pow(max(0.0, sin(t * 0.12 + h * 60.0)), 30.0);
rgb = mix(rgb, vec3(1.0), tw);
} else if (fxLook == 6) {          // ghost
rgb = vec3(0.82, 0.92, 1.0);
a = 0.3 + 0.55 * (0.5 + 0.5 * sin(t * 0.09 + fxAnchor * 0.15));
if (fxShadow == 1) a = 0.0;
} else if (fxLook == 7) {          // blood
float yy = fxLocal.y * (8.0 + fxDrip) / 8.0;
rgb = mix(vec3(0.95, 0.12, 0.10), vec3(0.45, 0.0, 0.03), clamp(yy, 0.0, 1.4));
if (yy < 0.16) rgb += 0.2;                                // wet highlight on the top row
} else if (fxLook == 8) {          // digital rain
float speed = 0.6 + fxHash(px.x) * 0.9;
float v = fract((t * speed - fxPos.y) / 26.0 + fxHash(px.x + 9.0));
rgb = mix(vec3(0.08, 0.45, 0.16), vec3(0.55, 1.0, 0.6), pow(1.0 - v, 3.0));
if (v < 0.05) rgb = vec3(0.9, 1.0, 0.9);
} else if (fxLook == 9) {          // rainbow, combinable
rgb = fxHsv(vec3(fxPos.x * 0.012 + fxPos.y * 0.005 - t * 0.012, 0.75, 1.0));
}
if (fxShadow == 1 && fxLook != 2) { rgb *= 0.25; if (fxLook == 4) a *= 0.5; }
color = vec4(rgb * fxShade, color.a * a);
}
fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
return;
}

vec4 color = texture(Sampler0, texCoord0) * ColorModulator;
if (color.a < 0.1) discard;
if (isFixedWhiteColor == 0.0) color = color * vertexColor;

fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);

// Custom-font marker: glyph pixels stored with alpha 254 (visually same as 255).
// Sampled per-fragment since Minecraft stitches bitmap-font glyphs into an atlas.
// int rawAlpha = int(round(texture(Sampler0, texCoord0).a * 255.0));
// bool isCustomFont = rawAlpha == 254;

if (isRed(fragColor) && ScreenSize.x - gl_FragCoord.x < 52) {
discard;
} else if (compareColor(color, RainbowCol) || isRainbowColor == 1.0) {
// Rainbow — HSV-based, position + time driven (from effectRainbow reference)
vec3 rgb = hsvToRgb(vec3(0.005 * (vPos.x + vPos.y) - GameTime * 300.0, 0.7, 1.0));
fragColor.rgb = rgb;
} else if (isPulseEffect == 1.0) {
// Pulse — mix two colors with sin (from effectPulse reference)
float t = sin(GameTime * 800.0 * TAU) * 0.5 + 0.5;
fragColor.rgb = mix(PulseColorA.rgb, PulseColorB.rgb, t);
} else if (isGradientEffect == 1.0) {
// Gradient — position-based sweep between two colors (from effectGradient reference)
float rot = 0.08 * (vPos.x + vPos.y) - GameTime * 500.0 * PI;
float t = (sin(rot) + 1.0) * 0.5;
fragColor.rgb = mix(GradientColorA.rgb, GradientColorB.rgb, t);
} else if (isEmberEffect == 1.0) {
// Ember — smooth cosine wave replaces the old fract() sawtooth, so there's no
// visible 1→0 seam every cycle. Same period/speed as before, just continuous.
float phase = (vPos.y * 0.08 - GameTime * 400.0) * TAU;
float rise = sin(phase) * 0.5 + 0.5;
float flicker = 0.75 + 0.25 * sin(GameTime * 18000.0 + vPos.x * 3.7);
float hue = mix(0.02, 0.13, rise);                 // deep red → yellow
float sat = mix(0.95, 0.25, pow(rise, 4.0));       // desaturates toward the hot tip
float val = clamp(mix(0.45, 1.15, rise) * flicker, 0.0, 1.0);
fragColor.rgb = hsvToRgb(vec3(hue, sat, val));
} else if (isShineEffect == 1.0) {
// Shine — base tint + diagonal sweep highlight (same mechanic as OwnerShinyColor)
fragColor.rgb = ShineBase.rgb;
if (isGui == 1.0) {
float sc = gl_FragCoord.x / (ScreenSize.x - (sin(radians(fract(GameTime * 2400) * 360))) * gl_FragCoord.x);
float h = abs(tan(radians((gl_FragCoord.x - gl_FragCoord.y - ScreenSize.x * fract(GameTime * 50)))) * sc / 5);
fragColor.rgb += ShineHighlight.rgb * h;
}
fragColor.a = 1.0;
} else if (isEmoji == 1.0) {
ivec2 size = textureSize(Sampler0, 0);
vec4 properties = sentColor;
float stepCount = 1;
vec2 stepper = vec2(0, size.y / 255.) * stepCount * 9;
ivec2 coord = ivec2(texCoord0 * 128.);
fragColor = texture(Sampler0, texCoord0 + stepper);
} else if (compareColor(texture(Sampler0, texCoord0), ItemShinyColor) || compareColor(texture(Sampler0, texCoord0), ShinyColor) || compareColor(texture(Sampler0, texCoord0), AraAraShinyColor) || compareColor(texture(Sampler0, texCoord0), OwnerShinyColor) || compareColor(texture(Sampler0, texCoord0), MvpShinyColor) || isShiny == 1.0) { // <-- HOZZÁADVA: MvpShinyColor ellenőrzés
if (isGui == 0.0) return;
float sc = gl_FragCoord.x / (ScreenSize.x - (sin(radians(fract(GameTime * 2400) * 360))) * gl_FragCoord.x);
fragColor += vec4(abs(tan(radians((gl_FragCoord.x - gl_FragCoord.y - ScreenSize.x * fract(GameTime * 50)))) * sc / 5));
fragColor.a = 1.0;
}

if (isFadeEffect == 1.0) {
// Fade — alpha oscillates (from effectFade reference)
fragColor.a = mix(fragColor.a, 0.0, sin(GameTime * 1200.0 * PI) * 0.5 + 0.5);
}

// Custom-font white shine: overlay a diagonal highlight on top of the original chat color
// if (isCustomFont && isGui == 1.0) {
//     float sc = gl_FragCoord.x / (ScreenSize.x - (sin(radians(fract(GameTime * 2400) * 360))) * gl_FragCoord.x);
//     float h = abs(tan(radians((gl_FragCoord.x - gl_FragCoord.y - ScreenSize.x * fract(GameTime * 50)))) * sc / 5);
//     fragColor.rgb += vec3(h);
// }
}