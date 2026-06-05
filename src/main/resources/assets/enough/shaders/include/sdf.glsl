// Функция скругленного прямоугольника с разными радиусами углов
// p - позиция пикселя (от центра)
// b - половина размера (width/2, height/2)
// r - радиусы углов (TopRight, BottomRight, TopLeft, BottomLeft)
float sdRoundedBox(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

// Простой круг
float sdCircle(vec2 p, float r) {
    return length(p) - r;
}