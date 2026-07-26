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

void main() {
    float u = clamp(vUv.x, 0.0, 1.0);
    float v = vUv.y;
    float t = GameTime * 1200.0;

    vec3 N = normalize(vNormal);
    vec3 V = normalize(-vViewPos);
    float ndv = clamp(abs(dot(N, V)), 0.0, 1.0);

    float face = mix(0.16, 0.70, ndv);
    float rim = pow(1.0 - ndv, 2.15);
    float grazing = pow(1.0 - ndv, 4.0);
    float side = abs(fract(v) - 0.5) * 2.0;
    float axial = 1.0 - smoothstep(0.12, 0.92, side);

    float dissolve = smoothstep(0.00, 0.82, u);
    float tailZone = 1.0 - smoothstep(0.00, 0.34, u);
    float tailWisp = 0.76 + 0.24 * sin((u * 8.0 - t * 0.036 + v * 3.25) * 6.2831853);
    float tailMod = mix(1.0, tailWisp, tailZone);

    float packetA = fract((1.0 - u) * 1.45 + t * 0.030 + sin(v * 6.2831853) * 0.035);
    float packetB = fract((1.0 - u) * 3.10 + t * 0.052 + v * 0.31);
    float causticG = (packetA - 0.52) / 0.105;
    float causticA = exp(-causticG * causticG);
    float causticH = (packetB - 0.50) / 0.070;
    float causticB = exp(-causticH * causticH);
    float filamentG = (side - 0.34 - 0.08 * sin(u * 9.0 - t * 0.04)) / 0.105;
    float filament = exp(-filamentG * filamentG);
    float silk = 0.5 + 0.5 * sin((u * 4.4 + v * 1.7 - t * 0.023) * 6.2831853);

    float breath = 0.95 + 0.05 * sin(t * 0.013);
    float headBoost = mix(1.0, 1.18, smoothstep(0.66, 1.0, u));

    vec3 theme = vColor.rgb;
    float life = clamp(vColor.a, 0.0, 1.0);

    vec3 hot = oklab_mix_srgb(theme, vec3(1.0), 0.56);
    vec3 cold = oklab_mix_srgb(theme * 0.44, vec3(0.42, 0.74, 1.0), 0.30);
    vec3 pearl = oklab_mix_srgb(hot, vec3(1.0), 0.42);
    vec3 surface = oklab_mix_srgb(cold, hot, face * 0.68 + axial * 0.18);
    vec3 packetCol = oklab_mix_srgb(theme, pearl, 0.68);
    float energy = causticA * (0.56 + axial * 0.64) + causticB * filament * 0.72 + silk * axial * 0.12;
    vec3 painted = surface * (0.82 + face * 0.28) + packetCol * energy * 0.78 + pearl * grazing * 0.82;
    painted *= breath * headBoost;

    float baseAlpha = face * 0.22 + rim * 0.42 + grazing * 0.36 + axial * 0.10 + energy * 0.18;
    float lifeEase = life * life * (3.0 - 2.0 * life);
    float alpha = baseAlpha * dissolve * tailMod * lifeEase;
    alpha *= 0.92 + causticA * 0.13 + causticB * 0.10;

    vec3 tone = painted / (painted + vec3(0.82));
    fragColor = vec4(clamp(tone, 0.0, 1.0), clamp(alpha, 0.0, 1.0)) * ColorModulator;
}
