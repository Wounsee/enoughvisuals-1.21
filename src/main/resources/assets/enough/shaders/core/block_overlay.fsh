#version 150

uniform float Time;
uniform int Mode;
uniform vec4 TintColor;

in vec2 TexCoord;
in vec4 vertexColor;
out vec4 OutColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += noise(p) * a;
        p = p * 2.02 + vec2(8.4, 5.7);
        a *= 0.5;
    }
    return v;
}

float ridged(vec2 p) {
    float v = 0.0;
    float a = 0.55;
    for (int i = 0; i < 4; i++) {
        float r = 1.0 - abs(noise(p) * 2.0 - 1.0);
        v += r * a;
        p = p * 2.18 + vec2(3.1, 9.2);
        a *= 0.52;
    }
    return v;
}

void main() {
    vec2 uv = TexCoord * 7.5;
    vec3 col = TintColor.rgb;

    if (Mode == 1) {
        float pulse = 0.62 + sin((uv.x + uv.y) * 2.2 + Time * 2.0) * 0.18;
        float grain = fbm(uv * 0.9 + vec2(Time * 0.12, -Time * 0.08));
        col = mix(col * 0.45, mix(col, vec3(1.0), 0.45), clamp(pulse + grain * 0.35, 0.0, 1.0));
    } else if (Mode == 2) {
        vec2 drift = vec2(Time * 0.20, -Time * 0.15);
        vec2 warp = vec2(fbm(uv * 0.34 + drift), fbm(uv * 0.29 - drift + vec2(3.7, 1.8)));
        vec2 q = uv + (warp - 0.5) * 2.4;
        float mist = fbm(q * 0.28 - drift);
        float veins = pow(clamp(ridged(q * 0.82 + vec2(mist * 2.5, mist * 1.6)), 0.0, 1.0), 2.4);
        float energy = clamp(mist * 0.22 + veins * 0.88, 0.0, 1.0);
        col = mix(col * 0.25, mix(col, vec3(1.0), 0.5), energy);
    } else if (Mode == 3) {
        float p1 = sin(uv.x * 2.8 + Time * 2.0);
        float p2 = sin(uv.y * 2.3 + Time * 1.5);
        float p3 = sin((uv.x + uv.y) * 1.8 + Time * 1.8);
        float energy = clamp((p1 + p2 + p3) * 0.16 + 0.55, 0.0, 1.0);
        col = mix(col * 0.35, mix(col, vec3(1.0), 0.68), energy);
    } else if (Mode == 4) { // Aurora
        float w1 = sin(uv.x * 1.5 + Time * 1.8 + fbm(uv * 0.8));
        float w2 = cos(uv.y * 1.2 - Time * 1.4 + fbm(uv * 0.6 + vec2(Time * 0.2, 0.0)));
        float wave = pow(clamp(0.5 + 0.5 * (w1 * 0.6 + w2 * 0.4), 0.0, 1.0), 2.0);
        col = mix(col * 0.3, mix(col, vec3(0.3, 1.0, 0.8), 0.7), wave);
    } else if (Mode == 5) { // Stars
        float starField = pow(hash(floor(uv * 12.0)), 16.0) * (0.5 + 0.5 * sin(Time * 4.0 + uv.x * 10.0));
        float neb = fbm(uv * 0.4 + vec2(Time * 0.05, -Time * 0.03));
        col = mix(col * 0.3, col, neb) + vec3(starField * 1.5);
    } else if (Mode == 6) { // Snow
        vec2 suv = uv + vec2(sin(Time * 0.8 + uv.y * 2.0) * 0.3, Time * 1.5);
        float flake = pow(hash(floor(suv * 8.0)), 12.0);
        col = mix(col * 0.6, vec3(0.9, 0.95, 1.0), flake);
    } else if (Mode == 7) { // Glow
        float g = 0.5 + 0.5 * sin(length(uv - vec2(3.75)) * 1.5 - Time * 3.0);
        col = mix(col * 0.4, mix(col, vec3(1.0), 0.8), pow(g, 2.2));
    } else if (Mode == 8) { // Caustic
        vec2 cp = uv * 0.8;
        float c1 = sin(cp.x * 3.0 + Time * 2.0) + sin(cp.y * 3.0 + Time * 2.0);
        float c2 = sin((cp.x + cp.y) * 2.5 - Time * 1.5);
        float caust = pow(clamp(0.5 + 0.25 * (c1 + c2), 0.0, 1.0), 3.0);
        col = mix(col * 0.4, mix(col, vec3(1.0), 0.85), caust);
    } else if (Mode == 9) { // Clouds
        float cloud = fbm(uv * 0.35 + vec2(Time * 0.1, Time * 0.06));
        col = mix(col * 0.35, mix(col, vec3(0.95, 0.98, 1.0), 0.6), smoothstep(0.3, 0.75, cloud));
    } else {
        float row = floor(uv.y * 8.0);
        float shift = step(0.82, hash(vec2(row, floor(Time * 8.0)))) * 0.5;
        float scan = 0.78 + 0.22 * sin(uv.y * 32.0);
        float split = hash(vec2(floor((uv.x + shift) * 5.0), row));
        col = mix(col * scan, vec3(col.r, col.g * 0.45, min(1.0, col.b * 1.4)), step(0.75, split));
    }

    OutColor = vec4(col, TintColor.a * vertexColor.a);
}
