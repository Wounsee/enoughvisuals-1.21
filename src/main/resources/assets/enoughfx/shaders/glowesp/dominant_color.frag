#version 330 core

in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uSource;

void main() {
    vec3 total = vec3(0.0);
    float weightTotal = 0.0;

    for (int y = 0; y < 12; y++) {
        for (int x = 0; x < 20; x++) {
            vec2 uv = (vec2(float(x), float(y)) + 0.5) / vec2(20.0, 12.0);
            vec4 sampleColor = texture(uSource, uv);
            float coverage = max(sampleColor.a, max(sampleColor.r, max(sampleColor.g, sampleColor.b)));
            float chroma = max(sampleColor.r, max(sampleColor.g, sampleColor.b))
                - min(sampleColor.r, min(sampleColor.g, sampleColor.b));
            float weight = step(0.02, coverage) * (0.45 + chroma * 1.8);
            total += sampleColor.rgb * weight;
            weightTotal += weight;
        }
    }

    vec3 color = weightTotal > 0.001 ? total / weightTotal : vec3(1.0);
    float peak = max(color.r, max(color.g, color.b));
    if (peak > 0.001) {
        color /= peak;
    }
    color = mix(vec3(dot(color, vec3(0.299, 0.587, 0.114))), color, 1.25);
    FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
