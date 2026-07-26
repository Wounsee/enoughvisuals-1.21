#version 330 core

in vec2 vUv;
out vec4 fragColor;

const int MAX_PINCH = 12;

uniform sampler2D u_DepthTexture;
uniform mat4 u_Proj;
uniform mat4 u_InvProj;
uniform vec2 u_Resolution;
uniform float u_Time;
uniform int u_Count;
uniform int u_DepthAvailable;

uniform vec3 u_Center[MAX_PINCH];
uniform float u_LifeTime[MAX_PINCH];
uniform vec4 u_Params[MAX_PINCH];
uniform vec3 u_CoreTint[MAX_PINCH];

float saturate(float v) {
    return clamp(v, 0.0, 1.0);
}

float sq(float x) {
    return x * x;
}

float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 34.56);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm2(vec2 p) {
    float s = 0.6 * vnoise(p);
    s += 0.4 * vnoise(p * 2.07 + vec2(9.2, 4.7));
    return s;
}

float occlusion(vec2 uv, float plasmaDist) {
    if (u_DepthAvailable != 1) {
        return 1.0;
    }
    float sd = texture(u_DepthTexture, uv).r;
    if (sd >= 0.999999) {
        return 1.0;
    }
    vec4 vp = u_InvProj * vec4(uv * 2.0 - 1.0, sd * 2.0 - 1.0, 1.0);
    float sceneDist = -(vp.z / vp.w);
    return 1.0 - smoothstep(0.10, 0.55, plasmaDist - sceneDist);
}

void main() {
    vec2 uv = vUv;
    float aspect = u_Resolution.x / u_Resolution.y;

    vec3 accum = vec3(0.0);

    int count = min(u_Count, MAX_PINCH);
    for (int i = 0; i < MAX_PINCH; i++) {
        if (i >= count) {
            break;
        }
        vec3 c = u_Center[i];
        if (c.z > -0.02) {
            continue;
        }
        float t = u_LifeTime[i];
        if (t >= 1.0) {
            continue;
        }

        vec4 clip = u_Proj * vec4(c, 1.0);
        if (clip.w <= 1.0e-4) {
            continue;
        }
        vec2 cuv = clip.xy / clip.w * 0.5 + 0.5;

        float dist = -c.z;
        float worldR = u_Params[i].x;
        float m11 = u_Proj[1].y;
        float maxScreenR = min(0.14 + 0.05 * worldR, 0.30);
        float radiusUv = clamp(0.5 * m11 * worldR / dist, 0.02, maxScreenR);

        vec2 d = uv - cuv;
        d.x *= aspect;
        float r = length(d) / radiusUv;
        if (r > 1.4) {
            continue;
        }

        float temp = u_Params[i].y;
        float seed = u_Params[i].z;
        float intensity = u_Params[i].w;
        vec3 tint = u_CoreTint[i];
        float ang = atan(d.y, d.x);

        float eIn = smoothstep(0.0, 0.14, t);
        float eOut = 1.0 - smoothstep(0.40, 1.0, t);
        float life = eIn * eOut;

        float te = 1.0 - pow(1.0 - t, 2.4);
        float ringR = te * 1.02;

        float n = fbm2(vec2(ang * (2.0 + temp * 2.0) + seed * 3.0 + u_Time * 0.35, ringR * 1.6 + seed));
        float ringRad = ringR + (n - 0.5) * 0.06;

        float ringW = mix(0.34, 0.15, te);
        float ringG = (r - ringRad) / ringW;
        float ring = exp(-sq(ringG)) * (0.72 + 0.56 * n);

        float coreBloom = exp(-sq(r / 0.42));
        float coreE = (0.55 + 0.45 * eOut) * exp(-t * 3.0);

        float haze = exp(-sq(r / 0.95));

        vec3 ringCol = tint;
        vec3 coreCol = mix(tint, vec3(1.0), 0.5);
        vec3 hazeCol = mix(tint, vec3(0.55, 0.16, 0.62), 0.55);

        vec3 glow = ringCol * (ring * 0.40 * life)
                  + coreCol * (coreBloom * coreE * 0.46)
                  + hazeCol * (haze * life * 0.13);

        glow *= intensity;

        float occ = occlusion(uv, dist);
        accum += glow * occ;
    }

    if (dot(accum, accum) <= 0.0) {
        discard;
    }

    accum = accum / (1.0 + accum * 0.55);
    fragColor = vec4(accum, 1.0);
}
