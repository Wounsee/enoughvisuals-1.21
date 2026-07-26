#version 330 core

layout(location = 0) out vec4 fragColor;

uniform float u_Time;
uniform float u_WindSpeed;
uniform vec3 u_CameraPos;
uniform vec2 u_Resolution;
uniform vec3 u_AtmosphereTint;
uniform sampler2D u_ScreenTexture;
uniform sampler2D u_DepthTexture;
uniform mat4 u_InverseProjectionMatrix;
uniform mat4 u_InverseViewMatrix;

const float TAU = 6.28318530718;

float saturate(float v) {
    return clamp(v, 0.0, 1.0);
}

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 hash33(vec3 p) {
    return vec3(hash13(p + 17.17), hash13(p + 43.31), hash13(p + 91.67));
}

vec2 rotate2(vec2 p, float a) {
    float s = sin(a);
    float c = cos(a);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
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

float airBand(vec3 p) {
    float worldLayer = smoothstep(59.0, 66.0, p.y) * (1.0 - smoothstep(104.0, 138.0, p.y));
    float cameraLayer = smoothstep(u_CameraPos.y + 1.7, u_CameraPos.y + 6.0, p.y) * (1.0 - smoothstep(u_CameraPos.y + 18.0, u_CameraPos.y + 33.0, p.y));
    return saturate(max(worldLayer * 0.62, cameraLayer));
}

float softRibbon(vec3 p, float seed, float speed) {
    vec2 windDir = normalize(vec2(cos(seed), sin(seed)));
    vec2 xz = vec2(dot(p.xz, windDir), dot(p.xz, vec2(-windDir.y, windDir.x)));
    float drift = u_Time * (0.020 + speed * 0.060);
    float wave = sin(xz.x * 0.105 + p.y * 0.065 - drift * 3.4 + seed * 5.0);
    wave += sin(xz.x * 0.041 - p.y * 0.092 + drift * 2.2 + seed * 11.0) * 0.55;
    float path = xz.y * 0.096 + wave * 0.46;
    float line = 1.0 - smoothstep(0.020, 0.135, abs(fract(path + 0.5) - 0.5));
    float dash = 0.35 + 0.65 * smoothstep(0.16, 0.76, sin(xz.x * 0.155 - drift * 7.0 + seed * 17.0) * 0.5 + 0.5);
    float veil = 0.62 + 0.38 * sin((p.x + p.z) * 0.027 + p.y * 0.044 + seed * 23.0 + u_Time * 0.030);
    return line * dash * veil;
}

vec3 windVolume(vec3 rayDir, float maxDistance) {
    float speed = clamp(u_WindSpeed, 0.0, 2.0);
    float amount = 0.0;
    for (int i = 0; i < 6; i++) {
        float k = (float(i) + 0.5) / 6.0;
        float t = mix(3.0, maxDistance, k);
        vec3 p = u_CameraPos + rayDir * t;
        float depthFade = smoothstep(2.6, 8.0, t) * (1.0 - smoothstep(maxDistance * 0.70, maxDistance, t));
        float layer = airBand(p);
        float v = softRibbon(p, 0.73, speed) * 0.64;
        v += softRibbon(p + vec3(7.4, 3.6, -5.2), 2.18, speed) * 0.40;
        amount += v * layer * depthFade * (1.0 - smoothstep(64.0, 98.0, t));
    }
    float alpha = saturate(amount * (0.010 + speed * 0.030));
    vec3 color = mix(u_AtmosphereTint * vec3(0.72, 0.84, 1.10), vec3(0.82, 0.93, 1.0), 0.70);
    return color * alpha;
}

vec3 movingPoint(vec3 cell, vec3 rnd, float invScale) {
    float t = u_Time * (0.20 + rnd.z * 0.18);
    vec3 base = (cell + rnd) * invScale;
    vec3 drift = vec3(
        sin(t + rnd.x * TAU) * (0.42 + rnd.y * 0.72),
        sin(t * 0.77 + rnd.z * TAU) * (0.22 + rnd.x * 0.46),
        cos(t * 0.91 + rnd.y * TAU) * (0.42 + rnd.z * 0.72)
    );
    return base + drift;
}

vec2 fireflyAt(vec3 pos, vec3 rayDir, float maxDistance, vec3 rnd, float densityGate) {
    vec3 toPoint = pos - u_CameraPos;
    float along = dot(toPoint, rayDir);
    float inFront = smoothstep(1.8, 4.5, along) * (1.0 - smoothstep(maxDistance - 2.0, maxDistance + 4.0, along));
    vec3 closest = u_CameraPos + rayDir * along;
    float d = length(pos - closest);
    float layer = smoothstep(58.0, 65.0, pos.y) * (1.0 - smoothstep(92.0, 122.0, pos.y));
    float twinkle = 0.55 + 0.45 * pow(0.5 + 0.5 * sin(u_Time * (1.05 + rnd.y * 1.55) + rnd.x * TAU), 2.0);
    float slowFade = 0.68 + 0.32 * sin(u_Time * (0.23 + rnd.z * 0.19) + rnd.y * TAU);
    float radius = 0.085 + rnd.x * 0.060;
    float haloRadius = 0.62 + rnd.z * 0.78;
    float core = exp(-d * d / max(radius * radius, 0.0001)) * 0.18;
    float halo = exp(-d * d / max(haloRadius * haloRadius, 0.0001)) * 0.20;
    float distanceFade = 1.0 - smoothstep(38.0, 82.0, along);
    float visibility = layer * inFront * twinkle * slowFade * distanceFade * densityGate;
    return vec2(core * visibility, halo * visibility);
}

vec3 fireflyField(vec3 rayDir, float maxDistance, vec2 uv) {
    float jitter = hash12(gl_FragCoord.xy + floor(u_Time * 0.15));
    float coreEnergy = 0.0;
    float haloEnergy = 0.0;
    for (int i = 0; i < 8; i++) {
        float k = (float(i) + jitter) / 8.0;
        float t = mix(4.0, maxDistance, k);
        vec3 probe = u_CameraPos + rayDir * t;
        float scaleA = 0.105;
        vec3 cellA = floor(probe * scaleA);
        vec3 rndA = hash33(cellA + vec3(4.7, 19.1, 37.3));
        float gateA = smoothstep(0.78, 0.98, rndA.x);
        vec2 eA = fireflyAt(movingPoint(cellA, rndA, 1.0 / scaleA), rayDir, maxDistance, rndA, gateA);
        coreEnergy += eA.x;
        haloEnergy += eA.y;
        float scaleB = 0.061;
        vec3 cellB = floor((probe + vec3(11.3, -2.1, 7.8)) * scaleB);
        vec3 rndB = hash33(cellB + vec3(28.9, 7.4, 61.2));
        float gateB = smoothstep(0.88, 0.995, rndB.y);
        vec2 eB = fireflyAt(movingPoint(cellB, rndB, 1.0 / scaleB), rayDir, maxDistance, rndB, gateB);
        coreEnergy += eB.x * 0.64;
        haloEnergy += eB.y * 0.72;
    }
    coreEnergy = saturate(coreEnergy * 0.92);
    haloEnergy = saturate(haloEnergy * 0.82);
    vec3 haloColor = vec3(1.0, 0.62, 0.18);
    vec3 coreColor = vec3(1.0, 0.86, 0.48);
    return haloColor * haloEnergy + coreColor * coreEnergy;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(u_Resolution, vec2(1.0));
    float depth = texture(u_DepthTexture, uv).r;
    vec3 screenColor = texture(u_ScreenTexture, uv).rgb;
    vec3 rayDir = getWorldRay(uv);
    vec3 surfacePos = getLinealWorldPos(uv, min(depth, 0.999999));
    float sceneDistance = length(surfacePos - u_CameraPos);
    float validDepth = 1.0 - step(0.999999, depth);
    float maxDistance = mix(78.0, min(sceneDistance, 78.0), validDepth);
    maxDistance = max(maxDistance, 7.0);
    vec3 color = screenColor + windVolume(rayDir, maxDistance) + fireflyField(rayDir, maxDistance, uv);
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
