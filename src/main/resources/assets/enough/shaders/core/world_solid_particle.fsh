#version 150

in vec2 TexCoord;
in vec4 vertexColor;
out vec4 OutColor;

void main() {
    vec2 centered = TexCoord * 2.0 - 1.0;
    float distanceToCenter = length(centered);
    float antialias = max(fwidth(distanceToCenter), 0.002);
    float coverage = 1.0 - smoothstep(1.0 - antialias, 1.0 + antialias, distanceToCenter);
    if (coverage <= 0.001) discard;
    OutColor = vec4(vertexColor.rgb, coverage);
}
