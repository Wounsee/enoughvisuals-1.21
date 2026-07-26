#version 150

uniform float Time;
uniform float GlowStrength;

in vec2 TexCoord;
in vec4 vertexColor;
out vec4 OutColor;

void main() {
    vec2 centered = TexCoord * 2.0 - 1.0;
    float distanceToCenter = length(centered);
    float antialias = max(fwidth(distanceToCenter) * 1.45, 0.004);

    float outer = 1.0 - smoothstep(0.72, 1.0 + antialias, distanceToCenter);
    float body = 1.0 - smoothstep(0.42, 0.69 + antialias, distanceToCenter);
    float core = 1.0 - smoothstep(0.0, 0.31 + antialias, distanceToCenter);
    float rim = smoothstep(0.68, 0.43, distanceToCenter)
              * smoothstep(0.2, 0.48, distanceToCenter);
    float pulse = 0.94 + sin(Time * 3.4 + vertexColor.r * 5.7) * 0.06;

    vec3 tint = vertexColor.rgb;
    vec3 color = mix(tint * 0.78, tint, body);
    color = mix(color, vec3(1.0), core * 0.82);
    color += tint * rim * 0.2 * GlowStrength;

    float alpha = vertexColor.a * pulse
                * clamp(outer * (0.15 + GlowStrength * 0.11) + body * 0.62 + core * 0.34, 0.0, 1.0);
    if (alpha <= 0.002) discard;
    OutColor = vec4(color, alpha);
}
