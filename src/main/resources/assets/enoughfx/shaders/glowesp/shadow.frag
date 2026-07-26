#version 330 core

in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uSource;
uniform vec2 uTexelSize;
uniform vec2 uDirection;
uniform int uRadius;
uniform float uKernel[64];

void main() {
    float blurred = texture(uSource, vUv).a * uKernel[0];
    vec2 stepOffset = uTexelSize * uDirection;

    for (int i = 1; i < 64; i++) {
        if (i > uRadius) {
            break;
        }
        vec2 offset = stepOffset * float(i);
        float weight = uKernel[i];
        blurred += texture(uSource, vUv - offset).a * weight;
        blurred += texture(uSource, vUv + offset).a * weight;
    }

    FragColor = vec4(blurred, blurred, blurred, blurred);
}
