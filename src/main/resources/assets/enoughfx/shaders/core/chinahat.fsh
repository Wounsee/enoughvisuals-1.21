#version 150

uniform float GameTime;
uniform vec4 ColorModulator;

in vec3 vViewPos;
in vec3 vNormal;
in vec2 vUv;
in vec4 vColor;

out vec4 fragColor;

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

float ported_sat(float v) {
    return clamp(v, 0.0, 1.0);
}

float ported_hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(443.8975, 397.2973, 491.1871));
    p3 += dot(p3, p3.yzx + 19.1919);
    return fract((p3.x + p3.y) * p3.z);
}

float ported_noise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = ported_hash21(i);
    float b = ported_hash21(i + vec2(1.0, 0.0));
    float c = ported_hash21(i + vec2(0.0, 1.0));
    float d = ported_hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float ported_fbm2(vec2 p) {
    return ported_noise2(p) * 0.5000 + ported_noise2(p * 2.071 + vec2(4.17, 7.91)) * 0.2500 + ported_noise2(p * 4.113 + vec2(9.83, 2.41)) * 0.1250 + ported_noise2(p * 8.217 + vec2(1.37, 6.73)) * 0.0625;
}

vec3 ported_hueShift(vec3 c, float r) {
    vec3 k = vec3(0.57735026919);
    float cs = cos(r);
    float sn = sin(r);
    return c * cs + cross(k, c) * sn + k * dot(k, c) * (1.0 - cs);
}

vec3 ported_tone(vec3 c) {
    float m = max(max(c.r, c.g), c.b);
    return clamp(c / (1.0 + m * 0.28), 0.0, 1.0);
}

float ported_pulse(float x, float w) {
    float d = abs(fract(x) - 0.5);
    return 1.0 - smoothstep(0.0, w, d);
}

void main() {
    float pass = floor(vUv.y);
    float lane = fract(vUv.y);
    float phase = fract(vUv.x);
    float time = GameTime;

    vec3 n = normalize(vNormal);
    vec3 eye = normalize(-vViewPos);
    vec3 accent = max(vColor.rgb, vec3(0.01));
    vec2 flow = vec2(phase * 9.0 + time * 4.8, lane * 7.5 - time * 3.2);
    float frost = ported_fbm2(flow);
    float frostFine = ported_fbm2(flow * 2.45 + vec2(11.7, 5.2));
    float bump = ported_fbm2(flow * 1.36 + vec2(2.0, -3.0));
    float bumpX = ported_fbm2(flow * 1.36 + vec2(2.047, -3.0)) - bump;
    float bumpY = ported_fbm2(flow * 1.36 + vec2(2.0, -2.953)) - bump;
    vec3 dpx = dFdx(vViewPos);
    vec3 dpy = dFdy(vViewPos);
    vec3 screenX = dpx / max(length(dpx), 0.00001);
    vec3 screenY = dpy / max(length(dpy), 0.00001);
    n = normalize(n + (screenX * bumpX + screenY * bumpY) * (1.35 + (1.0 - lane) * 0.80));
    float ndv = ported_sat(abs(dot(n, eye)));
    float fresnel = pow(1.0 - ndv, 3.45);
    float softFresnel = pow(1.0 - ndv, 1.35);
    vec3 neon = ported_hueShift(accent, sin(time * 31.0 + phase * 6.2831853) * 0.030);
    float sweep = ported_pulse(phase - time * 7.0, 0.075);
    float sparkle = pow(ported_sat(frostFine), 7.0);

    vec3 color = vec3(0.0);
    float alpha = vColor.a;

    if (pass < 0.5) {
        float rim = smoothstep(0.58, 1.0, lane);
        float topHot = exp(-(lane * 3.05) * (lane * 3.05));
        float topBand = exp(-((lane - 0.125) * 8.25) * ((lane - 0.125) * 8.25));
        float tipPresence = 0.46 + 0.54 * smoothstep(0.0, 0.18, lane);
        float outerFade = 1.0 - smoothstep(0.992, 1.0, lane);
        float grain = smoothstep(0.34, 1.0, frost) * 0.17 + smoothstep(0.43, 1.0, frostFine) * 0.125;
        vec3 base = vec3(0.0042, 0.0040, 0.0068);
        vec3 mica = vec3(0.0105, 0.0084, 0.0148) * frost + vec3(0.0068, 0.0052, 0.0118) * frostFine;
        vec3 reflection = oklab_mix_srgb(vec3(0.020, 0.022, 0.034), neon, 0.38) * (fresnel * 1.55 + softFresnel * 0.28);
        vec3 caustic = neon * (grain * 0.58 + rim * 0.115 + sweep * 0.055 + sparkle * 0.52);
        vec3 topLight = neon * (topHot * 0.185 + topBand * 0.145);
        color = base + mica + reflection + caustic + topLight + accent * (0.020 + rim * 0.045);
        alpha *= ported_sat((0.515 + fresnel * 0.315 + softFresnel * 0.080 + rim * 0.165 + topHot * 0.175 + topBand * 0.120 + grain * 0.135) * tipPresence * outerFade);
    } else if (pass < 1.5) {
        float core = exp(-((lane - 0.80) * 6.35) * ((lane - 0.80) * 6.35));
        float body = exp(-((lane - 0.62) * 2.05) * ((lane - 0.62) * 2.05));
        float feather = smoothstep(0.025, 0.155, lane) * (1.0 - smoothstep(0.986, 1.0, lane));
        float hot = smoothstep(0.42, 0.96, lane);
        vec3 splitA = ported_hueShift(neon, -0.035);
        vec3 splitB = ported_hueShift(neon, 0.045);
        color = oklab_mix_srgb(splitA, splitB, 0.5 + 0.5 * sin(phase * 19.0 + time * 34.0));
        color *= 1.05 + body * 0.62 + core * 1.70 + hot * 0.76 + fresnel * 0.70 + sweep * 0.32;
        color += vec3(1.0, 0.90, 1.0) * core * 0.42 + accent * body * 0.22;
        alpha *= ported_sat(feather * (0.46 + body * 0.42 + core * 0.74 + hot * 0.24 + sweep * 0.18));
    } else if (pass < 2.5) {
        float core = exp(-lane * 5.20);
        float decay = exp(-lane * 2.92);
        float corona = exp(-(lane * 1.72) * (lane * 1.72));
        float haze = exp(-lane * 0.82);
        float edgeFade = 1.0 - smoothstep(0.960, 1.0, lane);
        float pulseGlow = 0.78 + 0.22 * sin(time * 42.0 + phase * 6.2831853);
        color = neon * (core * 2.55 + decay * 1.35 + corona * 0.74 + haze * 0.26 + fresnel * 0.34 + sweep * 0.26);
        color += accent * (core * 0.58 + decay * 0.34 + corona * 0.22) + vec3(1.0, 0.88, 1.0) * core * 0.42;
        alpha *= ported_sat((core * pulseGlow * 1.16 + decay * 0.38 + corona * 0.22 + haze * 0.08 + sweep * 0.12) * edgeFade);
    } else {
        discard;
    }

    vec4 outColor = vec4(ported_tone(color), ported_sat(alpha)) * ColorModulator;
    if (outColor.a <= 0.001) {
        discard;
    }
    fragColor = outColor;
}
