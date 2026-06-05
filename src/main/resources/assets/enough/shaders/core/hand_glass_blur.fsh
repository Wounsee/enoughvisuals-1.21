#version 150

uniform sampler2D Sampler0;
uniform vec2 TexelSize;
uniform vec2 Direction;
uniform float Radius;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    if (Radius < 0.01) {
        OutColor = texture(Sampler0, TexCoord);
        return;
    }

    float centerWeight = 1.0;
    vec4 color = texture(Sampler0, TexCoord) * centerWeight;
    float weightSum = centerWeight;
    float stepSize = max(Radius / 7.0, 0.42);
    for (int i = 1; i <= 7; i++) {
        float offset = float(i) * stepSize;
        float weight = exp(-(float(i * i)) / 18.0);
        vec2 delta = Direction * TexelSize * offset;
        color += texture(Sampler0, TexCoord + delta) * weight;
        color += texture(Sampler0, TexCoord - delta) * weight;
        weightSum += weight * 2.0;
    }
    OutColor = color / weightSum;
}
