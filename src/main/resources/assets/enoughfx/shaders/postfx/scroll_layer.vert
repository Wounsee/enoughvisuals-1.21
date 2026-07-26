#version 330 core
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aUv;
uniform vec2 uViewport;
uniform vec2 uSize;
out vec2 vUv;
out vec2 vLocalPx;
out vec2 vPosPx;
void main() {
    vUv = aUv;
    vLocalPx = vec2(aUv.x * uSize.x, (1.0 - aUv.y) * uSize.y);
    vPosPx = aPos;
    vec2 ndc = vec2((aPos.x / uViewport.x) * 2.0 - 1.0, 1.0 - (aPos.y / uViewport.y) * 2.0);
    gl_Position = vec4(ndc, 0.0, 1.0);
}
