#version 330 core
in vec2 vUv;
uniform sampler2D uSource;
uniform vec2 uResolution;
uniform int uRippleCount;
uniform vec2 uRippleCenter[5];
uniform float uRippleAge[5];
uniform float uRipplePower[5];
uniform float uRippleKind[5];
uniform vec3 uRipplePreviousColorTop[5];
uniform vec3 uRipplePreviousColorBottom[5];
uniform vec3 uRippleColorTop[5];
uniform vec3 uRippleColorBottom[5];
uniform vec4 uThemeGuiRect;
uniform vec4 uThemePanelRect;
uniform vec2 uThemeRadii;
out vec4 fragColor;

vec3 srgb_to_linear(vec3 c) {
    return vec3(
        c.r <= 0.04045 ? c.r / 12.92 : pow((c.r + 0.055) / 1.055, 2.4),
        c.g <= 0.04045 ? c.g / 12.92 : pow((c.g + 0.055) / 1.055, 2.4),
        c.b <= 0.04045 ? c.b / 12.92 : pow((c.b + 0.055) / 1.055, 2.4)
    );
}

vec3 linear_to_srgb(vec3 c) {
    c = clamp(c, 0.0, 1.0);
    return vec3(
        c.r <= 0.0031308 ? c.r * 12.92 : 1.055 * pow(c.r, 0.4166666666666667) - 0.055,
        c.g <= 0.0031308 ? c.g * 12.92 : 1.055 * pow(c.g, 0.4166666666666667) - 0.055,
        c.b <= 0.0031308 ? c.b * 12.92 : 1.055 * pow(c.b, 0.4166666666666667) - 0.055
    );
}

vec3 linear_srgb_to_oklab(vec3 c) {
    float l = 0.4122214708 * c.r + 0.5363325363 * c.g + 0.0514459929 * c.b;
    float m = 0.2119034982 * c.r + 0.6806995451 * c.g + 0.1073969566 * c.b;
    float s = 0.0883024619 * c.r + 0.2817188376 * c.g + 0.6299787005 * c.b;
    float l_ = sign(l) * pow(abs(l), 0.333333333333);
    float m_ = sign(m) * pow(abs(m), 0.333333333333);
    float s_ = sign(s) * pow(abs(s), 0.333333333333);
    return vec3(
        0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_,
        1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_,
        0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_
    );
}

vec3 oklab_to_linear_srgb(vec3 c) {
    float l_ = c.x + 0.3963377774 * c.y + 0.2158037573 * c.z;
    float m_ = c.x - 0.1055613458 * c.y - 0.0638541728 * c.z;
    float s_ = c.x - 0.0894841775 * c.y - 1.2914855480 * c.z;
    float l = l_ * l_ * l_;
    float m = m_ * m_ * m_;
    float s = s_ * s_ * s_;
    return vec3(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    );
}

vec3 oklab_mix(vec3 colA, vec3 colB, float t) {
    vec3 labA = linear_srgb_to_oklab(colA);
    vec3 labB = linear_srgb_to_oklab(colB);
    return oklab_to_linear_srgb(mix(labA, labB, t));
}

vec3 oklab_mix_srgb(vec3 colA, vec3 colB, float t) {
    return linear_to_srgb(oklab_mix(srgb_to_linear(colA), srgb_to_linear(colB), clamp(t, 0.0, 1.0)));
}

float easeOutQuad(float value) {
    float t = clamp(value, 0.0, 1.0);
    return 1.0 - (1.0 - t) * (1.0 - t);
}

float smootherstep(float value) {
    float t = clamp(value, 0.0, 1.0);
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}

float springDecay(float value) {
    float t = clamp(value, 0.0, 1.0);
    float envelope = exp(-2.72 * t);
    float tail = 1.0 - smoothstep(0.72, 1.0, t);
    return envelope * tail * tail;
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + vec3(33.33));
    return fract((p3.x + p3.y) * p3.z);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

vec4 sampleSource(vec2 uv) {
    return texture(uSource, clamp(uv, vec2(0.0), vec2(1.0)));
}

float sdRoundRect(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + vec2(radius);
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

float roundedRectMask(vec2 p, vec4 rect, float radius) {
    if (rect.z <= 0.5 || rect.w <= 0.5) {
        return 1.0;
    }
    vec2 center = rect.xy + rect.zw * 0.5;
    float d = sdRoundRect(p - center, rect.zw * 0.5, clamp(radius, 0.0, min(rect.z, rect.w) * 0.5));
    return 1.0 - smoothstep(0.0, 1.75, d);
}

void main() {
    vec2 pixel = vec2(vUv.x * uResolution.x, (1.0 - vUv.y) * uResolution.y);
    float diagonal = max(length(uResolution), 1.0);
    vec2 totalOffsetPx = vec2(0.0);
    vec2 totalSwirlPx = vec2(0.0);
    float totalSplit = 0.0;
    float totalGlow = 0.0;
    float totalExposure = 0.0;
    float totalThemePresence = 0.0;
    float totalThemeReveal = 0.0;
    float totalThemeIgnition = 0.0;
    float totalThemeMute = 0.0;
    vec3 totalGlowColor = vec3(0.0);
    vec3 totalThemeColor = vec3(0.0);
    float totalThemeColorWeight = 0.0;

    for (int i = 0; i < 5; i++) {
        if (i >= uRippleCount) {
            break;
        }

        float age = clamp(uRippleAge[i], 0.0, 1.0);
        if (age >= 1.0) {
            continue;
        }

        float eased = easeOutQuad(age);
        float kind = clamp(uRippleKind[i], 0.0, 1.0);
        float birth = smootherstep(age / mix(0.065, 0.22, kind));
        float tail = smoothstep(0.60, 1.0, age);
        float decay = springDecay(age) * birth * mix(1.0, 1.0 - smoothstep(0.84, 1.0, age), kind);
        float power = max(uRipplePower[i], 0.0);
        vec2 delta = pixel - uRippleCenter[i];
        float dist = length(delta);
        vec2 dir = dist > 0.0001 ? delta / dist : vec2(0.0, 1.0);
        vec2 tangent = vec2(-dir.y, dir.x);
        float travel = diagonal * mix(mix(0.004, 0.026, kind), mix(0.0575, 0.90, kind), eased);
        float band = mix(diagonal * mix(0.0095, 0.036, kind), diagonal * mix(0.0024, 0.015, kind), eased);
        float front = abs(dist - travel);
        float shell = 1.0 - smoothstep(0.0, band, front);
        float phase = clamp(1.0 - front / max(band, 0.0001), 0.0, 1.0);
        float stableNoise = valueNoise(pixel * 0.018 + vec2(float(i) * 18.37, float(i) * 4.91));
        float fineNoise = valueNoise(pixel * 0.061 + vec2(float(i) * 9.17, 2.4));
        float dissolveThreshold = mix(-0.22, 1.18, tail);
        float dissolveMask = mix(1.0, smoothstep(dissolveThreshold - 0.38, dissolveThreshold + 0.18, stableNoise * 0.72 + fineNoise * 0.26 + phase * 0.16), kind);
        float shock = sin(phase * 1.5707963) * shell * dissolveMask;
        float flickerNoise = hash12(pixel * 0.0085 + vec2(float(i) * 17.0, age * 11.0));
        float noise = mix(flickerNoise, stableNoise, kind);
        float dissolve = mix(0.985, 1.018, noise) * dissolveMask;
        float torsion = sin(phase * 3.1415926 + age * 2.4 + noise * 1.1) * shell * mix(1.0, 0.18, kind) * dissolveMask;
        float fresnel = pow(clamp(1.0 - abs(phase * 2.0 - 1.0), 0.0, 1.0), mix(1.8, 0.58, kind));
        float lens = exp(-(front * front) / max(1.0, band * band * mix(1.8, 5.4, kind))) * decay * power * dissolveMask;
        float energy = shock * decay * power * dissolve;
        float colorFlow = 0.5 + 0.5 * sin((phase * 1.7 + age * 1.15 + noise * 0.35 + float(i) * 0.17) * 6.2831853);
        vec3 previousAccentColor = oklab_mix_srgb(uRipplePreviousColorBottom[i], uRipplePreviousColorTop[i], clamp(phase + stableNoise * 0.08, 0.0, 1.0));
        vec3 accentColor = oklab_mix_srgb(uRippleColorBottom[i], uRippleColorTop[i], clamp(phase + noise * 0.08, 0.0, 1.0));
        vec3 spectralA = oklab_mix_srgb(uRippleColorBottom[i], vec3(1.0, 0.36, 0.92), 0.32 + 0.18 * colorFlow);
        vec3 spectralB = oklab_mix_srgb(uRippleColorTop[i], vec3(0.28, 0.92, 1.0), 0.34 + 0.16 * (1.0 - colorFlow));
        vec3 rippleColor = oklab_mix_srgb(accentColor, oklab_mix_srgb(spectralA, spectralB, colorFlow), 0.58 * kind);
        float angle = atan(dir.y, dir.x);
        float angularWave = 0.5 + 0.5 * sin(angle * 6.0 + age * 8.4 + stableNoise * 1.7 + float(i) * 1.13);
        float angularBlade = pow(angularWave, 3.4);
        float warpFalloff = mix(1.0, 1.0 - smoothstep(0.64, 1.0, age), kind);
        float ribbon = kind * exp(-(front * front) / max(1.0, band * band * 18.0)) * decay * power * dissolveMask * (1.0 - smoothstep(0.78, 1.0, age));
        float frontSigned = travel - dist;
        float reveal = kind * smoothstep(-band * 1.55, band * 2.45, frontSigned) * birth;
        float revealEase = smootherstep(clamp(reveal, 0.0, 1.0));
        float passage = kind * exp(-(frontSigned * frontSigned) / max(1.0, band * band * 14.0)) * birth * (1.0 - smoothstep(0.76, 1.0, age)) * dissolveMask;
        float secondaryFront = kind * exp(-((front - band * 1.42) * (front - band * 1.42)) / max(1.0, band * band * 1.9)) * birth * (1.0 - smoothstep(0.70, 0.96, age)) * dissolveMask;
        float sparkCell = hash12(floor(pixel * 0.035) + vec2(float(i) * 23.0, 7.0));
        float sparkPulse = 0.5 + 0.5 * sin(age * 28.0 + sparkCell * 6.2831853);
        float sparks = kind * shell * birth * (1.0 - smoothstep(0.62, 0.92, age)) * smoothstep(0.965, 1.0, sparkCell) * smootherstep(sparkPulse) * dissolveMask;
        float caustic = kind * power * (passage * 0.52 + secondaryFront * 0.48 + shell * fresnel * 0.24) * angularBlade * (1.0 - smoothstep(0.74, 1.0, age));
        float settledTrail = revealEase * (0.16 + 0.58 * (1.0 - smoothstep(diagonal * 0.12, diagonal * 0.52, max(frontSigned, 0.0))));
        float preWave = kind * (1.0 - revealEase) * smoothstep(0.04, 0.24, age) * (1.0 - smoothstep(0.70, 1.0, age));
        vec3 revealColor = oklab_mix_srgb(previousAccentColor, rippleColor, 0.68 + 0.32 * colorFlow);
        vec3 causticColor = oklab_mix_srgb(oklab_mix_srgb(spectralA, spectralB, angularWave), oklab_mix_srgb(rippleColor, vec3(1.0), 0.24 + fresnel * 0.18), 0.34);

        totalOffsetPx += dir * (energy * (diagonal * mix(0.003, 0.00062, kind) + mix(1.0, 0.14, kind)) + lens * diagonal * mix(0.0008, 0.00022, kind)) * warpFalloff;
        totalSwirlPx += tangent * torsion * decay * power * (diagonal * mix(0.00024, 0.000006, kind) + mix(0.085, 0.006, kind)) * warpFalloff;
        totalSplit += energy * mix(0.76 + phase * 0.12, 0.26 + phase * 0.045, kind) + ribbon * 0.12 + caustic * 0.055;
        totalGlow += shock * (0.2 + decay * mix(0.42, 0.92, kind)) * power + fresnel * lens * mix(0.0, 1.52, kind) + ribbon * 1.02 + caustic * 0.72 + secondaryFront * 0.24 + sparks * 0.62;
        totalExposure += max(0.0, energy) * (0.09 + mix(0.3, 0.42, kind) * shell) + fresnel * lens * mix(0.0, 0.28, kind) + ribbon * 0.10 + caustic * 0.045 + sparks * 0.038;
        totalThemePresence += kind * clamp(shock + fresnel * lens + ribbon + caustic + sparks, 0.0, 1.0);
        totalGlowColor += rippleColor * (energy * mix(0.18, 0.86, kind) + fresnel * lens * mix(0.0, 1.48, kind) + ribbon * 1.38);
        totalGlowColor += causticColor * (caustic * 1.65 + secondaryFront * 0.58 + sparks * 1.2);
        totalThemeReveal = max(totalThemeReveal, settledTrail * (0.18 + 0.30 * power));
        totalThemeIgnition += passage * power * (0.48 + fresnel * 0.54 + ribbon * 0.28) + caustic * 0.24 + secondaryFront * 0.10;
        totalThemeMute = max(totalThemeMute, preWave * 0.12);
        totalThemeColor += revealColor * (settledTrail * 0.62 + passage * 1.62 + ribbon * 0.76);
        totalThemeColor += causticColor * (caustic * 0.92 + secondaryFront * 0.34);
        totalThemeColorWeight += settledTrail * 0.62 + passage * 1.62 + ribbon * 0.76 + caustic * 0.92 + secondaryFront * 0.34;
    }

    vec2 totalWarpPx = totalOffsetPx + totalSwirlPx;
    vec2 offsetUv = totalWarpPx / max(uResolution, vec2(1.0));
    float split = clamp(totalSplit, 0.0, 1.0);
    vec2 dispersionDir = length(totalWarpPx) > 0.0001 ? normalize(totalWarpPx) : vec2(0.0, 1.0);
    vec2 compactSplitUv = dispersionDir * ((0.26 + split * 0.88) / max(uResolution, vec2(1.0)));

    vec4 baseSample = sampleSource(vUv + offsetUv * 0.988);
    vec4 redSample = sampleSource(vUv + offsetUv * (1.0 + split * 0.12) + compactSplitUv * (0.34 + split * 0.18));
    vec4 greenSample = sampleSource(vUv + offsetUv * (0.996 + split * 0.024));
    vec4 blueSample = sampleSource(vUv + offsetUv * (1.0 - split * 0.09) - compactSplitUv * (0.28 + split * 0.12));

    float alpha = max(baseSample.a, max(redSample.a, max(greenSample.a, blueSample.a)));
    vec3 color = vec3(redSample.r, greenSample.g, blueSample.b);
    vec3 luma = vec3(dot(color, vec3(0.299, 0.587, 0.114)));
    color = mix(luma, color, 1.0 + split * 0.11);
    float guiMask = max(roundedRectMask(pixel, uThemeGuiRect, uThemeRadii.x), roundedRectMask(pixel, uThemePanelRect, uThemeRadii.y));
    float themeReveal = smootherstep(clamp(totalThemeReveal, 0.0, 1.0)) * guiMask;
    float themeIgnition = smootherstep(clamp(totalThemeIgnition, 0.0, 1.0)) * guiMask;
    float themeMute = clamp(totalThemeMute, 0.0, 0.18) * guiMask;
    vec3 themeColor = totalThemeColorWeight > 0.0001 ? totalThemeColor / max(totalThemeColorWeight, 0.0001) : vec3(0.72, 0.82, 1.0);
    color = oklab_mix_srgb(color, luma, themeMute);
    vec3 themeScreen = 1.0 - (1.0 - color) * (1.0 - themeColor * (0.22 + themeIgnition * 0.46));
    vec3 themeLift = oklab_mix_srgb(color, themeScreen, clamp(themeReveal * 0.30 + themeIgnition * 0.22, 0.0, 0.52));
    color = oklab_mix_srgb(color, themeLift, clamp(themeReveal * 0.50 + themeIgnition * 0.31, 0.0, 0.64));
    color += themeColor * (themeIgnition * 0.13 + themeReveal * 0.026);
    float effectAlpha = max(alpha, clamp(totalThemePresence, 0.0, 1.0));
    float glow = clamp(totalGlow, 0.0, 1.0) * effectAlpha;
    float exposure = clamp(totalExposure, 0.0, 1.0) * effectAlpha;
    vec3 glowColor = totalGlow > 0.0001 ? totalGlowColor / max(totalGlow, 0.0001) : vec3(0.72, 0.82, 1.0);
    vec3 cinematicGlow = oklab_mix_srgb(glowColor, vec3(max(max(glowColor.r, glowColor.g), glowColor.b)), 0.14);
    color += (baseSample.rgb * 0.05 + cinematicGlow * 0.25 + vec3(0.018, 0.022, 0.028)) * glow;
    color += oklab_mix_srgb(vec3(1.0, 0.97, 0.94), cinematicGlow, 0.54) * exposure * 0.078;
    color = oklab_mix_srgb(baseSample.rgb, color, clamp(0.62 + split * 0.19 + glow * 0.19 + themeReveal * 0.12 + themeIgnition * 0.10, 0.0, 1.0));

    fragColor = vec4(clamp(color, 0.0, 1.0), alpha);
}
