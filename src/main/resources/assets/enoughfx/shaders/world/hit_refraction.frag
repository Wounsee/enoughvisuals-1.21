#version 330 core

in vec2 vUv;
out vec4 fragColor;

const int MAX_TORI = 10;
const int MAX_STEPS = 96;
const float SURF = 0.0009;

uniform sampler2D u_scene;
uniform sampler2D u_depth;
uniform mat4 u_proj;
uniform mat4 u_invProj;
uniform vec2 u_resolution;
uniform float u_time;
uniform int u_count;
uniform int u_chroma;
uniform int u_depthOcclusion;

uniform vec3 u_center[MAX_TORI];
uniform vec3 u_axis[MAX_TORI];
uniform vec4 u_shape[MAX_TORI];
uniform vec3 u_glow[MAX_TORI];

float saturate(float v) {
    return clamp(v, 0.0, 1.0);
}

vec2 safeUv(vec2 uv) {
    return clamp(uv, vec2(0.0008), vec2(0.9992));
}

float sdTorus(vec3 q, vec3 axis, float radius, float thickness) {
    float y = dot(q, axis);
    vec3 planar = q - axis * y;
    float x = length(planar);
    return length(vec2(x - radius, y)) - thickness;
}

float mapScene(vec3 p, out int hitIndex) {
    float best = 1.0e9;
    hitIndex = -1;
    int count = min(u_count, MAX_TORI);
    for (int i = 0; i < MAX_TORI; i++) {
        if (i >= count) {
            break;
        }
        if (u_shape[i].z <= 0.0) {
            continue;
        }
        float d = sdTorus(p - u_center[i], u_axis[i], u_shape[i].x, max(u_shape[i].y, 0.012));
        if (d < best) {
            best = d;
            hitIndex = i;
        }
    }
    return best;
}

vec3 calcNormal(vec3 p) {
    int dummy;
    vec2 e = vec2(1.0, -1.0) * 0.0011;
    return normalize(
        e.xyy * mapScene(p + e.xyy, dummy) +
        e.yyx * mapScene(p + e.yyx, dummy) +
        e.yxy * mapScene(p + e.yxy, dummy) +
        e.xxx * mapScene(p + e.xxx, dummy)
    );
}

vec3 rayDirView(vec2 uv) {
    vec2 ndc = uv * 2.0 - 1.0;
    vec4 far = u_invProj * vec4(ndc, 1.0, 1.0);
    return normalize(far.xyz / far.w);
}

vec2 projectUv(vec3 viewPos, out bool ok) {
    vec4 clip = u_proj * vec4(viewPos, 1.0);
    ok = clip.w > 0.0001;
    if (!ok) {
        return vec2(0.0);
    }
    return clip.xy / clip.w * 0.5 + 0.5;
}

float projectDepth(vec3 viewPos, out bool ok) {
    vec4 clip = u_proj * vec4(viewPos, 1.0);
    ok = clip.w > 0.0001;
    if (!ok) {
        return 1.0;
    }
    return clip.z / clip.w * 0.5 + 0.5;
}

void main() {
    vec2 uv = vUv;
    vec3 sceneCol = texture(u_scene, uv).rgb;

    vec3 ro = vec3(0.0);
    vec3 rd = rayDirView(uv);

    float tEnter = 1.0e9;
    float tExit = -1.0e9;
    bool any = false;
    int count = min(u_count, MAX_TORI);
    for (int i = 0; i < MAX_TORI; i++) {
        if (i >= count) {
            break;
        }
        if (u_shape[i].z <= 0.0) {
            continue;
        }
        vec3 oc = ro - u_center[i];
        float rb = u_shape[i].x + max(u_shape[i].y, 0.012) + 0.05;
        float b = dot(oc, rd);
        float c = dot(oc, oc) - rb * rb;
        float disc = b * b - c;
        if (disc < 0.0) {
            continue;
        }
        float s = sqrt(disc);
        float t1 = -b + s;
        if (t1 < 0.0) {
            continue;
        }
        float t0 = -b - s;
        tEnter = min(tEnter, max(t0, 0.0));
        tExit = max(tExit, t1);
        any = true;
    }

    if (!any) {
        fragColor = vec4(sceneCol, 1.0);
        return;
    }

    float t = tEnter;
    int marchIndex = -1;
    bool hit = false;
    for (int s = 0; s < MAX_STEPS; s++) {
        vec3 p = ro + rd * t;
        float d = mapScene(p, marchIndex);
        if (d < SURF) {
            hit = true;
            break;
        }
        t += max(d * 0.85, 0.0016);
        if (t > tExit) {
            break;
        }
    }

    if (!hit) {
        fragColor = vec4(sceneCol, 1.0);
        return;
    }

    vec3 p = ro + rd * t;
    int idx;
    mapScene(p, idx);
    if (idx < 0) {
        fragColor = vec4(sceneCol, 1.0);
        return;
    }

    vec3 normal = calcNormal(p);
    vec3 view = -rd;
    float ndv = saturate(dot(normal, view));
    float fresnel = pow(1.0 - ndv, 5.0);

    float energy = u_shape[idx].z;
    float force = u_shape[idx].w;
    vec3 glow = u_glow[idx];

    float depthMask = 1.0;
    if (u_depthOcclusion == 1) {
        bool depthOk;
        float torusDepth = projectDepth(p, depthOk);
        float sceneDepth = texture(u_depth, uv).r;
        float validDepth = 1.0 - step(0.999999, sceneDepth);
        if (depthOk && validDepth > 0.5) {
            float frontGap = torusDepth - sceneDepth;
            depthMask = 1.0 - smoothstep(0.00018, 0.0022, frontGap);
            if (depthMask <= 0.001) {
                fragColor = vec4(sceneCol, 1.0);
                return;
            }
        }
    }

    vec3 refr = refract(rd, normal, 0.66);
    if (dot(refr, refr) < 1.0e-5) {
        refr = reflect(rd, normal);
    }

    float pulse = 0.5 + 0.5 * sin(u_time * 8.0 + float(idx) * 1.73);
    float reach = max(u_shape[idx].y, 0.012) * 3.15 + 0.14;
    bool okBase;
    bool okRefr;
    vec2 baseUv = projectUv(p, okBase);
    vec2 refrUv = projectUv(p + refr * reach, okRefr);
    vec2 offset = vec2(0.0);
    if (okBase && okRefr) {
        float warp = 13.0 + force * 155.0 + fresnel * 9.0 + pulse * 2.8;
        offset = (refrUv - baseUv) * warp * energy;
        offset += normal.xy * (0.0014 + force * 0.022) * fresnel * energy;
    }

    vec3 refracted;
    if (u_chroma == 1) {
        vec2 disp = offset * (0.24 + fresnel * 0.42);
        refracted.r = texture(u_scene, safeUv(uv + offset + disp)).r;
        refracted.g = texture(u_scene, safeUv(uv + offset)).g;
        refracted.b = texture(u_scene, safeUv(uv + offset - disp)).b;
    } else {
        refracted = texture(u_scene, safeUv(uv + offset)).rgb;
    }

    refracted *= mix(vec3(1.0), vec3(0.78, 0.89, 1.14), 0.42 * energy);

    vec3 halfVec = normalize(view + normalize(vec3(-0.35, 0.65, 0.55)));
    vec3 halfVec2 = normalize(view + normalize(vec3(0.58, 0.42, -0.34)));
    float spec = pow(saturate(dot(normal, halfVec)), 82.0);
    float glint = pow(saturate(dot(normal, halfVec2)), 150.0) * (0.55 + pulse * 0.45);

    float shimmer = 0.5 + 0.5 * sin(u_time * 5.0 + (p.x + p.y + p.z) * 8.0);
    vec3 rim = glow * (fresnel * (1.95 + 1.0 * energy) + spec * 1.75 + glint * 2.2);
    vec3 inner = glow * pow(saturate(1.0 - ndv), 2.0) * (0.30 + 0.24 * shimmer);
    vec3 caustic = vec3(0.76, 0.91, 1.18) * pow(saturate(sin((p.x - p.y + p.z) * 18.0 + u_time * 7.0) * 0.5 + 0.5), 8.0) * fresnel * 0.18;

    vec3 glass = refracted + rim * energy + inner * energy + caustic * energy;

    float alpha = saturate(energy * depthMask);
    vec3 outColor = mix(sceneCol, glass, alpha);
    fragColor = vec4(outColor, 1.0);
}
