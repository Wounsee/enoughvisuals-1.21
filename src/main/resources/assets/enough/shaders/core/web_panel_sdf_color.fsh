#version 150

uniform vec2 Size;
uniform float Radius;
uniform float Exponent;

in vec2 TexCoord;
in vec4 vertexColor;
out vec4 OutColor;

float roundedBoxSdf(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    vec2 outside = max(q, 0.0);
    float power = max(Exponent, 2.0);
    float corner = pow(pow(outside.x, power) + pow(outside.y, power), 1.0 / power);
    return min(max(q.x, q.y), 0.0) + corner - radius;
}

void main() {
    vec2 p = (TexCoord - vec2(0.5)) * Size;
    float dist = roundedBoxSdf(p, Size * 0.5, min(Radius, min(Size.x, Size.y) * 0.5));
    float smoothing = max(fwidth(dist) * 1.15, 0.00015);
    float coverage = 1.0 - smoothstep(-smoothing, smoothing, dist);
    float alpha = vertexColor.a * coverage;
    if (alpha <= 0.002) discard;
    OutColor = vec4(vertexColor.rgb, alpha);
}
