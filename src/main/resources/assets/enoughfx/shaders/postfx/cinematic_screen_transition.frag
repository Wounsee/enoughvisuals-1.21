#version 330 core

in vec2 vUv;

uniform sampler2D uOldScreen;
uniform sampler2D uNewScreen;
uniform sampler2D uBlurredScreen;
uniform vec2 uResolution;
uniform float uProgress;
uniform float uLinearProgress;
uniform float uAlpha;
uniform float uScale;
uniform float uBlurMix;
uniform float uExposure;
uniform float uTime;

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

vec2 clampUv(vec2 uv) {
    return clamp(uv, vec2(0.0008), vec2(0.9992));
}

vec2 scaledAround(vec2 uv, float scale) {
    return clampUv((uv - 0.5) / max(scale, 0.0001) + 0.5);
}

void main() {
    float p     = clamp(uProgress, 0.0, 1.0);
    float alpha = clamp(uAlpha, 0.0, 1.0);
    float bMix  = clamp(uBlurMix, 0.0, 1.0);

    float oldScale = mix(1.000, 0.985, p);
    vec2  oldUv    = scaledAround(vUv, oldScale);
    vec3  oldSharp   = texture(uOldScreen,     oldUv).rgb;
    vec3  oldBlurred = texture(uBlurredScreen, oldUv).rgb;
    vec3  oldColor   = mix(oldSharp, oldBlurred, bMix);
    oldColor *= mix(1.0, 0.88, p);

    vec2 newUv = scaledAround(vUv, uScale);
    vec3 newColor = texture(uNewScreen, newUv).rgb;

    vec3 color = oklab_mix_srgb(oldColor, newColor, alpha);

    vec2  c       = vUv - 0.5;
    float aspect  = uResolution.x / max(uResolution.y, 1.0);
    vec2  optical = vec2(c.x * aspect, c.y);
    float radial  = length(optical);
    float vigEdge = smoothstep(0.55, 1.05, radial);
    float vigAmt  = 4.0 * p * (1.0 - p) * 0.06;
    color *= 1.0 - vigEdge * vigAmt;

    fragColor = vec4(max(color, vec3(0.0)), 1.0);
}
