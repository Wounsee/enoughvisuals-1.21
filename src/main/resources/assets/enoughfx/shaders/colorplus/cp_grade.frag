#version 330 core

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uScene;
uniform sampler2D uBloomTex;
uniform vec2  uResolution;

uniform float uStrength;
uniform float uExposure;
uniform float uContrast;
uniform float uSaturation;
uniform float uVibrance;
uniform float uGamma;
uniform float uTemperature;
uniform float uTint;
uniform vec3  uLift;
uniform vec3  uGammaRgb;
uniform vec3  uGain;
uniform float uBloomIntensity;
uniform float uBloomThreshold;
uniform float uBloomRadius;
uniform float uSharpness;
uniform float uVignette;
uniform float uFlipY;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

vec2 sceneUv(vec2 uv) {
    return mix(uv, vec2(uv.x, 1.0 - uv.y), uFlipY);
}

vec3 sampleScene(vec2 uv) {
    return texture(uScene, sceneUv(uv)).rgb;
}

vec3 acesFilm(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

vec3 toLinear(vec3 c) {
    return pow(max(c, vec3(0.0)), vec3(2.2));
}

vec3 toGamma(vec3 c) {
    return pow(max(c, vec3(0.0)), vec3(1.0 / 2.2));
}

vec3 applyTemp(vec3 c, float k) {
    vec3 warm = vec3(1.06, 0.98, 0.86);
    vec3 cool = vec3(0.86, 0.96, 1.08);
    vec3 tint = mix(vec3(1.0), mix(cool, warm, step(0.0, k)), abs(k));
    return c * tint;
}

vec3 applyTint(vec3 c, float t) {
    vec3 magenta = vec3(1.05, 0.94, 1.05);
    vec3 green = vec3(0.94, 1.06, 0.94);
    vec3 tint = mix(vec3(1.0), mix(green, magenta, step(0.0, t)), abs(t));
    return c * tint;
}

vec3 applySaturation(vec3 c, float s) {
    float l = dot(c, LUMA);
    return mix(vec3(l), c, 1.0 + s);
}

vec3 applyVibrance(vec3 c, float v) {
    float maxC = max(c.r, max(c.g, c.b));
    float minC = min(c.r, min(c.g, c.b));
    float sat = maxC - minC;
    float l = dot(c, LUMA);
    float weight = (1.0 - clamp(sat, 0.0, 1.0));
    return mix(vec3(l), c, 1.0 + v * weight * 1.4);
}

vec3 liftGammaGain(vec3 c, vec3 lift, vec3 gamma, vec3 gain) {
    c = c * gain;
    c = c + lift * (1.0 - c);
    c = pow(max(c, vec3(0.0)), vec3(1.0) / max(gamma, vec3(0.05)));
    return c;
}

vec3 applyContrast(vec3 c, float k) {
    float t = 1.0 + k * 0.85;
    return clamp((c - 0.5) * t + 0.5, 0.0, 16.0);
}

vec3 sampleBloom(vec2 uv) {
    return texture(uBloomTex, sceneUv(uv)).rgb;
}

vec3 casSharpen(vec2 uv, vec3 center, float amount) {
    vec2 texel = 1.0 / max(uResolution, vec2(1.0));
    vec3 a = sampleScene(uv + vec2(0.0, -texel.y));
    vec3 b = sampleScene(uv + vec2(-texel.x, 0.0));
    vec3 c = sampleScene(uv + vec2( texel.x, 0.0));
    vec3 d = sampleScene(uv + vec2(0.0,  texel.y));
    vec3 mn = min(min(a, b), min(c, d));
    vec3 mx = max(max(a, b), max(c, d));
    vec3 d_min = mn;
    vec3 d_max = vec3(1.0) - mx;
    vec3 ws = sqrt(clamp(min(d_min, d_max) / max(mx, vec3(1e-4)), 0.0, 1.0));
    vec3 average = (a + b + c + d) * 0.25;
    float maxC = max(center.r, max(center.g, center.b));
    float minC = min(center.r, min(center.g, center.b));
    float chroma = maxC - minC;
    float lum = dot(center, LUMA);
    float neutralHighlight = smoothstep(0.70, 0.98, lum) * (1.0 - smoothstep(0.045, 0.20, chroma));
    vec3 sharp = (center - average) * ws * amount * (1.0 - neutralHighlight * 0.94) * 1.6;
    return center + sharp;
}

void main() {
    vec2 uv = vUv;
    vec3 orig = sampleScene(uv);
    vec3 col = orig;

    if (uSharpness > 0.001) {
        col = casSharpen(uv, col, uSharpness);
    }

    vec3 lin = toLinear(col);
    lin *= pow(2.0, uExposure);
    lin = liftGammaGain(lin,
                        uLift * 0.18,
                        vec3(1.0) + uGammaRgb * 0.6,
                        vec3(1.0) + uGain * 0.6);
    lin = applyTemp(lin, uTemperature * 0.8);
    lin = applyTint(lin, uTint * 0.6);
    vec3 mapped = acesFilm(lin);
    col = toGamma(mapped);

    if (uBloomIntensity > 0.001) {
        vec3 bloom = sampleBloom(uv);
        bloom = toGamma(bloom);
        float maxC = max(orig.r, max(orig.g, orig.b));
        float minC = min(orig.r, min(orig.g, orig.b));
        float chroma = maxC - minC;
        float neutralHighlight = smoothstep(0.70, 1.0, dot(orig, LUMA)) * (1.0 - smoothstep(0.045, 0.22, chroma));
        col = col + bloom * uBloomIntensity * mix(2.4, 0.82, neutralHighlight);
    }

    col = pow(max(col, vec3(0.0)), vec3(1.0 / max(0.05, 1.0 + uGamma * 0.55)));
    col = applySaturation(col, uSaturation);
    col = applyVibrance(col, uVibrance);
    col = applyContrast(col, uContrast);

    if (uVignette > 0.001) {
        vec2 vc = uv - 0.5;
        float vig = 1.0 - dot(vc, vc) * (uVignette * 1.4);
        col *= clamp(vig, 0.55, 1.0);
    }

    col = clamp(col, 0.0, 1.0);
    vec3 outCol = mix(orig, col, clamp(uStrength, 0.0, 1.0));
    fragColor = vec4(outCol, 1.0);
}
