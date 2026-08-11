#version 150

uniform sampler2D Sampler0;
uniform vec2 TexelSize;
uniform vec2 Direction;
uniform float Radius;

in vec2 TexCoord;
out vec4 OutColor;

const float WEIGHTS[8] = float[](
    0.1345983481,
    0.1273245819,
    0.1077779317,
    0.0816380249,
    0.0553350352,
    0.0335623954,
    0.0182159056,
    0.0088469514
);

void main() {
    if (Radius < 0.01) {
        OutColor = texture(Sampler0, TexCoord);
        return;
    }

    vec4 color = texture(Sampler0, TexCoord) * WEIGHTS[0];
    float stepSize = max(Radius / 7.0, 0.42);
    for (int i = 1; i <= 7; i++) {
        float offset = float(i) * stepSize;
        vec2 delta = Direction * TexelSize * offset;
        color += texture(Sampler0, TexCoord + delta) * WEIGHTS[i];
        color += texture(Sampler0, TexCoord - delta) * WEIGHTS[i];
    }
    OutColor = color;
}
