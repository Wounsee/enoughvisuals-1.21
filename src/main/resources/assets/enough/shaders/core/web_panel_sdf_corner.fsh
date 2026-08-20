#version 150

uniform vec2 Size;
uniform float Radius;
uniform float Thickness;
uniform float Padding;
uniform int RightSide;

in vec2 TexCoord;
in vec4 vertexColor;
out vec4 OutColor;

void main() {
    vec2 pathSize = max(Size - vec2(Padding * 2.0), vec2(0.0001));
    vec2 p = vec2(TexCoord.x * Size.x, (1.0 - TexCoord.y) * Size.y) - vec2(Padding);
    if (RightSide == 0) p.x = pathSize.x - p.x;

    float radius = min(Radius, min(pathSize.x, pathSize.y) * 0.48);
    vec2 center = vec2(pathSize.x - radius, radius);

    // A single piecewise centreline keeps the stroke exactly the same width
    // through both tangent joins. Taking the union of three capsules caused
    // the visible blob/kink at the bend.
    float centerline;
    if (p.x <= center.x) {
        centerline = length(p - vec2(clamp(p.x, 0.0, center.x), 0.0));
    } else if (p.y >= center.y) {
        centerline = length(p - vec2(pathSize.x, clamp(p.y, center.y, pathSize.y)));
    } else {
        centerline = abs(length(p - center) - radius);
    }

    float dist = centerline - Thickness * 0.5;
    float smoothing = max(fwidth(dist) * 0.80, 0.00010);
    float coverage = 1.0 - smoothstep(-smoothing, smoothing, dist);
    float alpha = vertexColor.a * coverage;
    if (alpha <= 0.002) discard;
    OutColor = vec4(vertexColor.rgb, alpha);
}
