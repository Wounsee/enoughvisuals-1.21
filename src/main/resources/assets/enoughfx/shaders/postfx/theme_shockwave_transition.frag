#version 330 core

in vec2 vUv;

uniform sampler2D u_textureOld;
uniform sampler2D u_textureNew;
uniform vec2 u_resolution;
uniform float u_time;
uniform float u_progress;
uniform float u_linearProgress;
uniform vec2 u_center;
uniform float u_aspect;
uniform float u_radius;
uniform float u_maxRadius;
uniform vec3 u_accentTop;
uniform vec3 u_accentBottom;

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

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

float smootherstep(float value) {
    float t = saturate(value);
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
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

vec2 safeUv(vec2 uv) {
    return clamp(uv, vec2(0.0007), vec2(0.9993));
}

vec4 sampleScreen(sampler2D source, vec2 uv) {
    return texture(source, safeUv(uv));
}

vec3 sampleChromatic(sampler2D source, vec2 uv, vec2 splitUv) {
    float r = sampleScreen(source, uv + splitUv).r;
    float g = sampleScreen(source, uv).g;
    float b = sampleScreen(source, uv - splitUv).b;
    return vec3(r, g, b);
}

void main() {
    float p = saturate(u_progress);
    float lp = saturate(u_linearProgress);
    vec2 resolution = max(u_resolution, vec2(1.0));
    float aspect = max(u_aspect, 0.001);
    vec2 centerUv = vec2(u_center.x / resolution.x, 1.0 - u_center.y / resolution.y);
    vec2 aspectDelta = vec2((vUv.x - centerUv.x) * aspect, vUv.y - centerUv.y);
    float dist = length(aspectDelta);
    vec2 dirAspect = dist > 0.00001 ? aspectDelta / dist : vec2(0.0, 1.0);
    vec2 dirUv = normalize(vec2(dirAspect.x / aspect, dirAspect.y));
    vec2 tangentUv = vec2(-dirUv.y, dirUv.x);

    float maxRadius = max(u_maxRadius, 0.001);
    float radius = max(u_radius, 0.0);
    float band = mix(0.145, 0.052, smootherstep(p));
    float signedDistance = dist - radius;
    float normalizedBand = signedDistance / max(band, 0.0001);
    float absBand = abs(normalizedBand);

    float angle = atan(dirAspect.y, dirAspect.x);
    float stableNoise = valueNoise(vUv * resolution * 0.0065 + vec2(11.7, 4.3));
    float liquid = sin(angle * 5.0 + stableNoise * 2.2 + u_time * 1.55) * 0.5 + 0.5;
    float organicOffset = (stableNoise - 0.5) * band * 0.22 + (liquid - 0.5) * band * 0.10;

    float revealWidth = band * mix(1.22, 0.72, p);
    float reveal = 1.0 - smootherstep((signedDistance + organicOffset + revealWidth * 0.34) / max(revealWidth * 1.42, 0.0001));
    float completeReveal = smootherstep((radius - maxRadius + band * 0.20) / max(band * 2.20, 0.0001));
    reveal = max(saturate(reveal), completeReveal);

    float birth = smootherstep(lp * 7.0);
    float endFade = 1.0 - smootherstep((lp - 0.76) / 0.24);
    float energy = birth * endFade;
    float core = exp(-absBand * absBand * 1.95) * energy;
    float softShell = exp(-absBand * absBand * 0.30) * energy;
    float innerShell = exp(-(normalizedBand + 0.58) * (normalizedBand + 0.58) * 5.2) * energy;
    float outerShell = exp(-(normalizedBand - 0.72) * (normalizedBand - 0.72) * 3.1) * energy;
    float glassRidge = exp(-(normalizedBand - 0.18) * (normalizedBand - 0.18) * 18.0) * energy;
    float fresnel = pow(saturate(1.0 - absBand * 0.82), 2.05) * energy;

    float compression = sin(normalizedBand * 4.2 + liquid * 1.6) * core;
    vec2 lensUv = dirUv * (core * 0.0072 + softShell * 0.0024 + compression * 0.0022);
    vec2 shearUv = tangentUv * (liquid - 0.5) * core * 0.0028;
    vec2 warpUv = lensUv + shearUv;
    vec2 splitUv = dirUv * (core * 0.00125 + outerShell * 0.00062);

    vec4 oldBase = sampleScreen(u_textureOld, vUv + warpUv * 0.55);
    vec4 newBase = sampleScreen(u_textureNew, vUv - warpUv * 0.72);
    vec3 oldRgb = sampleChromatic(u_textureOld, vUv + warpUv * 0.48, splitUv * 0.48);
    vec3 newRgb = sampleChromatic(u_textureNew, vUv - warpUv * 0.82, -splitUv * 0.72);
    vec3 color = oklab_mix_srgb(oldRgb, newRgb, reveal);
    float alpha = mix(oldBase.a, newBase.a, reveal);

    vec3 accent = oklab_mix_srgb(u_accentBottom, u_accentTop, saturate(0.52 + dirAspect.y * 0.38 + (liquid - 0.5) * 0.24));
    vec3 pearl = oklab_mix_srgb(accent, vec3(0.92, 0.82, 1.0), 0.16 + stableNoise * 0.10);
    vec3 hotAccent = oklab_mix_srgb(accent, vec3(1.0), 0.18 + fresnel * 0.24);
    float caustic = pow(liquid, 3.6) * (outerShell * 0.48 + glassRidge * 0.30 + core * 0.10) * (1.0 - smootherstep((lp - 0.82) / 0.18));
    float rim = saturate(fresnel * 0.72 + outerShell * 0.42 + innerShell * 0.28 + glassRidge * 0.20 + caustic * 0.32);
    vec3 screened = 1.0 - (1.0 - color) * (1.0 - hotAccent * (rim * 0.36));
    color = oklab_mix_srgb(color, screened, saturate(rim * 0.72));
    color += hotAccent * (fresnel * 0.055 + outerShell * 0.033 + innerShell * 0.021 + glassRidge * 0.026);
    color += pearl * caustic * 0.075;

    float photographicLift = (core * 0.034 + fresnel * 0.046 + outerShell * 0.018 + caustic * 0.026) * (1.0 - p * 0.22);
    color += oklab_mix_srgb(vec3(1.0, 0.965, 0.90), hotAccent, 0.54) * photographicLift;

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
