#version 150

in vec2 TexCoord;
in vec2 LocalCoord;

uniform sampler2D Sampler0;
uniform vec2 PanelSize;   // panel size in GUI pixels
uniform float Radius;     // corner radius in GUI pixels
uniform vec4 TintColor;   // kept for json compatibility; tint is drawn in a separate pass
uniform float BlurAlpha;  // blur visibility 0..1

out vec4 fragColor;

// SDF for a rounded rectangle centered at origin
// b = half-size, r = corner radius
float sdRoundedBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    vec3 blurColor = texture(Sampler0, TexCoord).rgb + TintColor.rgb * 0.000001;

    vec2 pixelPos = LocalCoord * PanelSize;              // position in panel pixels
    vec2 center = PanelSize * 0.5;                        // center of panel
    float d = sdRoundedBox(pixelPos - center, center, Radius);

    float alpha = 1.0 - smoothstep(-1.0, 0.5, d);

    fragColor = vec4(blurColor, alpha * BlurAlpha);
}
