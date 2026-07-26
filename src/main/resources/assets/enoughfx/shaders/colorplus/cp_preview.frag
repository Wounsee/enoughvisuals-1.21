#version 330 core

in vec2 vUv;
in vec2 vLocal;
in vec2 vScreen;

uniform float uAlpha;
uniform float uCornerRadius;
uniform float uTime;
uniform float uLive;
uniform vec2 uRectSize;
uniform vec2 uMouse;
uniform vec4 uCurrentColor;
uniform vec4 uInitialColor;
uniform vec3 uAccentTop;
uniform vec3 uAccentBottom;

out vec4 fragColor;

float saturate(float v) {
    return clamp(v, 0.0, 1.0);
}

void main() {
    vec2 size = max(uRectSize, vec2(1.0));
    vec2 center = size * 0.5;
    float radius = max(0.5, min(min(size.x, size.y) * 0.5, uCornerRadius) - 0.55);
    vec2 p = vLocal - center;
    float dist = length(p);
    float aa = max(fwidth(dist), 0.75);
    float circle = smoothstep(radius + aa, radius - aa, dist);
    float inner = smoothstep(radius * 0.78, radius * 0.18, dist);
    float ring = smoothstep(aa * 2.6, 0.0, abs(dist - radius + aa * 0.35));
    float tile = clamp(radius * 0.56, 4.5, 8.0);
    vec2 checkerCell = floor(vLocal / tile);
    float checker = mod(checkerCell.x + checkerCell.y, 2.0);
    vec3 checkerColor = mix(vec3(0.72, 0.75, 0.73), vec3(0.91, 0.94, 0.91), checker);
    vec4 base = uCurrentColor;
    vec3 color = mix(checkerColor, base.rgb, base.a);
    vec2 mouse = clamp(uMouse, vec2(0.0), size) - center;
    float mouseEnergy = exp(-dot(mouse - p, mouse - p) / max(radius * radius * 0.72, 1.0));
    float angle = atan(p.y, p.x);
    float orbit = 0.5 + 0.5 * sin(angle * 2.0 + uTime * (2.2 + uLive * 0.8));
    float sweep = smoothstep(0.22, 0.0, abs(fract(angle / 6.2831853 + uTime * 0.18) - 0.5)) * (0.42 + uLive * 0.30);
    vec3 accent = mix(uAccentBottom, uAccentTop, orbit);
    vec3 initialGlow = mix(uInitialColor.rgb, accent, 0.35);
    color *= mix(0.78, 1.16, inner);
    color += accent * (ring * 0.18 + sweep * 0.10 + mouseEnergy * 0.16);
    color += initialGlow * ring * (0.10 + uLive * 0.08);
    color = max(color, checkerColor * (1.0 - base.a));
    fragColor = vec4(clamp(color, 0.0, 1.0), saturate(uAlpha) * circle);
}
