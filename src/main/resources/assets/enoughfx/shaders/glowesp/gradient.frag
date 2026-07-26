#version 330 core

in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uMask;
uniform sampler2D uBlur;
uniform sampler2D uAutoColor;
uniform vec2 uTexelSize;
uniform vec4 uColorTop;
uniform vec4 uColorBottom;
uniform float uOutlineWidth;
uniform float uGlowStrength;
uniform float uOutlineStrength;
uniform float uOpacity;
uniform float uTime;
uniform int uDebugView;
uniform int uColorStyle;
uniform int uAutoColorEnabled;

float maskAt(vec2 uv) {
    return texture(uMask, uv).a;
}

void main() {
    float mask = maskAt(vUv);
    float blurred = texture(uBlur, vUv).a;
    float primaryFlow = sin(vUv.x * 5.2 + uTime * 0.58);
    float secondaryFlow = sin(vUv.x * 11.0 - uTime * 0.24);
    float gradientPosition = clamp(0.5 + primaryFlow * 0.42 + secondaryFlow * 0.08, 0.0, 1.0);
    gradientPosition = smoothstep(0.04, 0.96, gradientPosition);
    vec3 topColor = uColorTop.rgb;
    vec3 bottomColor = uColorBottom.rgb;
    if (uAutoColorEnabled != 0) {
        vec3 sampledColor = texture(uAutoColor, vec2(0.5)).rgb;
        topColor = mix(sampledColor, vec3(1.0), 0.24);
        bottomColor = sampledColor * 0.52;
    }
    vec3 gradient = uColorStyle == 1
        ? topColor
        : mix(bottomColor, topColor, gradientPosition);

    if (uDebugView == 1) {
        FragColor = vec4(gradient, mask * uOpacity);
        return;
    }
    if (uDebugView == 2) {
        float externalBlur = blurred * (1.0 - mask);
        FragColor = vec4(gradient, externalBlur * uOpacity);
        return;
    }

    vec2 d = uTexelSize * max(uOutlineWidth, 0.5);
    vec2 h = d * 0.5;

    float nearby = mask;
    nearby = max(nearby, maskAt(vUv + vec2(d.x, 0.0)));
    nearby = max(nearby, maskAt(vUv - vec2(d.x, 0.0)));
    nearby = max(nearby, maskAt(vUv + vec2(0.0, d.y)));
    nearby = max(nearby, maskAt(vUv - vec2(0.0, d.y)));
    nearby = max(nearby, maskAt(vUv + d));
    nearby = max(nearby, maskAt(vUv - d));
    nearby = max(nearby, maskAt(vUv + vec2(d.x, -d.y)));
    nearby = max(nearby, maskAt(vUv + vec2(-d.x, d.y)));
    nearby = max(nearby, maskAt(vUv + vec2(h.x, 0.0)));
    nearby = max(nearby, maskAt(vUv - vec2(h.x, 0.0)));
    nearby = max(nearby, maskAt(vUv + vec2(0.0, h.y)));
    nearby = max(nearby, maskAt(vUv - vec2(0.0, h.y)));

    float outside = 1.0 - mask;
    float outline = max(nearby - mask, 0.0) * outside;
    float glow = pow(clamp(blurred, 0.0, 1.0), 0.72) * outside;
    vec3 hotEdge = mix(gradient, vec3(1.0), 0.22);

    float glowAlpha = 1.0 - exp(-glow * uGlowStrength);
    float outlineAlpha = clamp(outline * uOutlineStrength, 0.0, 1.0);
    float alpha = max(glowAlpha, outlineAlpha) * uOpacity;
    vec3 color = mix(gradient, hotEdge, outlineAlpha);

    FragColor = vec4(color, alpha);
}
