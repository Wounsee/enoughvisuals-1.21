#version 150

uniform float GameTime;
uniform vec4 ColorModulator;

in vec3 vViewPos;
in vec3 vNormal;
in vec2 vUv;
in vec4 vColor;

out vec4 fragColor;

const float TAU = 6.28318530718;

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
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float ported_noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = ported_hash21(i);
    float b = ported_hash21(i + vec2(1.0, 0.0));
    float c = ported_hash21(i + vec2(0.0, 1.0));
    float d = ported_hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float ported_fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += ported_noise(p) * a;
        p = p * 2.07 + vec2(17.13, 9.71);
        a *= 0.5;
    }
    return v;
}

float ported_pulse(float x, float w) {
    float d = abs(fract(x) - 0.5);
    return 1.0 - smoothstep(0.0, w, d);
}

void main() {
    float layer = floor(vUv.y);
    float lane = fract(vUv.y);
    vec3 base = max(vColor.rgb, vec3(0.025));
    vec3 accent = base;
    vec3 soft = oklab_mix_srgb(base, vec3(1.0), 0.18);
    vec3 hot = oklab_mix_srgb(base, vec3(1.0), 0.42);
    vec3 n = normalize(vNormal);
    vec3 eye = normalize(-vViewPos);
    float ndv = ported_sat(abs(dot(n, eye)));
    float fresnel = pow(1.0 - ndv, 2.40);
    float razor = pow(1.0 - ndv, 5.20);
    float volume = exp(-ndv * 2.10);
    float packet = ported_pulse(vUv.x * 0.86 - GameTime * 19.0, 0.055);
    float packetWide = ported_pulse(vUv.x * 0.62 - GameTime * 11.5, 0.155);
    float scan = ported_pulse(vUv.x * 3.35 - GameTime * 7.0, 0.18);
    float orbital = 0.5 + 0.5 * cos((lane * 2.0 + vUv.x * 0.28 - GameTime * 5.4) * TAU);
    float coil = pow(ported_sat(0.5 + 0.5 * cos((lane * 2.35 + vUv.x * 2.15 - GameTime * 4.8) * TAU)), 2.35);
    float grain = ported_fbm(vec2(lane * 18.0 + GameTime * 21.0, vUv.x * 9.0 - GameTime * 8.0));
    float caustic = pow(ported_sat(0.42 + 0.58 * sin((lane * 5.0 + vUv.x * 1.7 - GameTime * 13.0 + grain * 0.65) * TAU)), 3.2);
    vec3 color = vec3(0.0);
    float alpha = vColor.a;
    if (layer < 1.0) {
        float shell = volume * 0.45 + fresnel * 0.65 + razor * 1.05 + packetWide * 0.20 + packet * 0.26 + coil * 0.16;
        color = accent * (0.45 + shell * 0.85) + hot * (packet * 0.55 + caustic * 0.18 + coil * 0.20);
        color += soft * razor * 0.55;
        alpha *= ported_sat(0.08 + volume * 0.30 + fresnel * 0.45 + packetWide * 0.14 + grain * 0.05) * (0.78 + orbital * 0.22);
    } else if (layer < 2.0) {
        vec3 glass = vec3(0.020, 0.016, 0.025) + base * 0.10;
        vec3 body = oklab_mix_srgb(glass, accent * 0.50, 0.30 + caustic * 0.14 + coil * 0.06);
        color = body + accent * fresnel * 0.65 + hot * razor * 0.70 + soft * packet * 0.22 + accent * coil * 0.18;
        alpha *= ported_sat(0.22 + fresnel * 0.55 + razor * 0.42 + caustic * 0.08 + packet * 0.07 + coil * 0.04);
    } else if (layer < 3.0) {
        float ringEdge = smoothstep(0.18, 0.50, abs(lane - 0.5));
        float arc = 0.42 + ported_pulse(vUv.x * 2.8 - GameTime * 15.0, 0.15) * 0.55;
        color = oklab_mix_srgb(accent, hot, 0.22 + packetWide * 0.18) * (0.65 + arc * 0.70 + fresnel * 0.30);
        color += soft * (razor * 0.55 + packet * 0.34);
        alpha *= ported_sat((ringEdge * 0.50 + fresnel * 0.32 + packetWide * 0.16) * arc);
    } else if (layer < 4.0) {
        float segment = 0.50 + fresnel * 0.55 + razor * 0.65;
        float strike = 0.60 + scan * 0.40 + packet * 0.22 + coil * 0.30;
        color = oklab_mix_srgb(accent, hot, 0.18 + razor * 0.28) * segment * strike;
        color += soft * caustic * 0.20;
        alpha *= ported_sat(0.30 + fresnel * 0.50 + scan * 0.20 + packet * 0.12 + coil * 0.12);
    } else {
        float radial = lane;
        float core = 1.0 - smoothstep(0.00, 0.85, radial);
        float aura = 1.0 - smoothstep(0.10, 1.00, radial);
        float rim = smoothstep(0.45, 1.00, radial) * (1.0 - smoothstep(0.92, 1.00, radial));
        float surge = 0.82 + 0.18 * sin((vUv.x * 2.0 - GameTime * 6.0 + grain * 0.28) * TAU);
        color = accent * (0.30 + aura * 0.85 + fresnel * 0.65) + hot * (core * 0.85 + rim * 0.50 + caustic * 0.18);
        color += soft * (razor * 0.65 + core * 0.22);
        alpha *= ported_sat((aura * 0.42 + core * 0.26 + fresnel * 0.55 + rim * 0.20) * surge);
    }
    color *= 0.96 + grain * 0.10;
    vec4 outColor = vec4(max(color, vec3(0.0)), ported_sat(alpha)) * ColorModulator;
    if (outColor.a <= 0.001) {
        discard;
    }
    fragColor = outColor;
}
