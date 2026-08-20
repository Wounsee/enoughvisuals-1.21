#version 150

uniform vec2 Size;
uniform float Radius;
uniform float Thickness;
uniform int RightSide;

in vec2 TexCoord;
in vec4 vertexColor;
out vec4 OutColor;

float segmentDistance(vec2 p, vec2 a, vec2 b) {
    vec2 ab = b - a;
    float t = clamp(dot(p - a, ab) / max(dot(ab, ab), 0.000001), 0.0, 1.0);
    return length(p - (a + ab * t));
}

void main() {
    vec2 p = vec2(TexCoord.x * Size.x, (1.0 - TexCoord.y) * Size.y);
    if (RightSide == 0) p.x = Size.x - p.x;

    float radius = min(Radius, min(Size.x, Size.y) * 0.48);
    vec2 center = vec2(Size.x - radius, radius);
    float horizontal = segmentDistance(p, vec2(0.0, 0.0), vec2(center.x, 0.0));
    float vertical = segmentDistance(p, vec2(Size.x, center.y), vec2(Size.x, Size.y));
    float arc = abs(length(p - center) - radius);
    if (p.x < center.x || p.y > center.y) arc = 1000.0;
    float dist = min(min(horizontal, vertical), arc) - Thickness * 0.5;
    float smoothing = max(fwidth(dist) * 1.8, 0.00018);
    float coverage = 1.0 - smoothstep(-smoothing, smoothing, dist);
    float alpha = vertexColor.a * coverage;
    if (alpha <= 0.002) discard;
    OutColor = vec4(vertexColor.rgb, alpha);
}
