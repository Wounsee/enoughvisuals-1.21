#version 330 core
layout(location = 0) out vec4 fragColor;

in vec2 vUv;

uniform sampler2D uSource;
uniform sampler2D uBlurred;
uniform sampler2D uDepth;
uniform vec2 uBlurredTexelSize;
uniform float uBlurResolutionScale;
uniform vec2 uNearFar;
uniform mat4 uInverseProjection;
uniform bool uInverseProjectionValid;
uniform vec2 uThreshold;
uniform vec3 uTintColor;
uniform float uTintStrength;

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

float linearizeDepth(float depthSample, float nearPlane, float farPlane) {
    float ndc = depthSample * 2.0 - 1.0;
    return (2.0 * nearPlane * farPlane) / (farPlane + nearPlane - ndc * (farPlane - nearPlane));
}

void main() {
    vec4 baseColor = texture(uSource, vUv);
    float scale = max(uBlurResolutionScale, 1e-6);
    vec2 normalizedTexel = uBlurredTexelSize * scale;
    vec2 clampedUv = clamp(vUv, normalizedTexel * 0.5, 1.0 - normalizedTexel * 0.5);
    vec3 blurred = texture(uBlurred, clampedUv).rgb;
    float depthSample = texture(uDepth, vUv).r;

    float nearPlane = max(1e-5, uNearFar.x);
    float farPlane = max(nearPlane + 1e-4, uNearFar.y);
    float thresholdMin = min(uThreshold.x, uThreshold.y);
    float thresholdMax = max(uThreshold.x, uThreshold.y);
    float range = max(1e-5, thresholdMax - thresholdMin);

    float viewDistance = linearizeDepth(depthSample, nearPlane, farPlane);
    if (uInverseProjectionValid) {
        vec2 ndc = vUv * 2.0 - 1.0;
        float clipZ = depthSample * 2.0 - 1.0;
        vec4 clip = vec4(ndc, clipZ, 1.0);
        vec4 view = uInverseProjection * clip;
        float w = abs(view.w);
        if (w > 1e-6) {
            vec3 viewPos = view.xyz / w;
            viewDistance = length(viewPos);
        }
    }

    float normalized = clamp((viewDistance - thresholdMin) / range, 0.0, 1.0);
    float fogWeight = normalized * normalized * (3.0 - 2.0 * normalized);

    vec3 finalColor = oklab_mix_srgb(baseColor.rgb, blurred, fogWeight);
    float tintAmount = clamp(uTintStrength, 0.0, 1.0) * fogWeight;
    vec3 tintedColor = oklab_mix_srgb(finalColor, uTintColor, tintAmount);
    fragColor = vec4(tintedColor, baseColor.a);
}
