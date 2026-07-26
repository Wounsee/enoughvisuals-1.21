#version 150

uniform vec4 ColorModulator;

uniform sampler2D u_ScreenTexture;

layout(std140) uniform PrismaticChams {
    vec4 u_AccentTop;
    vec4 u_AccentBottom;
    vec4 u_CameraAndTime;
    vec4 u_Params;
    vec4 u_State;
    vec4 u_ResolutionInfo;
    ivec4 u_ModeFlags;
};

#define u_CameraPos u_CameraAndTime.xyz
#define u_Time u_CameraAndTime.w
#define u_Intensity u_Params.x
#define u_Opacity u_Params.y
#define u_Refraction u_Params.z
#define u_Animation u_State.x
#define u_Pass u_State.y
#define u_Seed u_State.z
#define u_DepthMode u_State.w
#define u_Resolution u_ResolutionInfo.xy
#define u_Texel u_ResolutionInfo.zw
#define u_Mode u_ModeFlags.x

in vec3 v_Normal;
in vec3 v_ViewDir;
in vec3 v_ViewPos;
in vec3 v_Position;
in vec4 v_Color;
in vec2 v_Uv;
in vec2 v_ScreenPos;
in vec2 v_EntityScreenPos;

out vec4 fragColor;

const float PI2 = 6.28318530718;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

vec2 safeUv(vec2 uv) {
    return clamp(uv, vec2(0.0015), vec2(0.9985));
}

vec3 toneMap(vec3 color) {
    return color / (vec3(1.0) + color * 0.58);
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise21(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

vec3 palette(float t) {
    vec3 a = vec3(0.54, 0.51, 0.58);
    vec3 b = vec3(0.48, 0.43, 0.42);
    vec3 c = vec3(0.98, 1.03, 0.92);
    vec3 d = vec3(0.02, 0.22, 0.38);
    return clamp(a + b * cos(PI2 * (c * t + d)), 0.0, 1.0);
}

vec3 screenChromatic(vec2 uv, vec2 offset) {
    float r = texture(u_ScreenTexture, safeUv(uv + offset * 1.18)).r;
    float g = texture(u_ScreenTexture, safeUv(uv + offset * 0.82)).g;
    float b = texture(u_ScreenTexture, safeUv(uv + offset * 0.48)).b;
    return vec3(r, g, b);
}

vec4 prismaticCrystal(vec2 screenUv, vec3 normal, vec3 viewDir, vec3 reflectDir, float fresnel, float pulse) {
    vec2 distortion = normal.xy * 0.05 * clamp(u_Refraction, 0.35, 1.15) * mix(0.42, 1.0, fresnel);
    vec3 refracted = screenChromatic(screenUv, distortion);
    float facetA = floor((dot(normalize(v_Position + normal * 0.35), vec3(0.88, 0.31, 0.37)) * 0.5 + 0.5) * 8.0) / 8.0;
    float facetB = floor((dot(normalize(v_Position.yzx - normal * 0.18), vec3(-0.24, 0.81, 0.54)) * 0.5 + 0.5) * 10.0) / 10.0;
    float facet = saturate(abs(facetA - facetB) * 2.8);
    float sparkle = pow(saturate(sin(dot(reflectDir, vec3(15.7, 7.4, 11.2)) + u_Time * 2.1 + u_Seed * 9.0) * 0.5 + 0.5), 18.0);
    vec3 accent = mix(u_AccentBottom.rgb, u_AccentTop.rgb, saturate(normal.y * 0.5 + 0.5));
    vec3 film = palette(facetA * 0.67 + facetB * 0.41 + fresnel * 0.42 + u_Time * 0.035);
    float rim = pow(fresnel, 1.55);
    vec3 color = refracted * 0.72 + mix(accent, film, 0.66) * (0.34 + facet * 0.28);
    color += vec3(1.0) * rim * 2.75;
    color += film * sparkle * (0.8 + pulse * 0.45);
    float alpha = mix(0.24, 0.82, saturate(rim + facet * 0.22));
    return vec4(color, alpha);
}

vec4 gravitationalVoid(vec2 screenUv, vec2 centerUv, vec3 normal, float fresnel, float pulse) {
    float inward = 1.0 - fresnel;
    vec2 warped = screenUv - normal.xy * 0.08 * inward * clamp(u_Refraction, 0.35, 1.15);
    warped = mix(warped, centerUv, 0.34 * inward);
    vec3 scene = texture(u_ScreenTexture, safeUv(warped)).rgb;
    float radius = length((screenUv - centerUv) * vec2(u_Resolution.x / max(u_Resolution.y, 1.0), 1.0));
    float disk = exp(-radius * 8.0) * (0.62 + 0.38 * pulse);
    float rim = pow(fresnel, 0.72);
    vec3 neon = mix(u_AccentBottom.rgb, u_AccentTop.rgb, 0.5 + 0.5 * sin(u_Time * 1.35 + u_Seed * PI2));
    vec3 diskColor = mix(neon, vec3(1.0, 0.92, 0.68), 0.28 + 0.24 * pulse);
    vec3 color = scene * 0.055 * inward;
    color += diskColor * disk * 1.65;
    color += neon * rim * 5.8;
    color = mix(vec3(0.0), color, saturate(rim * 1.65 + disk * 0.34));
    float alpha = saturate(0.38 + rim * 0.56 + disk * 0.22);
    return vec4(color, alpha);
}

vec4 etherealPhantom(vec2 screenUv, vec3 normal, vec3 viewDir, vec3 reflectDir, float fresnel, float pulse) {
    float n1 = noise21(v_ScreenPos * 0.42 + vec2(u_Time * 41.0, -u_Time * 27.0) + u_Seed * 97.0);
    float n2 = noise21(v_ScreenPos * 0.18 + vec2(-u_Time * 17.0, u_Time * 33.0) + u_Seed * 31.0);
    vec2 heat = vec2(n1 - 0.5, n2 - 0.5) * (0.008 + 0.017 * (1.0 - fresnel));
    vec2 warped = screenUv + heat + normal.xy * 0.018 * clamp(u_Refraction, 0.35, 1.15);
    vec3 scene = texture(u_ScreenTexture, safeUv(warped)).rgb;
    float film = dot(reflectDir, vec3(0.31, 0.57, 0.21)) * 0.5 + 0.5 + u_Time * 0.045 + u_Seed * 0.31;
    vec3 iridescence = palette(film + fresnel * 0.72);
    vec3 glint = mix(u_AccentBottom.rgb, u_AccentTop.rgb, saturate(normal.y * 0.5 + 0.5));
    float shimmer = pow(saturate(n1 * n2 + fresnel * 0.58), 3.2);
    vec3 color = scene * 0.86 + iridescence * (0.18 + fresnel * 0.62) + glint * shimmer * 0.68;
    float alpha = saturate(0.11 + fresnel * 0.32 + shimmer * 0.08);
    return vec4(color, alpha);
}

void main() {
    vec3 normal = normalize(v_Normal);
    vec3 viewDir = normalize(v_ViewDir);
    vec3 reflectDir = reflect(-viewDir, normal);
    float ndv = saturate(dot(normal, viewDir));
    float fresnel = pow(1.0 - ndv, 3.0);
    float pulse = 0.5 + 0.5 * sin(u_Time * 2.2 + u_Seed * PI2);
    vec2 screenUv = safeUv(v_ScreenPos / max(u_Resolution, vec2(1.0)));
    vec2 centerUv = safeUv(v_EntityScreenPos / max(u_Resolution, vec2(1.0)));

    vec4 result;
    if (u_Mode == 1) {
        result = gravitationalVoid(screenUv, centerUv, normal, fresnel, pulse);
    } else if (u_Mode == 2) {
        result = etherealPhantom(screenUv, normal, viewDir, reflectDir, fresnel, pulse);
    } else {
        result = prismaticCrystal(screenUv, normal, viewDir, reflectDir, fresnel, pulse);
    }

    float animation = smoothstep(0.0, 1.0, clamp(u_Animation, 0.0, 1.0));
    float overshoot = max(u_Animation - 1.0, 0.0);
    float passMask = step(0.5, u_Pass);
    vec3 passTint = mix(u_AccentBottom.rgb, u_AccentTop.rgb, pulse);
    result.rgb = mix(result.rgb, result.rgb * 0.56 + passTint * fresnel * 0.82, passMask);
    result.a *= mix(1.0, 0.46, passMask);
    result.rgb += v_Color.rgb * 0.025;
    result.rgb *= u_Intensity * (1.0 + overshoot * 1.2);
    result.a = saturate(result.a * u_Opacity * animation);
    fragColor = vec4(clamp(toneMap(result.rgb), 0.0, 1.0), result.a) * ColorModulator;
}
