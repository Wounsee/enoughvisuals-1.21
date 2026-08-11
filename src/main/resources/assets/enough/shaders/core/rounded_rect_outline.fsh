#version 150

out vec4 fragColor;

uniform vec2 uScreenSize;
uniform vec4 uRect;
uniform vec4 uRadii;
uniform vec4 uColor;
uniform float uSmoothing;
uniform float uLineWidth;
uniform float uExponent;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    vec2 rr = (p.x > 0.0)
        ? ((p.y > 0.0) ? r.zw : r.yz)
        : ((p.y > 0.0) ? r.wx : r.xy);
    float radius = rr.x;
    vec2 q = abs(p) - b + vec2(radius);
    vec2 outside = max(q, 0.0);
    float exponent = max(uExponent, 2.0);
    float corner = pow(pow(outside.x, exponent) + pow(outside.y, exponent), 1.0 / exponent);
    return min(max(q.x, q.y), 0.0) + corner - radius;
}

void main() {
    vec2 pixelPos = gl_FragCoord.xy;
    pixelPos.y = uScreenSize.y - pixelPos.y;

    vec2 center = uRect.xy + uRect.zw * 0.5;
    vec2 halfSize = uRect.zw * 0.5;
    vec2 p = pixelPos - center;

    float dist = roundedBoxSDF(p, halfSize, uRadii);

    float halfLine = uLineWidth * 0.5;
    float alpha = 1.0 - smoothstep(-uSmoothing, uSmoothing, abs(dist) - halfLine);

    fragColor = uColor * vec4(1.0, 1.0, 1.0, alpha);
}
