#version 150

uniform sampler2D uTexture;
uniform vec2 uSize; // ширина и высота без паддинга
uniform vec4 uRadii; // радиусы углов
uniform float uAlpha;
uniform float uSmoothing;

in vec2 texCoord;
in vec2 localPos; // Координаты относительно самого элемента, не искажаются при наклоне!
out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 halfSize = uSize * 0.5;
    vec2 p = localPos - halfSize;

    float dist = roundedBoxSDF(p, halfSize, uRadii);
    float alphaDist = 1.0 - smoothstep(-uSmoothing, uSmoothing, dist);

    if (alphaDist <= 0.005) discard;

    vec4 texColor = texture(uTexture, texCoord);
    if (texColor.a <= 0.005) discard;

    fragColor = texColor * vec4(1.0, 1.0, 1.0, uAlpha * alphaDist);
}