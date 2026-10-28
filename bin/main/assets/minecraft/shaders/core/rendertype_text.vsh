#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform vec4 BouncyColor = vec4(6, 26, 1, 1.0);
uniform vec4 GlitchColor = vec4(6, 26, 2, 1.0);
uniform vec4 BlinkingColor = vec4(6, 26, 4, 1.0);
uniform vec4 RainbowColor = vec4(6, 26, 8, 1.0);
uniform vec4 PulseColor = vec4(6, 26, 16, 1.0);
uniform vec4 GradientColor = vec4(6, 26, 32, 1.0);
uniform vec4 ShineColor = vec4(6, 26, 64, 1.0);
uniform vec4 FadeColor = vec4(6, 26, 128, 1.0);
uniform vec4 EmberColor = vec4(12, 200, 4, 1.0);
uniform vec4 SpinColor  = vec4(14, 201, 2, 1.0);
uniform vec4 BloomColor = vec4(16, 203, 3, 1.0);

uniform vec4 EmojiColor = vec4(19, 22, 17, 1.0);
uniform vec4 ShinyColor = vec4(156, 216, 210, 1.0);
uniform vec4 OwnerShinyColor = vec4(249, 4, 2, 1.0);
uniform vec4 MvpShinyColor = vec4(178, 28, 28, 1.0); // HOZZÁADVA
uniform vec4 AraAraShinyColor = vec4(223, 48, 200, 1.0);
uniform vec4 ItemShinyColor = vec4(154, 170, 50, 1.0);

uniform float amount = 5;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform float GameTime;
uniform vec2 ScreenSize;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
flat out float isGui;
flat out float isRainbowColor;
flat out float isPulseEffect;
flat out float isGradientEffect;
flat out float isShineEffect;
flat out float isFadeEffect;
flat out float isEmberEffect;
flat out vec3 vPos;
flat out float isEmoji;
flat out float isShiny;
flat out float isFixedWhiteColor;
//flat out vec2 pos;
flat out vec4 sentColor;
// New chat effect family (&w codes), see the fx block in main()
flat out int fxLook;
flat out int fxShadow;
flat out float fxShade;
flat out float fxDrip;
flat out float fxAnchor;
out vec2 fxPos;
out vec2 fxLocal;

bool compareColor(vec4 a, vec4 target) {
    return round(a.r * 255) == round(target.r)
    && round(a.g * 255) == round(target.g)
    && round(a.b * 255) == round(target.b);
}

vec4 addColors(vec4 a, vec4 b) {
    return vec4(a.r + b.r, a.g + b.g, a.b + b.b, 1.0);
}

float getLastColor() {
    return RainbowColor.b;
}

float noise(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

bool bitwise_and(float number, float mask) {
    return mod(floor(number / mask), 2.0) == 1.0;
}

// ---- New chat effect family ------------------------------------------------
// Marker colour (10, 162 + 4*look, 194 + 4*motion). Every channel is 2 mod 4, so the drop
// shadow (drawn at floor(rgb / 4)) arrives as exactly (2, 40 + look, 48 + motion).
float fxHash(float n) { return fract(sin(n * 91.3458) * 47453.5453); }
float fxNoise(float x) {
    float i = floor(x), f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(fxHash(i), fxHash(i + 1.0), f);
}
vec2 fxRotate(vec2 p, float r) { float c = cos(r), s = sin(r); return vec2(p.x * c - p.y * s, p.x * s + p.y * c); }
// Minecraft builds every glyph quad as top-left, bottom-left, bottom-right, top-right.
vec2 fxCornerOf(int v) {
    if (v == 0) return vec2(0.0, 0.0);
    if (v == 1) return vec2(0.0, 1.0);
    if (v == 2) return vec2(1.0, 1.0);
    return vec2(1.0, 0.0);
}

float blinkSpeed = 5.0;
float blinkThreshold = 0.1;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;

    isGui = 1.0;
    isRainbowColor = 0.0;
    isPulseEffect = 0.0;
    isGradientEffect = 0.0;
    isShineEffect = 0.0;
    isFadeEffect = 0.0;
    isEmberEffect = 0.0;
    vPos = Position;
    isEmoji = 0.0;
    isShiny = 0.0;
    isFixedWhiteColor = 0.0;
    //pos = vec2(0, 0);
    sentColor = vec4(0);

    if (gl_Position.z == 0.0) {
        isGui = 0.0;
    }

    // ---- fx: new chat effect family (&w codes). Decodes the marker colour, moves the glyph
    // in GUI pixels and returns early, so none of the older effects below touch these glyphs.
    fxLook = -1; fxShadow = 0; fxShade = 1.0; fxDrip = 0.0;
    int fxMove = 0;
    vec3 pos = Position;
    ivec3 fxc = ivec3(round(Color.rgb * 255.0));
    if (fxc.r == 10 && fxc.g >= 162 && fxc.g <= 222 && fxc.b >= 194 && (fxc.g & 3) == 2 && (fxc.b & 3) == 2) {
        fxLook = (fxc.g - 162) / 4; fxMove = (fxc.b - 194) / 4;
    } else if (fxc.r == 2 && fxc.g >= 40 && fxc.g <= 55 && fxc.b >= 48 && fxc.b <= 62) {
        fxLook = fxc.g - 40; fxMove = fxc.b - 48; fxShadow = 1;      // this glyph's drop shadow
    }
    vec2 corner = fxCornerOf(gl_VertexID % 4);
    fxLocal = corner;
    fxPos = Position.xy - float(fxShadow);   // GUI px; the shadow is put back onto its letter
    float ax = fxPos.x - corner.x * 8.0;     // cell's left edge: same for all 4 corners and the shadow
    fxAnchor = ax;
    if (fxLook >= 0) {
        vertexColor = vec4(1.0, 1.0, 1.0, Color.a);                  // keep chat fade
        float t = GameTime * 24000.0;                                // ticks
        bool fxGui = ProjMat[3][3] != 0.0;                           // orthographic = GUI
        vec2 lc = corner * 8.0 - 4.0;                                // corner relative to the cell centre
        vec2 off = vec2(0.0);
        if (fxGui) {
            if (fxMove == 1) {          // wave
                                        off.y = sin(t * 0.35 - ax * 0.22) * 1.5;
            } else if (fxMove == 2) {          // shake / rage
                                               float k = floor(t);
                                               off = (vec2(fxNoise(ax * 0.31 + k * 7.1), fxNoise(ax * 0.31 + k * 3.7 + 50.0)) - 0.5) * 1.6;
            } else if (fxMove == 3) {          // swing
                                               float ang = sin(t * 0.22 + ax * 0.17) * 0.32;
                                               vec2 hang = lc + vec2(0.0, 4.0);                          // corner relative to the top centre
                                               off = fxRotate(hang, ang) - hang;
            } else if (fxMove == 4) {          // coin flip
                                               float c = cos(t * 0.16 + ax * 0.11);
                                               off.x = lc.x * (c - 1.0);
                                               fxShade = (0.6 + 0.4 * abs(c)) * (c < 0.0 ? 0.75 : 1.0);
            } else if (fxMove == 5) {          // burst
                                               float u = fract(t / 80.0);
                                               float e = u < 0.08 ? sin(u / 0.08 * 1.5708) : exp(-(u - 0.08) * 9.0) * cos((u - 0.08) * 22.0);
                                               float dir = ax * 0.47;
                                               off = vec2(cos(dir), sin(dir) - 0.4) * 9.0 * e
                                               + fxRotate(lc, e * (fxNoise(ax * 0.2) - 0.5) * 3.0) - lc;
            } else if (fxMove == 6) {          // drip
                                               float u = fract(t / 60.0 + fxNoise(ax * 0.13) * 3.0);
                                               fxDrip = 6.0 * smoothstep(0.0, 0.8, u) * step(u, 0.9);
                                               off.y = corner.y * fxDrip;                                // only the bottom corners move
            }
            if (fxLook == 4 && fxHash(floor(t / 3.0)) > 0.85) off.x += (fxHash(floor(t / 3.0) + 1.0) - 0.5) * 3.0; // hologram slip
        }
        if (fxLook == 2 && fxShadow == 1) {                          // neon: shadow becomes a centred glow
                                                                     vec2 grow = corner * 2.0 - 1.0;
                                                                     off += grow - 1.0;
                                                                     texCoord0 += grow / vec2(textureSize(Sampler0, 0));
        }
        pos.xy += off;
    }
    if (fxLook >= 0) {
        gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
        vertexDistance = length((ModelViewMat * vec4(pos, 1.0)).xyz);
        vertexColor *= texelFetch(Sampler2, UV2 / 16, 0);
        return;
    }

    bool isSubtitle = (gl_Position.y < 0.0);

    // Detect title text (Position.z ~0.12 in 1.19.4+)
    if (!isSubtitle && (abs(Position.z - 0.0) < 0.001 || abs(Position.z - 0.12) < 0.001)) {
        // Use alpha as fade-in progress (0.0 -> 1.0)
        float progress = clamp(vertexColor.a, 0.0, 1.0);

        // Invert so animation starts when title starts fading in
        float slide = (1.0 - progress);

        // Apply Y offset (40px slide down at start → 0px when fully visible)
        gl_Position.y += slide * 0.1;
    }

    // Use raw Color, not lightmap-multiplied vertexColor — otherwise
    // lightmap scales the blue byte and effect bits collide (e.g. 64 → 13 = bouncy+blink+rainbow)
    vec4 verColor = vec4(Color.rgb, 1.0);
    float combinedColor = round(verColor.b * 255);

    // UI glyph convention: glyphs tinted #FCF8F4 (visually white) render untinted and their
    // drop-shadow pass — which arrives with rgb * 0.25 = exactly (63, 62, 61) — is collapsed
    // off-screen. Channels are multiples of 4 so the quartered values round unambiguously and
    // collide with no vanilla colour's shadow. Used by the generator panel / fishing UI cards.
    if (compareColor(verColor, vec4(63.0, 62.0, 61.0, 1.0))) {
        gl_Position = vec4(2.0e10, 2.0e10, 2.0e10, 1.0); // shadow quad: cull
    }
    if (compareColor(verColor, vec4(252.0, 248.0, 244.0, 1.0))) {
        vertexColor = vec4(1.0, 1.0, 1.0, Color.a) * texelFetch(Sampler2, UV2 / 16, 0);
    }

    if (compareColor(verColor, vec4(BouncyColor.r, BouncyColor.g, combinedColor, 1.0))) {
        if (bitwise_and(combinedColor, BouncyColor.b)) {
            // Use gl_VertexID / 4 so all 4 corners of one glyph share the same phase
            // (glyph moves as a whole); consecutive glyphs are offset in time.
            int glyphId = gl_VertexID / 4;
            float time = fract(GameTime * 2000.0 + float(glyphId) * 0.137);
            gl_Position.y += abs(sin(radians(360 * time)) * .01) / 1.5;
            vertexColor = vec4(1.0, 1.0, 1.0, 1.0);
        }
        if (bitwise_and(combinedColor, GlitchColor.b)) {
            if (compareColor(verColor, GlitchColor)) vertexColor = vec4(1);
            float glitchCycle = 3.0;
            float glitchDuration = 0.5;
            float consistentTime = (GameTime * 24000.0 / 20.0);
            float cycleTime = mod(consistentTime, glitchCycle);

            if (cycleTime < glitchDuration) {
                float glitchIntensity = smoothstep(0.0, glitchDuration, cycleTime);
                float random = fract(sin(dot(gl_Position.xy + vec2(consistentTime), vec2(12.9898, 78.233))) * 43758.5453);
                gl_Position.x += (random * 0.05 - 0.025) * glitchIntensity;
            }
        }
        if (bitwise_and(combinedColor, BlinkingColor.b)) {
            if (compareColor(verColor, BlinkingColor)) vertexColor = vec4(1);
            // Clean binary on/off blink (matches effectBlink from new code)
            float blinkSpeedFactor = 0.2;
            if (sin(GameTime * 6400.0 * blinkSpeedFactor * 3.14159265359) < 0.0) {
                vertexColor.a = 0.0;
            }
        }
        if (bitwise_and(combinedColor, RainbowColor.b)) {
            isRainbowColor = 1.0;
            vertexColor.rgb = vec3(1.0);
        }
        if (bitwise_and(combinedColor, PulseColor.b)) {
            if (compareColor(verColor, PulseColor)) vertexColor = vec4(1);
            isPulseEffect = 1.0;
        }
        if (bitwise_and(combinedColor, GradientColor.b)) {
            if (compareColor(verColor, GradientColor)) vertexColor = vec4(1);
            isGradientEffect = 1.0;
        }
        if (bitwise_and(combinedColor, ShineColor.b)) {
            if (compareColor(verColor, ShineColor)) vertexColor = vec4(1);
            isShineEffect = 1.0;
        }
        if (bitwise_and(combinedColor, FadeColor.b)) {
            if (compareColor(verColor, FadeColor)) vertexColor = vec4(1);
            isFadeEffect = 1.0;
        }
    }
    else if (compareColor(verColor, EmberColor)) {
        // Ember — fragment handles the flickering hot gradient
        isEmberEffect = 1.0;
        vertexColor = vec4(1.0);
    }
    else if (compareColor(verColor, SpinColor)) {
        // Orbit — each glyph bobs in a small circle. Phase is offset per-glyph via
        // gl_VertexID / 4, so all 4 vertices of one glyph share a phase (glyph stays
        // upright), and consecutive glyphs ripple through the orbit in sequence.
        vertexColor = vec4(1.0);

        int glyphId = gl_VertexID / 4;
        float phase = float(glyphId) * 0.9;
        float time  = GameTime * 2000.0 + phase;
        float radius = 0.004;

        gl_Position.x += cos(time) * radius;
        gl_Position.y += sin(time) * radius;
    }
    else if (compareColor(verColor, BloomColor)) {
        // Bloom — each glyph breathes (pulses in size). Phase-offset per glyph so a
        // scale wave ripples across the text. Each vertex is offset outward/inward
        // from its glyph's center based on which corner of the quad it is.
        vertexColor = vec4(1.0);

        int vId     = gl_VertexID % 4;
        int glyphId = gl_VertexID / 4;

        // Corner offset from the quad's center (-0.5 / +0.5 on each axis)
        vec2 cornerOffset;
        if (vId == 0)      cornerOffset = vec2(-0.5, -0.5);
        else if (vId == 1) cornerOffset = vec2( 0.5, -0.5);
        else if (vId == 2) cornerOffset = vec2( 0.5,  0.5);
        else               cornerOffset = vec2(-0.5,  0.5);

        float phase = float(glyphId) * 0.8;
        float time  = GameTime * 1500.0 + phase;

        // Scale oscillates between 0.85 and 1.15 (±15% size swing)
        float scale = 1.0 + sin(time) * 0.15;

        // 0.04 ≈ glyph width in GUI clip space — tune if glyphs swell too much / little
        gl_Position.xy += cornerOffset * (scale - 1.0) * 0.04;
    }
    else if (compareColor(Color, ItemShinyColor)) {
        isShiny = 1.0;
        isFixedWhiteColor = 1.0;
    }
    else if (compareColor(Color, OwnerShinyColor) || compareColor(Color, AraAraShinyColor) || compareColor(Color, ShinyColor)) {
        isShiny = 1.0;
    }
    else if (compareColor(Color, MvpShinyColor)) { // HOZZÁADVA
                                                   isShiny = 1.0;
                                                   isFixedWhiteColor = 1.0;
    }
    else if (compareColor(verColor, EmojiColor)) {
        gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
        //        vertexColor = vec4(1.0);
        //        vec4 color = texture(Sampler0, UV0);
        //        gl_Position = vec4(color.x * 2. - 1., color.y * 2. - 1., gl_Position.z, 1.0);
        //        texCoord0 = color.xy + vec2(20, 20);
        ivec2 size = textureSize(Sampler0, 0);
        sentColor = texelFetch(Sampler2, UV2 / 16, 0);
        isEmoji = 1.0;
        //        texCoord0 = vec2(UV0.r, UV0.g + 100000);
    }
    ivec2 size = textureSize(Sampler2, 0);
    sentColor = texelFetch(Sampler2, ivec2(0, size.y - 1), 0);
}