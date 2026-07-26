#version 330 core

layout(location = 0) out vec4 fragColor;

uniform sampler2D u_ScreenTexture;
uniform sampler2D u_DepthTexture;
uniform mat4 u_InverseProjectionMatrix;
uniform mat4 u_InverseViewMatrix;
uniform vec3 u_CameraPos;
uniform vec2 u_Resolution;
uniform vec3 u_AtmosphereTint;
uniform float u_Time;
uniform float u_WindSpeed;
uniform float u_RainIntensity;
uniform vec3 u_WindDirection;

float saturate(float v) {
    return clamp(v, 0.0, 1.0);
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float hash13(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
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

float ign(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}

vec2 safeNormalize(vec2 v, vec2 fallback) {
    float l = length(v);
    return l > 0.0001 ? v / l : fallback;
}

vec3 safeNormalize3(vec3 v, vec3 fallback) {
    float l = length(v);
    return l > 0.0001 ? v / l : fallback;
}

float luminance(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

vec3 getWorldPos(vec2 uv, float depth) {
    vec4 clipSpace = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewSpace = u_InverseProjectionMatrix * clipSpace;
    viewSpace *= abs(viewSpace.w) > 0.000001 ? 1.0 / viewSpace.w : 1.0;
    vec4 worldSpace = u_InverseViewMatrix * viewSpace;
    return worldSpace.xyz;
}

vec3 getWorldRay(vec2 uv) {
    vec4 clipSpace = vec4(uv * 2.0 - 1.0, 1.0, 1.0);
    vec4 viewSpace = u_InverseProjectionMatrix * clipSpace;
    viewSpace *= abs(viewSpace.w) > 0.000001 ? 1.0 / viewSpace.w : 1.0;
    vec4 worldSpace = u_InverseViewMatrix * viewSpace;
    return normalize(worldSpace.xyz - u_CameraPos);
}

vec3 blurScreen(vec2 uv, float radius) {
    vec2 px = radius / max(u_Resolution, vec2(1.0));
    vec3 c = texture(u_ScreenTexture, uv).rgb * 0.50;
    c += texture(u_ScreenTexture, clamp(uv + vec2(px.x, 0.0), vec2(0.001), vec2(0.999))).rgb * 0.125;
    c += texture(u_ScreenTexture, clamp(uv - vec2(px.x, 0.0), vec2(0.001), vec2(0.999))).rgb * 0.125;
    c += texture(u_ScreenTexture, clamp(uv + vec2(0.0, px.y), vec2(0.001), vec2(0.999))).rgb * 0.125;
    c += texture(u_ScreenTexture, clamp(uv - vec2(0.0, px.y), vec2(0.001), vec2(0.999))).rgb * 0.125;
    return c;
}

float altitudeMask(vec3 p) {
    return smoothstep(-58.0, -30.0, p.y) * (1.0 - smoothstep(228.0, 266.0, p.y));
}

float rainVeil(vec3 p, vec3 windDir, vec3 sideDir, float layer, float nearFactor, float strength, inout vec2 lens, inout float gleam, inout float haze) {
    vec3 q = p;
    q -= windDir * u_Time * mix(0.38, 0.88, layer);
    q.y += u_Time * mix(1.35, 2.70, layer);
    float side = dot(q, sideDir);
    float wind = dot(q, windDir);
    vec2 a = vec2(side * mix(0.018, 0.033, layer) + q.y * mix(0.0037, 0.0065, layer), wind * mix(0.012, 0.025, layer) - q.y * mix(0.0020, 0.0040, layer));
    vec2 b = vec2(side * mix(0.058, 0.096, layer) - q.y * mix(0.0027, 0.0048, layer), wind * mix(0.031, 0.056, layer) + q.y * mix(0.0014, 0.0030, layer));
    float n0 = valueNoise(a + vec2(layer * 13.7 + u_Time * 0.018, layer * 8.3 - u_Time * 0.013));
    float n1 = valueNoise(b + vec2(layer * 29.1 - u_Time * 0.015, layer * 17.9 + u_Time * 0.019));
    float fiber = 0.5 + 0.5 * sin(q.y * mix(0.014, 0.026, layer) + side * mix(0.026, 0.047, layer) + n0 * 4.8 + layer * 6.1);
    float silk = pow(smoothstep(0.34, 0.86, n1 * 0.72 + n0 * 0.34), 1.44);
    float sheet = smoothstep(0.26, 0.80, n0 * 0.72 + n1 * 0.30);
    float density = (sheet * (0.58 + fiber * 0.30) + silk * 0.30) * altitudeMask(p) * (0.54 + strength * 0.88) * (0.74 + nearFactor * 0.34) * mix(1.0, 0.68, layer);
    lens += vec2((n0 - 0.5) * 0.019 + (silk - 0.5) * 0.011, -(fiber * 0.55 + n1 * 0.45) * 0.022) * density;
    gleam += (silk * 0.18 + sheet * fiber * 0.08) * (0.12 + nearFactor * 0.22) * altitudeMask(p);
    haze += density * (0.34 + layer * 0.22);
    return density;
}

float strandField(vec3 p, vec3 sideDir, vec3 fallDir, vec3 depthDir, vec3 velocity, float scale, float layer, float nearFactor, float strength, inout vec2 lens, inout float gleam) {
    vec3 q = p - velocity * u_Time;
    float along = dot(q, fallDir);
    float depth = dot(q, depthDir);
    float crossA = dot(q, sideDir) * scale + depth * scale * mix(0.22, 0.46, layer);
    float crossB = dot(q, sideDir) * scale * mix(0.66, 0.86, layer) - depth * scale * mix(0.52, 0.82, layer);
    float warp = valueNoise(vec2(crossA * 0.055 + layer * 11.0, depth * 0.045 + along * 0.012));
    float coord = mix(crossA, crossB, layer * 0.55) + (warp - 0.5) * mix(0.88, 0.52, nearFactor);
    float lane = floor(coord + layer * 37.0);
    float f = fract(coord + layer * 37.0);
    float rnd = hash12(vec2(lane * 0.71 + layer * 19.0, layer * 43.0 + 9.0));
    float rnd2 = hash12(vec2(lane * 1.33 + 17.0, layer * 23.0 + rnd * 7.0));
    float gate = smoothstep(mix(0.96, 0.58, strength), 1.0, rnd2);
    float center = 0.5 + (rnd - 0.5) * mix(0.82, 0.46, nearFactor);
    float width = mix(0.020, 0.052, nearFactor) * mix(1.10, 0.78, layer) * (0.86 + strength * 0.28);
    float lineG = (f - center) / max(width, 0.001);
    float line = exp(-lineG * lineG);
    float phase = along * mix(0.052, 0.090, layer) + rnd * 0.77;
    float segment = floor(phase);
    float local = fract(phase);
    float segRnd = hash12(vec2(segment * 0.63 + lane * 0.27, rnd * 13.0 + layer * 17.0));
    float headAt = mix(0.040, 0.150, segRnd);
    float headG = (local - headAt) / mix(0.046, 0.030, layer);
    float head = exp(-headG * headG);
    float tailEnd = min(0.96, headAt + mix(0.34, 0.72, nearFactor) * mix(0.84, 1.20, segRnd));
    float tail = smoothstep(headAt, headAt + mix(0.055, 0.125, nearFactor), local) * (1.0 - smoothstep(tailEnd, min(0.99, tailEnd + 0.18), local));
    float broken = smoothstep(0.18, 0.88, hash13(vec3(lane * 0.37, segment * 0.61, layer * 5.0)) * 0.72 + valueNoise(vec2(lane * 0.13, along * 0.018 + layer * 3.0)) * 0.28);
    float density = line * max(head * 1.22, tail * 0.72) * gate * (0.62 + broken * 0.38) * altitudeMask(p);
    lens += vec2((rnd - 0.5) * 0.010, -line * (0.005 + nearFactor * 0.010)) * density;
    gleam += line * gate * (head * 0.84 + tail * 0.26) * (0.16 + nearFactor * 0.42);
    return density * (0.78 + nearFactor * 0.66);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(u_Resolution, vec2(1.0));
    vec3 screenColor = texture(u_ScreenTexture, uv).rgb;
    float rawIntensity = saturate(u_RainIntensity);
    if (rawIntensity <= 0.0001) {
        fragColor = vec4(screenColor, 1.0);
        return;
    }

    float strength = pow(1.0 - exp(-rawIntensity * 2.15), 0.86);
    float depth = texture(u_DepthTexture, uv).r;
    float hasDepth = 1.0 - step(0.999999, depth);
    vec3 rayDir = getWorldRay(uv);
    vec3 surfacePos = getWorldPos(uv, min(depth, 0.999999));
    float sceneDistance = length(surfacePos - u_CameraPos);
    float geometryDistance = hasDepth > 0.5 ? sceneDistance : 100000.0;
    if (hasDepth > 0.5 && geometryDistance <= 0.78) {
        fragColor = vec4(screenColor, 1.0);
        return;
    }

    vec2 wind2 = safeNormalize(u_WindDirection.xz, normalize(vec2(0.819, 0.574)));
    float windPower = 0.10 + clamp(u_WindSpeed, 0.0, 2.0) * 0.42;
    vec3 windDir = vec3(wind2.x, 0.0, wind2.y);
    vec3 sideDir = vec3(-wind2.y, 0.0, wind2.x);
    vec3 fallDir = normalize(vec3(wind2.x * (0.026 + windPower * 0.034), -1.0, wind2.y * (0.026 + windPower * 0.034)));
    vec3 depthDir = safeNormalize3(cross(sideDir, fallDir), windDir);
    vec3 velocity = normalize(vec3(wind2.x * windPower * 0.34, -1.0, wind2.y * windPower * 0.34)) * (6.8 + clamp(u_WindSpeed, 0.0, 2.0) * 2.5);
    vec3 sunDir = normalize(vec3(-0.66, 0.14, 0.74));

    float pixelLoad = smoothstep(2500000.0, 7000000.0, u_Resolution.x * u_Resolution.y);
    int dynamicSteps = int(clamp(floor(mix(3.0, 4.0, strength) - pixelLoad * 0.85 + 0.5), 2.0, 4.0));
    float traceDistance = mix(32.0, 58.0, strength);
    float minDistance = 0.78;
    float stepSize = max(0.001, (traceDistance - minDistance) / float(max(dynamicSteps, 1)));
    float jitter = mix(0.32, ign(gl_FragCoord.xy), 0.74);
    float rayStart = minDistance + stepSize * jitter;

    const int maxSteps = 4;
    float rain = 0.0;
    float nearRain = 0.0;
    float streaks = 0.0;
    float haze = 0.0;
    float gleam = 0.0;
    vec2 lens = vec2(0.0);

    for (int i = 0; i < maxSteps; i++) {
        if (i >= dynamicSteps) {
            break;
        }
        float t = rayStart + float(i) * stepSize;
        float occlusion = hasDepth > 0.5 ? smoothstep(0.0, 1.35, geometryDistance - t) : 1.0;
        float distanceFade = 1.0 - smoothstep(traceDistance * 0.76, traceDistance, t);
        float fade = occlusion * distanceFade;
        if (fade <= 0.0001) {
            continue;
        }

        vec3 p = u_CameraPos + rayDir * t;
        float nearFactor = 1.0 - saturate(t / 34.0);
        float localGleam = 0.0;
        float localHaze = 0.0;
        vec2 localLens = vec2(0.0);
        float veil = rainVeil(p, windDir, sideDir, 0.0, nearFactor, strength, localLens, localGleam, localHaze);
        veil += rainVeil(p + vec3(19.0, 5.0, -13.0), windDir, sideDir, 0.72, nearFactor * 0.86, strength, localLens, localGleam, localHaze) * 0.48;
        float strandGleam = 0.0;
        vec2 strandLens = vec2(0.0);
        float sharp = strandField(p, sideDir, fallDir, depthDir, velocity, 1.16, 0.0, nearFactor, strength, strandLens, strandGleam);
        sharp += strandField(p + vec3(-14.0, 2.0, 17.0), sideDir, fallDir, depthDir, velocity, 0.74, 0.65, nearFactor * 0.78, strength, strandLens, strandGleam) * 0.62;
        float density = (veil + sharp * (0.50 + nearFactor * 0.52)) * fade;
        float weight = stepSize * (0.046 + strength * 0.072);
        rain += density * weight;
        nearRain += density * nearFactor * weight;
        streaks += sharp * fade * stepSize * (0.042 + strength * 0.070);
        haze += localHaze * fade * stepSize * (0.050 + strength * 0.076);
        gleam += (localGleam + strandGleam * 0.70) * fade * stepSize * (0.044 + strength * 0.066);
        lens += (localLens + strandLens) * fade * stepSize * (0.004 + strength * 0.006);
    }

    rain = saturate(rain * (1.52 + strength * 1.08));
    nearRain = saturate(nearRain * (1.36 + strength * 0.96));
    streaks = saturate(streaks * (1.42 + strength * 1.18));
    haze = saturate(haze * (1.14 + strength * 0.98));
    gleam = saturate(gleam * (1.08 + strength * 0.94));

    float forwardScatter = pow(saturate(dot(rayDir, sunDir) * 0.5 + 0.5), 3.2);
    float horizon = pow(1.0 - saturate(abs(rayDir.y)), 1.62);
    float upward = smoothstep(-0.14, 0.48, rayDir.y);
    float highlightGuard = smoothstep(0.68, 1.0, luminance(screenColor));
    vec2 refractedUv = clamp(uv + lens * (0.00015 + strength * 0.00032), vec2(0.001), vec2(0.999));
    vec3 refracted = texture(u_ScreenTexture, refractedUv).rgb;
    vec3 soft = blurScreen(refractedUv, 0.78 + nearRain * (1.92 + strength * 1.55) + haze * 1.18);

    vec3 atmosphere = max(u_AtmosphereTint, vec3(0.36, 0.45, 0.56));
    vec3 rainTint = mix(vec3(0.56, 0.70, 0.82), atmosphere, 0.42);
    vec3 dawnTint = vec3(1.0, 0.60, 0.42);
    vec3 coldMist = mix(vec3(0.49, 0.61, 0.70), vec3(0.72, 0.84, 0.91), upward);
    vec3 fogTint = mix(coldMist, dawnTint, saturate(forwardScatter * 0.44 + horizon * 0.18));
    vec3 specTint = mix(vec3(0.76, 0.88, 0.98), dawnTint, saturate(0.20 + forwardScatter * 0.64));

    float veil = saturate(haze * 0.46 + rain * 0.18 + horizon * strength * 0.052);
    float specular = saturate((gleam * (0.62 + forwardScatter * 0.88) + streaks * (0.13 + forwardScatter * 0.30)) * (1.0 - highlightGuard * 0.54));
    vec3 color = mix(screenColor, mix(refracted, soft, saturate(nearRain * 0.48 + haze * 0.22)), saturate(rain * 0.20 + haze * 0.13));
    color = mix(color, fogTint, veil * (0.23 + strength * 0.15));
    color += rainTint * rain * (0.14 + strength * 0.20) * (1.0 - highlightGuard * 0.24);
    color += fogTint * haze * (0.10 + strength * 0.15);
    color += specTint * specular * (0.20 + strength * 0.30);
    color += mix(vec3(0.74, 0.88, 0.98), specTint, 0.50) * streaks * (0.20 + strength * 0.32) * (1.0 - highlightGuard * 0.44);
    color += dawnTint * forwardScatter * haze * (0.035 + strength * 0.070);
    color = mix(color, color * vec3(0.91, 0.976, 1.045), saturate(streaks * 0.16 + rain * 0.07));
    color = color / (vec3(1.0) + max(color - 1.0, vec3(0.0)) * 0.34);
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
