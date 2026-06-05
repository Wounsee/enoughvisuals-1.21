#version 150

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform vec2 Resolution;
uniform float Offset;

out vec4 fragColor;

void main() {
    vec4 sum = vec4(0.0);
    float weightSum = 0.0;
    for (int i = -5; i <= 5; i++) {
        float sampleOffset = float(i);
        float weight = exp(-(sampleOffset * sampleOffset) / 12.0);
        sum += texture(Sampler0, TexCoord + vec2(0.0, sampleOffset * Resolution.y * Offset)) * weight;
        weightSum += weight;
    }
    fragColor = vec4((sum / weightSum).rgb, 1.0);
}
