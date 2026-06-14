#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform float Threshold;
uniform float Radius;
uniform float Strength;
uniform float TrailDecay;
uniform float Lag;
uniform vec4 EchoColor;
uniform float Time;
uniform int CompositeMode;
uniform vec2 CameraShift;

in vec2 TexCoord;
in vec4 FragColor;
out vec4 OutColor;

float handMask(vec2 uv) {
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        return 0.0;
    }

    vec3 diff = abs(texture(Sampler0, uv).rgb - texture(Sampler1, uv).rgb);
    float amount = max(diff.r, max(diff.g, diff.b));
    return smoothstep(Threshold, Threshold + 0.08, amount * 8.0);
}

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
        f.y
    );
}

float haloMask(vec2 uv, float radius) {
    vec2 pixel = 1.0 / vec2(textureSize(Sampler0, 0));
    float center = handMask(uv);
    float glow = 0.0;
    float weight = 0.0;

    for (int i = 0; i < 28; i++) {
        float fi = float(i);
        float angle = fi * 2.39996323 + Time * 0.08;
        float ring = 0.22 + fract(fi * 0.6180339) * 0.78;
        vec2 dir = vec2(cos(angle) * 1.16, sin(angle) * 0.92);
        float w = mix(1.15, 0.42, ring);
        glow += handMask(uv + dir * pixel * radius * ring) * w;
        weight += w;
    }

    float outside = 1.0 - smoothstep(0.02, 0.72, center);
    return smoothstep(0.025, 0.28, glow / max(weight, 0.001)) * outside;
}

void main() {
    vec2 uv = TexCoord;

    if (CompositeMode == 1) {
        vec4 trail = texture(Sampler2, uv);
        float alpha = trail.a * clamp(Strength, 0.0, 1.8);
        if (alpha < 0.003) {
            discard;
        }
        OutColor = vec4(trail.rgb, clamp(alpha, 0.0, 0.9)) * FragColor;
        return;
    }

    vec2 pixel = 1.0 / vec2(textureSize(Sampler2, 0));
    vec2 shimmer = vec2(
        noise(uv * 13.0 + vec2(Time * 0.31, 4.7)) - 0.5,
        noise(uv * 11.0 + vec2(8.1, -Time * 0.27)) - 0.5
    );
    vec2 lagOffset = CameraShift + shimmer * pixel * Lag * 0.65;
    vec4 previous = texture(Sampler2, uv + lagOffset);

    float halo = haloMask(uv, Radius);
    float staticNoise = noise(uv * vec2(150.0, 95.0) + Time * 0.85);
    float softPulse = 0.80 + 0.20 * noise(uv * 7.0 + vec2(Time * 0.18, -Time * 0.22));
    float currentAlpha = pow(clamp(halo, 0.0, 1.0), 0.78)
            * clamp(Strength, 0.0, 1.8)
            * clamp(EchoColor.a, 0.0, 1.0)
            * mix(0.78, 1.18, staticNoise)
            * softPulse
            * 0.72;

    vec3 currentColor = mix(EchoColor.rgb * 0.45, vec3(1.0), 0.18 + halo * 0.22);
    vec4 current = vec4(currentColor, currentAlpha);
    vec4 decayed = previous * clamp(TrailDecay, 0.0, 0.965);

    float currentWeight = current.a * 0.34;
    float outAlpha = clamp(decayed.a * 0.96 + currentWeight, 0.0, 0.62);
    vec3 outColor = (decayed.rgb * decayed.a * 0.96 + current.rgb * currentWeight) / max(outAlpha, 0.001);
    OutColor = vec4(outColor, outAlpha);
}
