#version 150

uniform sampler2D Sampler0;
uniform vec4 uColor;
uniform float uPxRange;
uniform float uMtsdf;
// framebuffer-space: left, right, left fade width, right fade width.
// Negative bounds disable clipping for ordinary text draws.
uniform vec4 uClipFade;

in vec2 texCoord;
out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec4 texel = texture(Sampler0, texCoord);

    // MSDF: медиана трёх каналов → расстояние
    float msdfDistance = median(texel.r, texel.g, texel.b);

    // Размер пикселя в UV пространстве
    vec2 unitRange = uPxRange / vec2(textureSize(Sampler0, 0));
    vec2 uvDerivative = max(fwidth(texCoord), vec2(0.000001));
    vec2 screenTexSize = vec2(1.0) / uvDerivative;
    float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);

    // MTSDF stores a true SDF in alpha. It is more stable than the RGB
    // channel seams for tiny glyphs; full-size glyphs retain sharp MSDF corners.
    float smallText = (1.0 - smoothstep(1.75, 2.75, screenPxRange))
            * step(0.5, uMtsdf);
    float sd = mix(msdfDistance, texel.a, smallText);
    float screenPxDistance = screenPxRange * (sd - 0.5);
    float regularCoverage = clamp(screenPxDistance + 0.5, 0.0, 1.0);
    float smallCoverage = smoothstep(-0.65, 0.65, screenPxDistance);
    float opacity = mix(regularCoverage, smallCoverage, smallText);

    if (uClipFade.x >= 0.0 && uClipFade.y > uClipFade.x) {
        float px = gl_FragCoord.x;
        if (px < uClipFade.x || px > uClipFade.y) discard;
        float edgeAlpha = 1.0;
        if (uClipFade.z > 0.0) {
            edgeAlpha *= smoothstep(uClipFade.x, uClipFade.x + uClipFade.z, px);
        }
        if (uClipFade.w > 0.0) {
            edgeAlpha *= 1.0 - smoothstep(uClipFade.y - uClipFade.w, uClipFade.y, px);
        }
        opacity *= edgeAlpha;
    }

    if (opacity < 0.001) discard;

    fragColor = vec4(uColor.rgb, uColor.a * opacity);
}
