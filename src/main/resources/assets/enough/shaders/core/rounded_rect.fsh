#version 150

out vec4 fragColor;

uniform vec2 uScreenSize;
uniform vec4 uRect;
uniform vec4 uRadii;     // tl, tr, br, bl
uniform vec4 uColor;
uniform float uSmoothing;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    // p.x > 0 = правая сторона, p.y > 0 = нижняя сторона (после flip Y)
    float radius;
    if (p.x > 0.0) {
        radius = (p.y > 0.0) ? r.z : r.y; // br : tr
    } else {
        radius = (p.y > 0.0) ? r.w : r.x; // bl : tl
    }

    radius = min(radius, min(b.x, b.y)); // clamp чтобы не выходил за bounds

    vec2 q = abs(p) - b + vec2(radius);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

void main() {
    vec2 pixelPos = gl_FragCoord.xy;
    pixelPos.y = uScreenSize.y - pixelPos.y;

    vec2 center  = uRect.xy + uRect.zw * 0.5;
    vec2 halfSize = uRect.zw * 0.5;
    vec2 p = pixelPos - center;

    float dist  = roundedBoxSDF(p, halfSize, uRadii);
    float alpha = 1.0 - smoothstep(-uSmoothing, uSmoothing, dist);

    fragColor = uColor * vec4(1.0, 1.0, 1.0, alpha);
}