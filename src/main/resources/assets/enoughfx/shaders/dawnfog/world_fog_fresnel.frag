#version 330 core

layout(location = 0) out vec4 fragColor;

uniform sampler2D u_ScreenTexture;
uniform sampler2D u_DepthTexture;
uniform mat4 u_InverseProjectionMatrix;
uniform mat4 u_InverseViewMatrix;
uniform vec3 u_CameraPos;
uniform vec2 u_Resolution;
uniform float u_Time;
uniform vec3 u_SunDirection;
uniform vec3 u_SunScreen;
uniform float u_FogDensity;
uniform float u_FogMinHeight;
uniform float u_FogMaxHeight;
uniform float u_ViewDistance;
uniform vec3 u_PaletteZenith;
uniform vec3 u_PaletteHorizonWarm;
uniform vec3 u_PaletteHorizonCool;
uniform vec3 u_PaletteFogWarm;
uniform vec3 u_PaletteFogCool;
uniform vec3 u_PaletteRay;
uniform float u_Rainbow;
uniform vec3 u_RainbowDir;
uniform float u_RainbowSize;
uniform float u_GodRays;
uniform float u_Softness;

const vec2 POISSON[8] = vec2[8](
    vec2(-0.326, -0.406), vec2(-0.840, -0.074), vec2(-0.696, 0.457), vec2(-0.203, 0.621),
    vec2(0.962, -0.195), vec2(0.473, -0.480), vec2(0.519, 0.767), vec2(0.185, -0.893)
);

float saturate(float v) {
    return clamp(v, 0.0, 1.0);
}

float luminance(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + vec3(33.33));
    return fract((p3.x + p3.y) * p3.z);
}

vec3 srgbToLinear(vec3 c) {
    return vec3(
        c.r <= 0.04045 ? c.r / 12.92 : pow((c.r + 0.055) / 1.055, 2.4),
        c.g <= 0.04045 ? c.g / 12.92 : pow((c.g + 0.055) / 1.055, 2.4),
        c.b <= 0.04045 ? c.b / 12.92 : pow((c.b + 0.055) / 1.055, 2.4)
    );
}

vec3 linearToSrgb(vec3 c) {
    c = clamp(c, 0.0, 1.0);
    return vec3(
        c.r <= 0.0031308 ? c.r * 12.92 : 1.055 * pow(c.r, 0.4166666666666667) - 0.055,
        c.g <= 0.0031308 ? c.g * 12.92 : 1.055 * pow(c.g, 0.4166666666666667) - 0.055,
        c.b <= 0.0031308 ? c.b * 12.92 : 1.055 * pow(c.b, 0.4166666666666667) - 0.055
    );
}

vec3 linearToOklab(vec3 c) {
    float l = 0.4122214708 * c.r + 0.5363325363 * c.g + 0.0514459929 * c.b;
    float m = 0.2119034982 * c.r + 0.6806995451 * c.g + 0.1073969566 * c.b;
    float s = 0.0883024619 * c.r + 0.2817188376 * c.g + 0.6299787005 * c.b;
    float l_ = sign(l) * pow(abs(l), 0.3333333333333333);
    float m_ = sign(m) * pow(abs(m), 0.3333333333333333);
    float s_ = sign(s) * pow(abs(s), 0.3333333333333333);
    return vec3(
        0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_,
        1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_,
        0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_
    );
}

vec3 oklabToLinear(vec3 c) {
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

vec3 oklabMix(vec3 a, vec3 b, float t) {
    vec3 la = linearToOklab(srgbToLinear(a));
    vec3 lb = linearToOklab(srgbToLinear(b));
    return linearToSrgb(oklabToLinear(mix(la, lb, saturate(t))));
}

vec4 permute4(vec4 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

vec4 taylorInvSqrt4(vec4 r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + 2.0 * C.xxx;
    vec3 x3 = x0 - 1.0 + 3.0 * C.xxx;
    i = mod(i, 289.0);
    vec4 p = permute4(permute4(permute4(
            i.z + vec4(0.0, i1.z, i2.z, 1.0))
            + i.y + vec4(0.0, i1.y, i2.y, 1.0))
            + i.x + vec4(0.0, i1.x, i2.x, 1.0));
    float n_ = 1.0 / 7.0;
    vec3 ns = n_ * D.wyz - D.xzx;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);
    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);
    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);
    vec4 norm = taylorInvSqrt4(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;
    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

float fbm(vec3 p) {
    float sum = 0.6 * snoise(p);
    sum += 0.3 * snoise(p * 2.11 + vec3(19.1, 7.3, 5.2));
    return sum;
}

vec3 spectral(float t) {
    t = clamp(t, 0.0, 1.0);
    vec3 c = mix(vec3(0.54, 0.30, 0.89), vec3(0.25, 0.50, 0.95), saturate(t * 4.0));
    c = mix(c, vec3(0.25, 0.85, 0.45), saturate(t * 4.0 - 1.0));
    c = mix(c, vec3(0.98, 0.90, 0.30), saturate(t * 4.0 - 2.0));
    c = mix(c, vec3(0.95, 0.28, 0.22), saturate(t * 4.0 - 3.0));
    return c;
}

vec3 worldPosFromDepth(vec2 uv, float depth) {
    vec4 clipSpace = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewSpace = u_InverseProjectionMatrix * clipSpace;
    float invW = abs(viewSpace.w) > 0.000001 ? 1.0 / viewSpace.w : 1.0;
    viewSpace *= invW;
    return (u_InverseViewMatrix * viewSpace).xyz;
}

float heightIntegral(vec3 ro, vec3 rd, float dist) {
    float thickness = max(u_FogMaxHeight - u_FogMinHeight, 1.0);
    float k = 2.0 / thickness;
    float rdy = abs(rd.y) < 0.00002 ? (rd.y < 0.0 ? -0.00002 : 0.00002) : rd.y;
    float base = exp(-clamp(k * max(ro.y - u_FogMinHeight, 0.0), 0.0, 60.0));
    float travel = 1.0 - exp(clamp(-k * rdy * dist, -60.0, 60.0));
    return clamp(base * travel / (k * rdy), 0.0, dist);
}

float henyeyGreenstein(float mu, float g) {
    float g2 = g * g;
    return (1.0 - g2) / pow(max(1.0 + g2 - 2.0 * g * mu, 0.0001), 1.5);
}

float godRaysMask(vec2 uv, vec2 sunUv, float jitter) {
    vec2 stepUv = (sunUv - uv) / 8.0;
    vec2 pos = uv + stepUv * jitter;
    float sum = 0.0;
    float weight = 1.0;
    float total = 0.0;
    for (int i = 0; i < 8; i++) {
        vec2 s = clamp(pos, vec2(0.001), vec2(0.999));
        sum += step(0.999999, texture(u_DepthTexture, s).r) * weight;
        total += weight;
        weight *= 0.925;
        pos += stepUv;
    }
    return sum / max(total, 0.0001);
}

vec3 sampleSceneSoft(vec2 uv, float radiusPx, float rotation) {
    vec3 center = texture(u_ScreenTexture, uv).rgb;
    if (radiusPx < 0.5) {
        return center;
    }
    vec2 px = vec2(radiusPx) / max(u_Resolution, vec2(1.0));
    float ca = cos(rotation);
    float sa = sin(rotation);
    mat2 basis = mat2(ca, sa, -sa, ca);
    vec3 acc = center * 0.25;
    for (int i = 0; i < 3; i++) {
        vec2 s = clamp(uv + basis * POISSON[i] * px, vec2(0.001), vec2(0.999));
        acc += texture(u_ScreenTexture, s).rgb * 0.25;
    }
    return acc;
}

vec3 atmoSky(vec3 rd, float mu) {
    float up = saturate(rd.y);
    float below = saturate(-rd.y);
    vec3 grad = oklabMix(u_PaletteHorizonCool, u_PaletteZenith, pow(up, 0.58));
    float band = pow(1.0 - up, 3.2);
    float sunNear = pow(saturate(mu * 0.5 + 0.5), 3.0);
    grad = oklabMix(grad, u_PaletteHorizonWarm, saturate(sunNear * (0.30 + 0.70 * band)));
    grad += u_PaletteRay * henyeyGreenstein(mu, 0.62) * 0.045;
    grad = mix(grad, u_PaletteHorizonCool * 0.9, below * 0.8);
    return grad;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(u_Resolution, vec2(1.0));
    float depth = texture(u_DepthTexture, uv).r;
    float validDepth = 1.0 - step(0.999999, depth);
    float skyMask = 1.0 - validDepth;

    vec3 ro = u_CameraPos;
    vec3 farPoint = worldPosFromDepth(uv, 0.999999);
    vec3 rd = normalize(farPoint - ro + vec3(0.0, 0.0000001, 0.0));
    vec3 surfacePos = worldPosFromDepth(uv, min(depth, 0.999999));
    float sceneDist = mix(2600.0, length(surfacePos - ro), validDepth);

    float edgeFade = smoothstep(0.0, 7.5, sceneDist);

    float midY = (u_FogMinHeight + u_FogMaxHeight) * 0.5;
    float toMid = abs(midY - ro.y) / max(abs(rd.y), 0.04);
    float probeDist = clamp(min(toMid, sceneDist * 0.5), 6.0, 220.0);
    float caveFade = smoothstep(u_FogMinHeight - 40.0, u_FogMinHeight - 12.0, ro.y);
    float densityMod = 1.0;
    if (caveFade > 0.0001) {
        vec3 probe = ro + rd * probeDist;
        vec3 drift = vec3(u_Time * 1.05, u_Time * 0.16, u_Time * 0.58);
        float macro = fbm(probe * 0.045 + drift * 0.6);
        float sheets = snoise(vec3(probe.x * 0.052, probe.y * 0.155, probe.z * 0.052) + drift * 0.35);
        vec3 probeNear = ro + rd * min(probeDist * 0.45, 60.0);
        float detail = snoise(probeNear * 0.21 - drift * 0.5);
        float turbulence = macro * 0.62 + sheets * 0.28 + detail * 0.10;
        densityMod = mix(0.55, 1.50, saturate(turbulence * 0.5 + 0.5));
        densityMod = mix(densityMod, 1.0, smoothstep(120.0, 220.0, probeDist));
    }
    float sigma = u_FogDensity * 0.058 * densityMod * caveFade;
    float fogGeom = saturate((1.0 - exp(-sigma * heightIntegral(ro, rd, min(sceneDist, 900.0)))) * edgeFade);
    float fogSky = saturate(1.0 - exp(-sigma * heightIntegral(ro, rd, 4000.0)));
    float fogAmount = mix(fogSky, fogGeom, validDepth);

    // Low drifting sheets keep the fog visibly alive close to the terrain.
    vec2 flowUv = surfacePos.xz * 0.036 + vec2(u_Time * 0.17, u_Time * 0.065);
    float flowMacro = fbm(vec3(flowUv.x, surfacePos.y * 0.075, flowUv.y));
    float flowDetail = snoise(vec3(surfacePos.x * 0.082 - u_Time * 0.21,
                                   surfacePos.y * 0.115,
                                   surfacePos.z * 0.082 + u_Time * 0.10));
    float flowNoise = saturate(flowMacro * 0.48 + flowDetail * 0.22 + 0.52);
    float groundBand = exp(-abs(surfacePos.y - (ro.y - 1.45)) * 0.48);
    float nearGate = smoothstep(5.0, 18.0, sceneDist) * (1.0 - smoothstep(115.0, 220.0, sceneDist));
    float surfaceWisps = smoothstep(0.50, 0.78, flowNoise) * groundBand * nearGate * validDepth * caveFade;
    fogAmount = max(fogAmount, surfaceWisps * (0.22 + 0.58 * u_FogDensity));

    vec3 sun = normalize(u_SunDirection);
    float mu = dot(rd, sun);
    float lowSun = saturate(1.0 - abs(sun.y) * 2.2);
    float horizonGlow = pow(saturate(1.0 - abs(rd.y) * 1.30), 2.3);

    vec3 skyCol = atmoSky(rd, mu);

    float sunward = pow(saturate(mu * 0.5 + 0.5), 3.4);
    float vert = saturate(rd.y * 1.9 + 0.55);
    vec3 shadowSide = mix(u_PaletteFogCool * 0.92, u_PaletteFogCool * 1.10, vert);
    vec3 dawnSide = mix(u_PaletteFogWarm, u_PaletteHorizonWarm, pow(saturate(mu), 2.4));
    float bleed = saturate(sunward * (0.30 + 0.70 * lowSun));
    vec3 fogTint = oklabMix(shadowSide, dawnSide, bleed);
    float forward = henyeyGreenstein(mu, 0.56) * 0.085;
    vec3 bloomGlow = u_PaletteRay * forward * (0.35 + 0.65 * lowSun) * horizonGlow;
    fogTint += bloomGlow * 0.85;
    float tintLum = luminance(fogTint);
    fogTint /= 1.0 + max(tintLum - 0.865, 0.0) * 2.4;

    float viewRange = max(u_ViewDistance, 64.0);
    float farHaze = smoothstep(viewRange * 0.45, viewRange * 0.92, sceneDist) * validDepth;
    farHaze *= mix(0.75, 1.0, pow(1.0 - saturate(rd.y), 3.2));

    float skyBlend = saturate(0.42 + 0.58 * pow(1.0 - saturate(rd.y), 2.6));

    float blurDrive = max(fogAmount, max(farHaze * 0.7, skyMask * skyBlend * 0.45));
    float blurRadius = u_Softness * 7.5 * blurDrive;
    float rotation = hash12(gl_FragCoord.xy) * 6.2831853;
    vec3 scene = sampleSceneSoft(uv, blurRadius, rotation);

    vec3 color = mix(scene, skyCol, skyMask * skyBlend);
    vec3 veiled = color * mix(vec3(1.0), vec3(0.90, 0.94, 1.03), fogAmount * 0.30);
    color = mix(veiled, fogTint, fogAmount);
    color = mix(color, skyCol, farHaze * 0.92);

    float rays = 0.0;
    vec3 sunGlow = vec3(0.0);
    if (u_GodRays > 0.001 && u_SunScreen.z > 0.001) {
        float jitter = hash12(gl_FragCoord.yx + vec2(17.0, 59.0));
        float mask = godRaysMask(uv, u_SunScreen.xy, jitter);
        float angular = pow(saturate(mu * 0.5 + 0.5), 3.0);
        rays = mask * angular * u_SunScreen.z * u_GodRays * 2.15;
        rays *= 0.62 + 0.38 * saturate(fogAmount + skyMask + farHaze);
        float dy = rd.y - sun.y;
        float angSq = max(2.0 * (1.0 - mu), 0.0);
        float disc = exp(-angSq * 2600.0);
        float dx2 = max(angSq - dy * dy, 0.0);
        float streak = exp(-(dy * dy * 850.0 + dx2 * 34.0));
        float glowVis = u_SunScreen.z * u_GodRays * (0.35 + 0.65 * lowSun);
        float sunCanvas = saturate(skyMask + farHaze);
        sunGlow = u_PaletteRay * (disc * 1.12 * sunCanvas + streak * 0.55 * mix(0.25, 1.0, sunCanvas)) * glowVis;
    }
    vec3 rayGlow = clamp(u_PaletteRay * rays * 1.10 + sunGlow, vec3(0.0), vec3(1.0));
    color = 1.0 - (1.0 - clamp(color, vec3(0.0), vec3(1.0))) * (1.0 - rayGlow);

    if (u_Rainbow > 0.001) {
        float cosA = dot(rd, normalize(u_RainbowDir));
        float ang = degrees(acos(clamp(cosA, -1.0, 1.0)));
        float radius = clamp(u_RainbowSize, 40.0, 64.0);
        float scale = radius / 42.0;
        float width = 3.4 * scale;
        float bandLo = radius - width;
        float canvas = saturate(skyMask + farHaze * 0.8);
        float lift = smoothstep(-0.02, 0.06, rd.y);
        float presence = canvas * lift * (1.0 - fogAmount * 0.9) * u_Rainbow;
        if (presence > 0.001) {
            float inner = 1.0 - smoothstep(bandLo - 6.5, bandLo + 0.4, ang);
            color += vec3(0.95, 0.97, 1.0) * inner * 0.05 * presence;
            float tP = (ang - bandLo) / width;
            if (tP > 0.0 && tP < 1.0) {
                float band = sin(tP * 3.14159265);
                color += mix(spectral(tP), vec3(1.0), 0.20) * band * band * 0.70 * presence;
            }
            float sn = (bandLo - ang) / scale;
            if (sn > 0.0 && sn < 3.6) {
                float fringe = sin(sn * 2.618);
                color += mix(spectral(0.22), vec3(1.0), 0.35) * fringe * fringe * exp(-sn * 1.1) * 0.16 * presence;
            }
        }
    }

    float outLum = luminance(color);
    color /= 1.0 + max(outLum - 0.985, 0.0) * 1.6;
    color += vec3((hash12(gl_FragCoord.xy + vec2(311.7, 74.3)) - 0.5) * 0.0078);

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
