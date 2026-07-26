#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform vec2 TexelSize;
uniform vec4 FillColor;
uniform vec4 GlowColor;
uniform float FillOpacity;
uniform float GlowStrength;
uniform float OutlineStrength;
uniform float OutlineWidth;
uniform float EchoDecay;
uniform int Mode;
uniform int Pass;

in vec2 TexCoord;
in vec4 FragColor;
out vec4 OutColor;

float maskAt(vec2 uv) {
    return texture(Sampler0, uv).a;
}

float outlineAt(vec2 uv, float center) {
    vec2 px = TexelSize * max(OutlineWidth, 0.5);
    float nearby = 0.0;
    for (int i = 0; i < 16; i++) {
        float angle = float(i) * 0.39269908;
        vec2 dir = vec2(cos(angle), sin(angle));
        nearby = max(nearby, maskAt(uv + dir * px * 0.55));
        nearby = max(nearby, maskAt(uv + dir * px));
    }
    float outside = 1.0 - smoothstep(0.02, 0.18, center);
    return smoothstep(0.02, 0.35, nearby) * outside;
}

void main() {
    vec2 uv = TexCoord;
    float mask = maskAt(uv);
    float blurred = texture(Sampler1, uv).a;
    float outside = 1.0 - smoothstep(0.02, 0.22, mask);
    float outer = smoothstep(0.01, 0.58, blurred) * outside;
    float outline = outlineAt(uv, mask);

    if (Pass == 0) {
        vec4 previous = texture(Sampler2, uv) * clamp(EchoDecay, 0.0, 0.995);
        float currentAlpha = clamp(
            outer * GlowStrength * 0.7
            + outline * OutlineStrength * 0.55
            + mask * FillOpacity * 0.28,
            0.0,
            0.88
        );
        vec3 currentColor = mix(FillColor.rgb, GlowColor.rgb, clamp(outer + outline, 0.0, 1.0));
        float alpha = max(previous.a, currentAlpha);
        vec3 color = currentAlpha >= previous.a ? currentColor : previous.rgb;
        OutColor = vec4(color, alpha);
        return;
    }

    float fullBoost = 1.0;
    float fillAlpha = mask * FillOpacity * fullBoost;
    float glowAlpha = outer * GlowStrength * (Mode == 2 ? 1.25 : 1.45);
    float outlineAlpha = outline * OutlineStrength * 1.15;
    vec4 echo = Mode == 1 ? texture(Sampler2, uv) : vec4(0.0);

    float alpha = clamp(fillAlpha + glowAlpha + outlineAlpha + echo.a * 0.72, 0.0, 1.0);
    if (alpha < 0.002) {
        discard;
    }

    vec3 color = FillColor.rgb * fillAlpha;
    color += GlowColor.rgb * (glowAlpha + outlineAlpha);
    color += echo.rgb * echo.a * 0.72;
    color /= max(fillAlpha + glowAlpha + outlineAlpha + echo.a * 0.72, 0.001);
    OutColor = vec4(color, alpha) * FragColor;
}
