#version 330 core
layout(location = 0) in vec2 aPos;

uniform vec2 uViewport;
uniform vec4 uRect;
uniform vec4 u_ElementRect;

out vec2 vUv;
out vec2 vLocal;
out vec2 vScreen;

void main() {
    vec2 px = uRect.xy + aPos * uRect.zw;
    vec4 elementRect = u_ElementRect.z > 0.0 && u_ElementRect.w > 0.0 ? u_ElementRect : uRect;
    vec2 ndc = vec2(px.x / max(uViewport.x, 1.0) * 2.0 - 1.0, 1.0 - px.y / max(uViewport.y, 1.0) * 2.0);
    vLocal = px - elementRect.xy;
    vUv = vLocal / max(elementRect.zw, vec2(1.0));
    vScreen = px;
    gl_Position = vec4(ndc, 0.0, 1.0);
}
