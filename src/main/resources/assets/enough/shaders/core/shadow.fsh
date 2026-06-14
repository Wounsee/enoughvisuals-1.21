#version 150

out vec4 fragColor;

uniform vec2 uScreenSize;
uniform vec4 uRect;
uniform vec4 uRadii;
uniform vec4 uShadowColor;
uniform float uShadowSize;
uniform float uSmoothing;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    vec2 rr = (p.x > 0.0)
        ? ((p.y > 0.0) ? r.zw : r.yz)
        : ((p.y > 0.0) ? r.wx : r.xy);
    float radius = rr.x;
    vec2 q = abs(p) - b + vec2(radius);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

void main() {
    vec2 pixelPos = gl_FragCoord.xy;
    pixelPos.y = uScreenSize.y - pixelPos.y;

    vec2 center = uRect.xy + uRect.zw * 0.5;
    vec2 halfSize = uRect.zw * 0.5;
    vec2 p = pixelPos - center;

    float dist = roundedBoxSDF(p, halfSize, uRadii);
    float shadowSize = max(uShadowSize, 0.001);
    float outside = max(dist, 0.0);
    float t = clamp(outside / shadowSize, 0.0, 1.0);

    float gaussian = exp(-t * t * 5.25);
    float edgeFade = 1.0 - smoothstep(shadowSize * 0.72, shadowSize, outside);
    float outsideMask = smoothstep(-uSmoothing, uSmoothing, dist);
    float alpha = gaussian * edgeFade * outsideMask;

    fragColor = vec4(uShadowColor.rgb, uShadowColor.a * alpha);
}
