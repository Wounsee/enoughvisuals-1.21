#version 150

uniform sampler2D Sampler0;
uniform vec4 uColor;
uniform float uPxRange;

in vec2 texCoord;
out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec4 texel = texture(Sampler0, texCoord);

    // MSDF: медиана трёх каналов → расстояние
    float sd = median(texel.r, texel.g, texel.b);

    // Размер пикселя в UV пространстве
    vec2 unitRange = uPxRange / vec2(textureSize(Sampler0, 0));
    vec2 screenTexSize = vec2(1.0) / fwidth(texCoord);
    float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);

    // Антиалиасинг через smoothstep
    float opacity = smoothstep(0.5 - 1.0 / screenPxRange, 0.5 + 1.0 / screenPxRange, sd);

    if (opacity < 0.001) discard;

    fragColor = vec4(uColor.rgb, uColor.a * opacity);
}