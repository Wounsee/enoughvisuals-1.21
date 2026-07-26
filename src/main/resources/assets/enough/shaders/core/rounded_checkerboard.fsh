#version 150

out vec4 fragColor;

uniform vec2 uScreenSize;
uniform vec4 uRect;
uniform float uRadius;
uniform float uCellSize;
uniform float uOpacity;
uniform float uSmoothing;

float roundedBoxSDF(vec2 p, vec2 b, float radius) {
    vec2 q = abs(p) - b + vec2(radius);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

void main() {
    vec2 pixelPos = vec2(gl_FragCoord.x, uScreenSize.y - gl_FragCoord.y);
    vec2 local = pixelPos - uRect.xy;
    float cell = max(uCellSize, 1.0);
    float parity = mod(floor(local.x / cell) + floor(local.y / cell), 2.0);
    vec3 lightColor = vec3(0.78, 0.76, 0.83);
    vec3 darkColor = vec3(0.39, 0.37, 0.45);

    vec2 halfSize = uRect.zw * 0.5;
    float radius = min(uRadius, min(halfSize.x, halfSize.y));
    float dist = roundedBoxSDF(pixelPos - (uRect.xy + halfSize), halfSize, radius);
    float coverage = 1.0 - smoothstep(-uSmoothing, uSmoothing, dist);
    fragColor = vec4(mix(lightColor, darkColor, parity), coverage * uOpacity);
}
