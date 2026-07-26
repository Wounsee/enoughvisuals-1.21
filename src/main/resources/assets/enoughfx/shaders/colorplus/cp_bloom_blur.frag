#version 330 core

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uScene;
uniform vec2  uResolution;
uniform vec2  uDirection;
uniform float uRadius;

const float WEIGHTS[9] = float[](
    0.227027,
    0.193596,
    0.150000,
    0.106200,
    0.068000,
    0.040000,
    0.022000,
    0.011000,
    0.005400
);

void main() {
    vec2 texel = 1.0 / max(uResolution, vec2(1.0));
    vec2 step = uDirection * texel * uRadius;

    vec3 col = texture(uScene, vUv).rgb * WEIGHTS[0];
    for (int i = 1; i < 9; i++) {
        vec2 off = step * float(i);
        col += texture(uScene, vUv + off).rgb * WEIGHTS[i];
        col += texture(uScene, vUv - off).rgb * WEIGHTS[i];
    }
    fragColor = vec4(col, 1.0);
}
