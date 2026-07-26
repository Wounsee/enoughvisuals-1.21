#version 330 core
in vec2 vUv;
in vec2 vLocal;
in vec2 vScreen;

uniform float uAlpha;
uniform float uCornerRadius;
uniform vec2 uRectSize;

out vec4 fragColor;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float roundedRectMask(vec2 local, vec2 size, float radius) {
    vec2 halfSize = size * 0.5;
    vec2 d = abs(local - halfSize) - (halfSize - vec2(radius));
    float outside = length(max(d, vec2(0.0))) + min(max(d.x, d.y), 0.0) - radius;
    return clamp(0.5 - outside, 0.0, 1.0);
}

void main() {
    float h = clamp(vUv.y, 0.0, 1.0);
    vec3 col = hsv2rgb(vec3(h, 1.0, 1.0));
    float radius = max(0.0, min(uCornerRadius, min(uRectSize.x, uRectSize.y) * 0.5));
    float mask = roundedRectMask(vLocal, uRectSize, radius);
    fragColor = vec4(col, clamp(uAlpha, 0.0, 1.0) * mask);
}
