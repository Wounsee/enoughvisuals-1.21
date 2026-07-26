#version 330 core

layout(location = 0) out vec4 fragColor;

uniform sampler2D u_DepthTexture;
uniform sampler2D u_ScreenTexture;
uniform mat4 u_InverseProjectionMatrix;
uniform mat4 u_InverseViewMatrix;
uniform vec3 u_CameraPos;
uniform vec2 u_Resolution;
uniform vec3 u_AtmosphereTint;
uniform vec3 u_SkyColor;
uniform float u_FogDensity;
uniform float u_HorizonDissolve;
uniform float u_SkyLift;
uniform float u_EdgeSoftness;

float saturate(float v) {
    return clamp(v, 0.0, 1.0);
}

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash13(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash13(i + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y), mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y), f.z);
}

vec3 getLinealWorldPos(vec2 uv, float depth) {
    vec4 clipSpace = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewSpace = u_InverseProjectionMatrix * clipSpace;
    float invW = abs(viewSpace.w) > 0.000001 ? 1.0 / viewSpace.w : 1.0;
    viewSpace *= invW;
    vec4 worldSpace = u_InverseViewMatrix * viewSpace;
    return worldSpace.xyz;
}

vec3 getWorldRay(vec2 uv) {
    vec3 farPos = getLinealWorldPos(uv, 1.0);
    return normalize(farPos - u_CameraPos + vec3(0.0, 0.0, 0.000001));
}

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

vec3 dawnPalette(vec3 ray, float horizon, float lowSky) {
    vec3 deepBlue = u_AtmosphereTint * vec3(0.50, 0.58, 0.86) + vec3(0.012, 0.016, 0.030);
    vec3 fogBlue = u_AtmosphereTint * vec3(0.78, 0.84, 1.05) + vec3(0.018, 0.018, 0.030);
    vec3 lavender = vec3(0.68, 0.56, 0.84);
    vec3 rose = vec3(0.98, 0.62, 0.72);
    vec3 amber = vec3(1.0, 0.55, 0.30);
    vec3 sunDir = normalize(vec3(-0.72, -0.10, 0.26));
    float sunGlow = pow(saturate(dot(ray, sunDir) * 0.5 + 0.5), 5.0) * horizon;
    vec3 color = mix(deepBlue, fogBlue, smoothstep(0.0, 0.70, horizon));
    color = mix(color, lavender, horizon * 0.32);
    color = mix(color, rose, horizon * lowSky * 0.48);
    color = mix(color, amber, sunGlow * 0.46);
    return color;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(u_Resolution, vec2(1.0));
    float depth = texture(u_DepthTexture, uv).r;
    vec3 original = texture(u_ScreenTexture, uv).rgb;
    vec3 ray = getWorldRay(uv);
    float validDepth = 1.0 - step(0.999999, depth);
    vec3 worldPos = getLinealWorldPos(uv, min(depth, 0.999999));
    float distanceToPixel = length(worldPos - u_CameraPos);
    float cameraDistance = mix(190.0, distanceToPixel, validDepth);
    float horizon = pow(saturate(1.0 - abs(ray.y) * 1.12), 1.36);
    float lowSky = saturate(1.0 - ray.y * 2.20);
    float density = max(u_FogDensity, 0.0);
    float lowWorld = exp(-max(worldPos.y - 58.0, 0.0) / 18.5);
    float heightFog = mix(0.25, 1.42, saturate(lowWorld));
    float highFade = 1.0 - smoothstep(96.0, 148.0, worldPos.y);
    float nearFade = smoothstep(3.0, 14.0, cameraDistance);
    float depthGradient = length(vec2(dFdx(depth), dFdy(depth)));
    float edgeSoftness = clamp(u_EdgeSoftness, 0.0, 1.0);
    float seam = smoothstep(0.0007, mix(0.010, 0.034, edgeSoftness), depthGradient) * horizon;
    float shapeNoise = noise3(worldPos * 0.026 + vec3(0.0, u_CameraPos.y * 0.003, 0.0));
    float densityMod = mix(0.82, 1.10, shapeNoise);
    float opticalDepth = density * cameraDistance * (0.34 + heightFog * 1.16) * (0.76 + horizon * 0.72) * densityMod;
    float terrainFog = (1.0 - exp(-opticalDepth)) * highFade * nearFade;
    float skyFog = saturate((0.18 + horizon * 0.74 + lowSky * 0.16) * density * 7.2);
    float farField = smoothstep(92.0, 238.0, cameraDistance) * validDepth;
    float horizonGate = smoothstep(0.18, 0.86, horizon);
    float verticalGate = smoothstep(-0.34, 0.28 + u_SkyLift * 0.42, ray.y + 0.12);
    float dissolveNoise = mix(0.82, 1.18, noise3(worldPos * 0.018 + vec3(u_CameraPos.xz * 0.0016, u_CameraPos.y * 0.002).xzy));
    float voidDissolve = saturate(u_HorizonDissolve * farField * horizonGate * (0.42 + verticalGate * 0.58) * dissolveNoise);
    float seamDissolve = saturate(seam * (0.35 + edgeSoftness * 0.70) * (0.45 + u_HorizonDissolve * 0.80));
    terrainFog = saturate(mix(terrainFog, max(terrainFog, skyFog), voidDissolve * (0.58 + u_SkyLift * 0.36)));
    float fogPower = saturate(max(terrainFog * validDepth, skyFog * (1.0 - validDepth)) + seamDissolve * 0.38 + voidDissolve * 0.22);
    vec3 fogColor = dawnPalette(ray, horizon, lowSky);
    vec3 liftedSky = mix(fogColor, u_SkyColor * vec3(0.92, 1.02, 1.18) + u_AtmosphereTint * 0.18, saturate(u_SkyLift * horizonGate));
    fogColor = mix(fogColor, liftedSky, saturate(voidDissolve + seamDissolve * 0.42));
    float highlightGuard = smoothstep(0.78, 1.0, luminance(original));
    vec3 cooled = original * mix(vec3(1.0), vec3(0.82, 0.89, 1.03), saturate(fogPower * 0.28));
    float assimilation = saturate(fogPower * (0.88 - highlightGuard * 0.15) + voidDissolve * (0.26 + u_SkyLift * 0.18) + seamDissolve * 0.16);
    vec3 color = mix(cooled, fogColor, assimilation);
    color += liftedSky * horizon * density * (0.34 + voidDissolve * 0.42) * (1.0 - validDepth * 0.20);
    color += vec3(1.0, 0.56, 0.32) * pow(horizon, 3.3) * density * (0.30 + voidDissolve * 0.12);
    color = mix(color, liftedSky, saturate(voidDissolve * 0.22 + seamDissolve * 0.10));
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
