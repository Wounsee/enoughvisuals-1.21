#version 150

uniform float Time;
uniform float GlowStrength;

in vec2 TexCoord;
in vec4 vertexColor;
out vec4 OutColor;

void main() {
    float across = abs(TexCoord.y * 2.0 - 1.0);
    float antialias = max(fwidth(across) * 1.5, 0.006);
    float body = 1.0 - smoothstep(0.38, 0.72 + antialias, across);
    float core = 1.0 - smoothstep(0.0, 0.27 + antialias, across);
    float roundedEnds = smoothstep(0.0, 0.075, TexCoord.x)
                      * smoothstep(0.0, 0.075, 1.0 - TexCoord.x);
    float pulse = 0.9 + sin(Time * 2.1 + TexCoord.x * 8.0) * 0.1;

    vec3 color = mix(vertexColor.rgb, vec3(1.0), core * 0.48);
    float alpha = vertexColor.a * roundedEnds * pulse
                * (body * 0.78 + core * 0.22 + (1.0 - across) * 0.08 * GlowStrength);
    if (alpha <= 0.002) discard;
    OutColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
