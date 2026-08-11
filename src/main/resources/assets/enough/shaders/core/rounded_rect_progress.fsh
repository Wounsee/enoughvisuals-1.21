#version 150

out vec4 fragColor;

uniform vec2 uScreenSize;
uniform vec4 uRect;
uniform vec4 uRadii;
uniform vec4 uColor;
uniform float uSmoothing;
uniform float uLineWidth;
uniform float uExponent;
uniform float uProgress;

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
    float ring = 1.0 - smoothstep(uLineWidth * 0.5 - uSmoothing,
                                  uLineWidth * 0.5 + uSmoothing, abs(dist));

    // atan(x, -y): zero at top and increasing clockwise.
    const float TAU = 6.28318530718;
    float phase = atan(p.x, -p.y) / TAU;
    if (phase < 0.0) phase += 1.0;
    float angularFeather = max(uSmoothing / max(max(halfSize.x, halfSize.y), 1.0), 0.001);
    float progressMask = 1.0 - smoothstep(uProgress - angularFeather, uProgress, phase);
    if (uProgress >= 0.9995) progressMask = 1.0;

    fragColor = uColor * vec4(1.0, 1.0, 1.0, ring * progressMask);
}
