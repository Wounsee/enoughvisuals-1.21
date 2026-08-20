#version 150

out vec4 fragColor;

uniform vec2 uScreenSize;
uniform vec2 uCenter;
uniform vec2 uRadius;    // rx, ry
uniform vec4 uColor;
uniform float uSmoothing;
uniform float uLineWidth; // 0 = filled
uniform float uProgress;  // <--- НОВОЕ: Прогресс от 0.0 до 1.0
uniform float uArcMode;
uniform float uArcStart;
uniform float uArcSweep;

#define PI 3.14159265359

void main() {
    vec2 pixelPos = gl_FragCoord.xy;
    pixelPos.y = uScreenSize.y - pixelPos.y;

    vec2 d = (pixelPos - uCenter) / uRadius;
    float dist = length(d) - 1.0;
    float pixelDist = dist * max(uRadius.x, uRadius.y);

    float alpha;
    if (uLineWidth > 0.0) {
        float halfLine = uLineWidth * 0.5;
        alpha = 1.0 - smoothstep(-uSmoothing, uSmoothing, abs(pixelDist) - halfLine);
    } else {
        alpha = 1.0 - smoothstep(-uSmoothing, uSmoothing, pixelDist);
    }

    if (uArcMode > 0.5) {
        float angle = atan(d.y, d.x);
        if (angle < 0.0) angle += 2.0 * PI;
        float delta = mod(angle - uArcStart + 2.0 * PI, 2.0 * PI);
        float angularAa = max(uSmoothing / max(max(uRadius.x, uRadius.y), 0.001), 0.008);
        float startMask = smoothstep(0.0, angularAa, delta);
        float endMask = 1.0 - smoothstep(uArcSweep - angularAa, uArcSweep + angularAa, delta);
        alpha *= startMask * endMask;
    } else if (uProgress < 1.0) {
        // Вырезаем дугу для анимации КД
        float angle = atan(d.x, d.y); // 0 градусов - ровно сверху
        if (angle < 0.0) angle += 2.0 * PI;
        float normalizedAngle = angle / (2.0 * PI);

        // Плавный срез края дуги
        float arcAlpha = 1.0 - smoothstep(uProgress - 0.01, uProgress + 0.01, normalizedAngle);
        alpha *= arcAlpha;
    }

    if (alpha <= 0.005) discard;
    fragColor = uColor * vec4(1.0, 1.0, 1.0, alpha);
}
