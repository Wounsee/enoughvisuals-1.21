#version 150

uniform float Time;
uniform float GlowStrength;

in vec2 TexCoord;
in vec4 vertexColor;
out vec4 OutColor;

void main() {
    vec2 edgeDistance2 = min(TexCoord, vec2(1.0) - TexCoord);
    float edgeDistance = min(edgeDistance2.x, edgeDistance2.y);
    float pixel = max(fwidth(edgeDistance), 0.0012);

    float outline = 1.0 - smoothstep(0.012 + pixel * 0.45, 0.026 + pixel * 1.55, edgeDistance);
    float halo = 1.0 - smoothstep(0.024 + pixel, 0.085 + pixel * 3.0, edgeDistance);
    float pulse = 0.97 + sin(Time * 1.7 + vertexColor.b * 4.0) * 0.03;

    vec3 color = mix(vertexColor.rgb * 0.72, vertexColor.rgb, outline);
    color = mix(color, vec3(1.0), outline * 0.2);
    float alpha = vertexColor.a * pulse
                * clamp(0.022 + outline * 0.74 + halo * 0.1 * GlowStrength, 0.0, 1.0);
    if (alpha <= 0.002) discard;
    OutColor = vec4(color, alpha);
}
