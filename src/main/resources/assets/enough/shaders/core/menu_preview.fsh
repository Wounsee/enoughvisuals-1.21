#version 150

uniform sampler2D uTexture;
uniform vec2 uSize;
uniform vec4 uRadii;
uniform float uAlpha;
uniform float uSmoothing;
uniform float uShadeStrength;

in vec2 texCoord;
in vec2 localPos;
out vec4 fragColor;

float roundedBoxSdf(vec2 point, vec2 halfSize, vec4 radii) {
    radii.xy = (point.x > 0.0) ? radii.xy : radii.zw;
    radii.x = (point.y > 0.0) ? radii.x : radii.y;
    vec2 edge = abs(point) - halfSize + radii.x;
    return min(max(edge.x, edge.y), 0.0) + length(max(edge, 0.0)) - radii.x;
}

void main() {
    vec2 halfSize = uSize * 0.5;
    float distanceToEdge = roundedBoxSdf(localPos - halfSize, halfSize, uRadii);
    float antialias = max(fwidth(distanceToEdge), uSmoothing * 0.35);
    float mask = 1.0 - smoothstep(-antialias, antialias, distanceToEdge);
    if (mask <= 0.003) discard;

    vec4 sampled = texture(uTexture, texCoord);
    if (sampled.a <= 0.003) discard;

    float vertical = clamp(localPos.y / max(uSize.y, 0.001), 0.0, 1.0);
    float shade = smoothstep(0.40, 1.0, vertical);
    shade *= shade * uShadeStrength;
    sampled.rgb *= 1.0 - shade;
    fragColor = vec4(sampled.rgb, sampled.a * mask * uAlpha);
}
