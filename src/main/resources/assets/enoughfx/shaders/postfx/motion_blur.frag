#version 330 core

layout(location = 0) out vec4 fragColor;

uniform sampler2D u_ScreenTexture;
uniform sampler2D u_DepthTexture;
uniform vec2 u_Resolution;
uniform mat4 u_InverseProjectionMatrix;
uniform mat4 u_InverseViewMatrix;
uniform mat4 u_PreviousProjectionMatrix;
uniform mat4 u_PreviousViewMatrix;
uniform vec3 u_CameraPos;
uniform vec3 u_PreviousCameraPos;
uniform float u_Strength;
uniform float u_TemporalScale;
uniform float u_MaxRadius;
uniform float u_EdgeFocus;
uniform float u_ChromaticPhase;
uniform float u_DepthGuard;
uniform float u_Decay;
uniform float u_Activation;
uniform float u_GlobalMotion;
uniform vec2 u_CameraVelocity;
uniform int u_Samples;

const int MAX_SAMPLES = 12;

float saturate(float v) {
    return clamp(v, 0.0, 1.0);
}

vec2 safeUv(vec2 uv) {
    return clamp(uv, vec2(0.0005), vec2(0.9995));
}

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

vec3 reconstructWorldPos(vec2 uv, float depth) {
    vec4 clipSpace = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewSpace = u_InverseProjectionMatrix * clipSpace;
    float invW = abs(viewSpace.w) > 0.000001 ? 1.0 / viewSpace.w : 1.0;
    viewSpace *= invW;
    vec4 worldSpace = u_InverseViewMatrix * viewSpace;
    return worldSpace.xyz;
}

vec2 previousUv(vec3 worldPos, vec2 fallbackUv, out float valid) {
    vec3 relative = worldPos - u_PreviousCameraPos;
    vec4 view = u_PreviousViewMatrix * vec4(relative, 1.0);
    vec4 clip = u_PreviousProjectionMatrix * view;
    if (abs(clip.w) <= 0.000001) {
        valid = 0.0;
        return fallbackUv;
    }
    vec2 ndc = clip.xy / clip.w;
    vec2 uv = ndc * 0.5 + 0.5;
    vec2 gate = step(vec2(-0.18), uv) * step(uv, vec2(1.18));
    valid = gate.x * gate.y * step(0.0, clip.w);
    return uv;
}

float structuralMask(vec2 uv, float centerDepth, vec3 centerColor) {
    vec2 px = 1.0 / max(u_Resolution, vec2(1.0));
    vec3 cL = texture(u_ScreenTexture, safeUv(uv - vec2(px.x, 0.0))).rgb;
    vec3 cR = texture(u_ScreenTexture, safeUv(uv + vec2(px.x, 0.0))).rgb;
    vec3 cD = texture(u_ScreenTexture, safeUv(uv - vec2(0.0, px.y))).rgb;
    vec3 cU = texture(u_ScreenTexture, safeUv(uv + vec2(0.0, px.y))).rgb;
    float lumaGrad = abs(luminance(cR) - luminance(cL)) + abs(luminance(cU) - luminance(cD));
    float localContrast = abs(luminance(centerColor) - (luminance(cL) + luminance(cR) + luminance(cD) + luminance(cU)) * 0.25);
    float dL = texture(u_DepthTexture, safeUv(uv - vec2(px.x, 0.0))).r;
    float dR = texture(u_DepthTexture, safeUv(uv + vec2(px.x, 0.0))).r;
    float dD = texture(u_DepthTexture, safeUv(uv - vec2(0.0, px.y))).r;
    float dU = texture(u_DepthTexture, safeUv(uv + vec2(0.0, px.y))).r;
    float depthGrad = abs(dR - dL) + abs(dU - dD);
    float validDepth = 1.0 - step(0.999999, centerDepth);
    float edge = lumaGrad * 1.28 + localContrast * 1.85 + depthGrad * mix(18.0, 46.0, u_DepthGuard) * validDepth;
    float threshold = mix(0.105, 0.018, saturate(u_EdgeFocus / 1.6));
    return smoothstep(threshold, threshold * 3.8 + 0.0001, edge);
}

float depthWeight(vec2 uv, float centerDepth) {
    float sampleDepth = texture(u_DepthTexture, safeUv(uv)).r;
    float valid = 1.0 - step(0.999999, centerDepth);
    float diff = abs(sampleDepth - centerDepth);
    float reject = smoothstep(0.004, 0.052, diff) * valid;
    return mix(1.0, 1.0 - reject * 0.86, u_DepthGuard);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(u_Resolution, vec2(1.0));
    vec4 original = texture(u_ScreenTexture, uv);
    float centerDepth = texture(u_DepthTexture, uv).r;
    float reconstructionDepth = min(centerDepth, 0.999999);
    vec3 worldPos = reconstructWorldPos(uv, reconstructionDepth);
    float prevValid = 0.0;
    vec2 prevUv = previousUv(worldPos, uv, prevValid);
    vec2 reprojectedVelocity = (uv - prevUv) * u_TemporalScale * prevValid;
    vec2 centered = uv * 2.0 - 1.0;
    vec2 periphery = centered * dot(u_CameraVelocity, centered) * 0.34;
    vec2 fallbackVelocity = (u_CameraVelocity + periphery) * u_TemporalScale;
    float reprojectedPx = length(reprojectedVelocity * u_Resolution);
    vec2 velocity = mix(fallbackVelocity, reprojectedVelocity, smoothstep(0.45, 3.0, reprojectedPx) * prevValid);
    vec2 velocityPx = velocity * u_Resolution;
    float speedPx = length(velocityPx);
    if (speedPx > u_MaxRadius) {
        velocity *= u_MaxRadius / max(speedPx, 0.0001);
        speedPx = u_MaxRadius;
    }
    float activationPx = max(0.03, u_Activation);
    float speedMask = smoothstep(activationPx, activationPx * 4.0 + 0.0001, speedPx);
    float edge = structuralMask(uv, centerDepth, original.rgb);
    float motionFill = smoothstep(0.55, 10.0, speedPx);
    float centerGuard = 1.0 - smoothstep(0.0, 0.46, length(centered));
    float modeMask = max(edge * 0.92, motionFill * mix(0.42, 0.74, 1.0 - centerGuard));
    float globalGate = smoothstep(0.0005, 0.045, u_GlobalMotion);
    float mask = saturate(speedMask * modeMask * mix(0.90, 1.0, globalGate) * u_Strength);
    if (mask <= 0.001) {
        fragColor = original;
        return;
    }
    int samples = clamp(u_Samples, 3, MAX_SAMPLES);
    float chroma = u_ChromaticPhase;
    vec3 acc = vec3(0.0);
    vec3 maxHold = original.rgb;
    float weightSum = 0.0;
    for (int i = 0; i < MAX_SAMPLES; i++) {
        if (i >= samples) {
            break;
        }
        float t = float(i) / float(max(samples - 1, 1));
        float weight = exp(-t * u_Decay);
        vec2 baseOffset = velocity * t;
        vec2 uvR = safeUv(uv - baseOffset * mix(1.0, 0.76, chroma));
        vec2 uvG = safeUv(uv - baseOffset);
        vec2 uvB = safeUv(uv - baseOffset * mix(1.0, 1.32, chroma));
        float depthMix = depthWeight(uvG, centerDepth);
        vec3 sampleColor = vec3(
                texture(u_ScreenTexture, uvR).r,
                texture(u_ScreenTexture, uvG).g,
                texture(u_ScreenTexture, uvB).b
        );
        float w = weight * depthMix;
        acc += sampleColor * w;
        weightSum += w;
        maxHold = max(maxHold, sampleColor * (0.92 + 0.08 * (1.0 - t)));
    }
    vec3 trail = acc / max(weightSum, 0.0001);
    float brightProtect = smoothstep(0.72, 1.0, luminance(original.rgb));
    vec3 persistent = mix(trail, maxHold, 0.18 + brightProtect * 0.10);
    vec3 color = mix(original.rgb, persistent, saturate(mask));
    float subpixelGlow = smoothstep(0.8, 10.0, speedPx) * max(edge, motionFill * 0.32) * chroma * 0.055;
    color += max(persistent - original.rgb, vec3(0.0)) * subpixelGlow;
    fragColor = vec4(clamp(color, 0.0, 1.0), original.a);
}
