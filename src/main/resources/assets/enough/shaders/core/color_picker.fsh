#version 150

out vec4 fragColor;

uniform vec2 uScreenSize;
uniform vec4 uRect;
uniform vec4 uHueColor;
uniform float uRadius;
uniform float uOpacity;
uniform float uSmoothing;

float roundedBoxSDF(vec2 p, vec2 b, float radius) {
    vec2 q = abs(p) - b + vec2(radius);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

void main() {
    vec2 pixelPos = vec2(gl_FragCoord.x, uScreenSize.y - gl_FragCoord.y);
    vec2 uv = clamp((pixelPos - uRect.xy) / uRect.zw, 0.0, 1.0);
    vec3 saturated = mix(vec3(1.0), uHueColor.rgb, uv.x);
    vec3 color = saturated * (1.0 - uv.y);

    vec2 halfSize = uRect.zw * 0.5;
    float dist = roundedBoxSDF(pixelPos - (uRect.xy + halfSize), halfSize,
            min(uRadius, min(halfSize.x, halfSize.y)));
    float coverage = 1.0 - smoothstep(-uSmoothing, uSmoothing, dist);
    fragColor = vec4(color, coverage * uOpacity);
}
