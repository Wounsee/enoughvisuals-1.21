#version 150

uniform float Time;
uniform float GlowStrength;

in vec2 TexCoord;
in vec4 vertexColor;
out vec4 OutColor;

float hash21(vec2 point) {
    point = fract(point * vec2(123.34, 456.21));
    point += dot(point, point + 45.32);
    return fract(point.x * point.y);
}

float valueNoise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);
    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float fbm(vec2 point) {
    float value = 0.0;
    float amplitude = 0.54;
    mat2 rotation = mat2(0.80, -0.60, 0.60, 0.80);
    for (int octave = 0; octave < 4; octave++) {
        value += valueNoise(point) * amplitude;
        point = rotation * point * 2.03 + 17.13;
        amplitude *= 0.49;
    }
    return value;
}

void main() {
    vec2 cell = floor(TexCoord + vec2(0.0001));
    vec2 local = fract(TexCoord) * 2.0 - 1.0;
    float seed = hash21(cell);

    vec2 drift = vec2(Time * 0.075, -Time * 0.052);
    vec2 domain = local * 2.15 + cell * 0.173 + drift;
    vec2 warp = vec2(
        fbm(domain + vec2(seed * 7.1, 2.8)),
        fbm(domain + vec2(5.2, seed * 9.3))
    ) - 0.5;
    float coarse = fbm(domain + warp * 1.35);
    float detail = fbm(domain * 1.82 - drift * 1.7 + warp * 0.55);

    vec2 cloudPoint = local + warp * 0.22;
    float radius = length(cloudPoint);
    float irregularEdge = 0.82 + (coarse - 0.5) * 0.34;
    float envelope = 1.0 - smoothstep(irregularEdge * 0.54, irregularEdge, radius);
    float wisps = smoothstep(0.22, 0.82, coarse * 0.72 + detail * 0.48);
    float hollows = smoothstep(0.12, 0.48, detail + coarse * 0.24);
    float density = envelope * wisps * (0.48 + hollows * 0.52);

    float glow = clamp(GlowStrength, 0.0, 2.5);
    vec3 tint = vertexColor.rgb;
    vec3 color = tint * (0.72 + coarse * 0.25);
    color = mix(color, tint + vec3(0.16), detail * (0.16 + glow * 0.035));
    float alpha = vertexColor.a * density * (0.78 + glow * 0.08);

    if (alpha <= 0.0015) discard;
    OutColor = vec4(color, clamp(alpha, 0.0, 0.72));
}
