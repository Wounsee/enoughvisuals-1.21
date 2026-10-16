#version 150

// Outer glow of the captured entity mask, ported from RivalVisuals' ShaderPlayer.
// glowSampler holds the Kawase-blurred mask: a = coverage, b = nearest entity depth.

uniform sampler2D glowSampler;
uniform sampler2D entitySampler;
uniform sampler2D sceneDepthSampler;
uniform sampler2D entityDepthSampler;

uniform vec4 primaryColor;
uniform vec4 secondaryColor;
uniform int gradient;
uniform float time;
uniform float glowAlphaMult;
uniform float intensity;
uniform int glowEnabled;
uniform int thinOutline;
uniform vec2 uTexelSize;

in vec2 texCoord;
out vec4 fragColor;

const float ALPHA_EPSILON = 0.001;
const float DEPTH_EPSILON = 0.00015;
const float TAU = 6.28318530718;

vec3 resolveColor(vec2 uv) {
    if (gradient == 0) return primaryColor.rgb;
    float phase = sin(time * 0.9) * 0.5 + 0.5;
    float t = clamp(uv.x * 0.6 + uv.y * 0.4 + (phase - 0.5) * 0.35, 0.0, 1.0);
    return mix(primaryColor.rgb, secondaryColor.rgb, t);
}

bool isOccluded(vec2 uv, float targetDepth) {
    float farDepth = smoothstep(0.78, 0.995, targetDepth);
    float depthTolerance = mix(DEPTH_EPSILON, 0.00024, farDepth);
    return texture(sceneDepthSampler, uv).r + depthTolerance < targetDepth;
}

void main() {
    // The model itself stays untouched: only pixels around it are lit.
    if (texture(entitySampler, texCoord).a > ALPHA_EPSILON) {
        discard;
    }

    if (thinOutline == 1) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x == 0 && y == 0) continue;
                vec2 sampleUv = texCoord + vec2(float(x), float(y)) * uTexelSize;
                float sampleAlpha = texture(entitySampler, sampleUv).a;
                if (sampleAlpha > ALPHA_EPSILON) {
                    if (isOccluded(texCoord, texture(entityDepthSampler, sampleUv).r)) discard;
                    fragColor = vec4(resolveColor(sampleUv), clamp(primaryColor.a * sampleAlpha, 0.0, 1.0));
                    return;
                }
            }
        }
    }

    if (glowEnabled == 0) discard;

    vec4 glowData = texture(glowSampler, texCoord);
    float blurAlpha = glowData.a;
    if (blurAlpha <= ALPHA_EPSILON) discard;
    if (isOccluded(texCoord, glowData.b)) discard;

    float alpha = clamp(blurAlpha * 1.6, 0.0, 1.0) * glowAlphaMult * clamp(intensity, 0.0, 1.5);
    if (alpha < 0.002) discard;
    fragColor = vec4(resolveColor(texCoord), clamp(alpha, 0.0, 1.0));
}
