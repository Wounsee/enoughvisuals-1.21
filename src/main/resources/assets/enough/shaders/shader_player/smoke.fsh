#version 150

in vec2 texCoord;
out vec4 outColor;

uniform sampler2D MaskTexture;
uniform sampler2D TrailTexture;
uniform float time;
uniform vec4 baseColor;
uniform float effectAlpha;
uniform vec2 resolution;
uniform int useTextureColor;
uniform int outlineOnly;
uniform int composeMode;

mat2 rot(float angle) {
    return mat2(cos(angle), sin(angle), -sin(angle), cos(angle));
}

float hash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.11369, 0.13787));
    p += dot(p, p.yxz + 19.19);
    return fract((p.x + p.y) * p.z);
}

float noise(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);

    float n000 = hash(p + vec3(0.0, 0.0, 0.0));
    float n100 = hash(p + vec3(1.0, 0.0, 0.0));
    float n010 = hash(p + vec3(0.0, 1.0, 0.0));
    float n110 = hash(p + vec3(1.0, 1.0, 0.0));
    float n001 = hash(p + vec3(0.0, 0.0, 1.0));
    float n101 = hash(p + vec3(1.0, 0.0, 1.0));
    float n011 = hash(p + vec3(0.0, 1.0, 1.0));
    float n111 = hash(p + vec3(1.0, 1.0, 1.0));

    float x00 = mix(n000, n100, f.x);
    float x10 = mix(n010, n110, f.x);
    float x01 = mix(n001, n101, f.x);
    float x11 = mix(n011, n111, f.x);
    float y0 = mix(x00, x10, f.y);
    float y1 = mix(x01, x11, f.y);
    return mix(y0, y1, f.z) * 2.0 - 1.0;
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    vec3 q = p;

    for (int i = 0; i < 4; i++) {
        q.xy = vec2(q.x * 0.92 - q.y * 0.39, q.x * 0.39 + q.y * 0.92);
        q += time * vec3(0.17, -0.50, 0.11);
        value += amplitude * noise(q);
        q *= 2.1;
        amplitude *= 0.42;
    }
    return value;
}

float maskRaw(vec2 coord) {
    vec2 pixel = 1.0 / max(resolution, vec2(1.0));
    float mask = texture(MaskTexture, coord).a;
    mask = max(mask, texture(MaskTexture, coord + vec2(pixel.x, 0.0)).a);
    mask = max(mask, texture(MaskTexture, coord - vec2(pixel.x, 0.0)).a);
    mask = max(mask, texture(MaskTexture, coord + vec2(0.0, pixel.y)).a);
    mask = max(mask, texture(MaskTexture, coord - vec2(0.0, pixel.y)).a);
    return smoothstep(0.01, 0.18, mask);
}

float expandedMask(vec2 coord) {
    vec2 pixel = 1.0 / max(resolution, vec2(1.0));
    float mask = maskRaw(coord) * 1.35;

    const int sampleCount = 8;
    for (int i = 0; i < sampleCount; i++) {
        float angle = float(i) / float(sampleCount) * 6.2831853;
        vec2 direction = vec2(cos(angle), sin(angle));
        mask = max(mask, maskRaw(coord + direction * pixel * 4.0) * 0.92);
        mask = max(mask, maskRaw(coord + direction * pixel * 11.0) * 0.52);
    }
    return clamp(mask, 0.0, 1.0);
}

vec3 sampledTextureColor(vec2 coord) {
    vec2 pixel = 1.0 / max(resolution, vec2(1.0));
    vec4 centerSample = texture(MaskTexture, coord);
    float centerWeight = smoothstep(0.01, 0.18, centerSample.a) * 1.5;
    vec3 colorSum = centerSample.rgb * centerWeight;
    float weightSum = centerWeight;

    const int sampleCount = 8;
    for (int i = 0; i < sampleCount; i++) {
        float angle = float(i) / float(sampleCount) * 6.2831853;
        vec2 direction = vec2(cos(angle), sin(angle));
        vec4 sourceSample = texture(MaskTexture, clamp(coord + direction * pixel * 10.0, vec2(0.0), vec2(1.0)));
        float weight = smoothstep(0.01, 0.18, sourceSample.a);
        colorSum += sourceSample.rgb * weight;
        weightSum += weight;
    }

    if (weightSum <= 0.001) {
        return vec3(0.72);
    }
    return clamp(colorSum / weightSum, 0.0, 1.0);
}

vec4 buildCurrentSmoke(vec2 coord) {
    float mask = maskRaw(coord);
    float around = expandedMask(coord);
    if (around <= 0.001) {
        return vec4(0.0);
    }

    vec2 centered = (coord * 2.0 - 1.0) * vec2(resolution.x / max(resolution.y, 1.0), 1.0);
    float swirl = noise(vec3(centered * 1.8, time * 0.18));
    vec2 smokeUv = centered;
    smokeUv += vec2(sin(time * 0.7 + centered.y * 5.0),
                    cos(time * 0.55 + centered.x * 4.0)) * 0.035;
    smokeUv *= rot(swirl * 0.45);

    float density = fbm(vec3(smokeUv * 4.2, time * 0.22));
    float detail = noise(vec3(smokeUv * 7.4 + 4.7, time * 0.37));
    float wisps = smoothstep(-0.28, 0.64, density + detail * 0.34);

    float edge = outlineOnly == 1
            ? clamp((around - mask) * 1.65, 0.0, 1.0)
            : clamp(around - mask * 0.32, 0.0, 1.0);
    float pulse = 0.82 + 0.18 * sin(time * 2.1 + density * 3.0);
    float alpha = edge * wisps * pulse;
    alpha *= smoothstep(0.0, 0.18, around);
    alpha = clamp(alpha * 0.34, 0.0, 0.55);

    vec3 smokeColor = useTextureColor == 1 ? sampledTextureColor(coord) : baseColor.rgb;
    vec3 coolSmoke = useTextureColor == 1
            ? smokeColor * 0.58
            : mix(smokeColor * 0.55, vec3(0.78, 0.84, 0.92), 0.34);
    vec3 hotSmoke = useTextureColor == 1
            ? smokeColor * 1.28
            : mix(smokeColor * 1.35, vec3(1.0), 0.18);
    vec3 color = mix(coolSmoke, hotSmoke, clamp(wisps * around, 0.0, 1.0));
    return vec4(color, alpha);
}

void main() {
    if (composeMode == 1) {
        vec4 trail = texture(TrailTexture, texCoord);
        float alpha = trail.a * effectAlpha;
        if (alpha <= 0.003) {
            discard;
        }
        outColor = vec4(trail.rgb, alpha);
        return;
    }

    vec2 drift = vec2(
        sin(time * 0.8 + texCoord.y * 9.0) * 0.0018,
        -0.0042 - sin(time * 0.31 + texCoord.x * 10.0) * 0.0008
    );
    vec4 previous = texture(TrailTexture, clamp(texCoord + drift, vec2(0.001), vec2(0.999)));
    previous.a *= 0.925;
    previous.rgb *= 0.985;

    vec4 current = buildCurrentSmoke(texCoord);
    float alpha = clamp(previous.a + current.a * (1.0 - previous.a), 0.0, 0.82);
    vec3 color = previous.rgb;
    if (current.a > 0.001) {
        float mixValue = current.a / max(alpha, 0.001);
        color = mix(previous.rgb, current.rgb, clamp(mixValue, 0.0, 1.0));
    }

    if (alpha <= 0.002) {
        discard;
    }
    outColor = vec4(color, alpha);
}
