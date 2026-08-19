#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform float BlurRadius;
uniform float Threshold;
uniform vec4 TintColor;
uniform vec4 GlassColor;
uniform int Mode;
uniform float GlassBlur;
uniform float GlassZoom;
uniform float GlowStrength;
uniform int UseItemColor;
uniform float Time;
uniform float WebOpacity;
uniform float WebGlow;

in vec2 TexCoord;
in vec4 FragColor;
out vec4 OutColor;

float handMask(vec2 uv) {
    vec3 d = abs(texture(Sampler0, uv).rgb - texture(Sampler2, uv).rgb);
    return smoothstep(Threshold, Threshold + 0.08, max(d.r, max(d.g, d.b)) * 8.0);
}

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += noise(p) * a;
        p = p * 2.02 + vec2(8.4, 5.7);
        a *= 0.5;
    }
    return v;
}

float ridged(vec2 p) {
    float v = 0.0;
    float a = 0.55;
    for (int i = 0; i < 4; i++) {
        float r = 1.0 - abs(noise(p) * 2.0 - 1.0);
        v += r * a;
        p = p * 2.18 + vec2(3.1, 9.2);
        a *= 0.52;
    }
    return v;
}

float outerGlow(vec2 uv, float radius) {
    vec2 gm = radius / vec2(textureSize(Sampler1, 0));
    float gs = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7854;
        vec2 dir = vec2(cos(a), sin(a));
        gs += handMask(uv + dir * gm * 0.5) + handMask(uv + dir * gm);
    }
    return (gs / 16.0) * (1.0 - handMask(uv));
}

vec3 blurredBackground(vec2 uv, float radius) {
    float zoom = 1.0 + clamp(GlassZoom, 0.0, 10.0) * 0.04;
    vec2 zoomed = vec2(0.5) + (uv - vec2(0.5)) / zoom;
    return texture(Sampler1, clamp(zoomed, vec2(0.001), vec2(0.999))).rgb;
}

vec3 nearbyItemColor(vec2 uv, float radius) {
    vec2 px = radius / vec2(textureSize(Sampler0, 0));
    vec3 col = texture(Sampler0, uv).rgb * handMask(uv) * 2.0;
    float weight = handMask(uv) * 2.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7854;
        vec2 dir = vec2(cos(a), sin(a));
        float sampleMask = handMask(uv + dir * px);
        col += texture(Sampler0, uv + dir * px).rgb * sampleMask;
        weight += sampleMask;
    }
    return weight > 0.001 ? col / weight : TintColor.rgb;
}

float innerEdge(vec2 uv) {
    vec2 px = 1.5 / vec2(textureSize(Sampler0, 0));
    float edge = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7854;
        edge = max(edge, handMask(uv) - handMask(uv + vec2(cos(a), sin(a)) * px));
    }
    return clamp(edge, 0.0, 1.0);
}

void main() {
    vec2 uv = TexCoord;
    float mask = handMask(uv);
    float outlineStrength = clamp(GlowStrength, 0.0, 1.0);
    float outlineAlpha = clamp(TintColor.a, 0.0, 1.0);
    float glassTint = clamp(GlassColor.a, 0.0, 0.65);

    if (Mode == 0) {
        if (mask < 0.01) discard;
        vec2 mult = BlurRadius / vec2(textureSize(Sampler1, 0));
        vec3 blurred = vec3(0.0);
        for (int i = 0; i < 8; i++) {
            float a = float(i) * 0.7854;
            vec2 dir = vec2(cos(a), sin(a));
            blurred += texture(Sampler1, uv + dir * mult * 0.33).rgb;
            blurred += texture(Sampler1, uv + dir * mult * 0.66).rgb;
            blurred += texture(Sampler1, uv + dir * mult).rgb;
        }
        blurred /= 24.0;
        OutColor = vec4(mix(blurred, TintColor.rgb, TintColor.a), mask * TintColor.a);
    } else if (Mode == 1) {
        vec3 edgeColor = UseItemColor == 1 ? nearbyItemColor(uv, BlurRadius * 2.0) : TintColor.rgb;
        if (mask > 0.01) {
            vec3 background = blurredBackground(uv, GlassBlur);
            vec3 glass = mix(background, GlassColor.rgb, glassTint);
            OutColor = vec4(glass, clamp(mask * outlineAlpha, 0.0, 1.0));
        } else {
            discard;
        }
    } else if (Mode == 2) {
        float glow = outerGlow(uv, BlurRadius * 2.25) * WebGlow;
        if (mask < 0.01 && glow < 0.005) discard;
        vec2 flow = uv * 2.5;
        vec2 drift = vec2(Time * 0.20, -Time * 0.15);
        vec2 warp = vec2(fbm(flow * 0.90 + drift * 0.75 + vec2(0.0, 4.1)),
                         fbm(flow * 0.78 - drift * 0.48 + vec2(3.7, 1.8)));
        vec2 q = flow + (warp - 0.5) * 1.8;
        float mist = fbm(q * 0.72 - drift * 0.24 + vec2(4.2, 8.1));
        float veins = pow(clamp(ridged(q * 1.85 + vec2(mist * 2.5, mist * 1.6) - drift * 0.55), 0.0, 1.0), 2.4);
        float energy = clamp(mist * 0.22 + veins * 0.88, 0.0, 1.0);
        vec3 col = mix(TintColor.rgb, mix(TintColor.rgb, vec3(1.0), 0.4), smoothstep(0.18, 0.98, energy));
        if (mask > 0.01) {
            OutColor = vec4(col * (0.35 + energy), clamp(mask * outlineAlpha * WebOpacity, 0.0, 1.0));
        } else {
            vec3 glowColor = mix(TintColor.rgb, vec3(1.0), 0.22 + energy * 0.28);
            OutColor = vec4(glowColor, clamp(pow(glow, 0.55) * outlineAlpha, 0.0, 1.0));
        }
    } else if (Mode == 3) { // Aurora
        if (mask < 0.01) discard;
        vec2 auv = uv * 3.0;
        float w1 = sin(auv.x * 2.0 + Time * 2.0 + fbm(auv * 1.2));
        float w2 = cos(auv.y * 1.5 - Time * 1.5 + fbm(auv * 0.9));
        float wave = pow(clamp(0.5 + 0.5 * (w1 * 0.6 + w2 * 0.4), 0.0, 1.0), 2.0);
        vec3 col = mix(TintColor.rgb * 0.2, mix(TintColor.rgb, vec3(0.3, 1.0, 0.8), 0.7), wave);
        OutColor = vec4(col, mask * outlineAlpha);
    } else if (Mode == 4) { // Stars
        if (mask < 0.01) discard;
        vec2 suv = uv * 6.0;
        float starField = pow(hash(floor(suv * 8.0)), 16.0) * (0.5 + 0.5 * sin(Time * 4.0 + suv.x * 10.0));
        float neb = fbm(suv * 0.4 + vec2(Time * 0.05, -Time * 0.03));
        vec3 col = mix(TintColor.rgb * 0.3, TintColor.rgb, neb) + vec3(starField * 1.5);
        OutColor = vec4(col, mask * outlineAlpha);
    } else if (Mode == 5) { // Glow
        if (mask < 0.01) discard;
        vec2 guv = (uv - vec2(0.5)) * 4.0;
        float g = 0.5 + 0.5 * sin(length(guv) * 2.0 - Time * 3.0);
        vec3 col = mix(TintColor.rgb * 0.35, mix(TintColor.rgb, vec3(1.0), 0.8), pow(g, 2.2));
        OutColor = vec4(col, mask * outlineAlpha);
    } else if (Mode == 6) { // Caustic
        if (mask < 0.01) discard;
        vec2 cuv = uv * 4.0;
        float c1 = sin(cuv.x * 3.0 + Time * 2.0) + sin(cuv.y * 3.0 + Time * 2.0);
        float c2 = sin((cuv.x + cuv.y) * 2.5 - Time * 1.5);
        float caust = pow(clamp(0.5 + 0.25 * (c1 + c2), 0.0, 1.0), 3.0);
        vec3 col = mix(TintColor.rgb * 0.3, mix(TintColor.rgb, vec3(1.0), 0.85), caust);
        OutColor = vec4(col, mask * outlineAlpha);
    } else if (Mode == 7) { // Clouds
        if (mask < 0.01) discard;
        float cloud = fbm(uv * 3.0 + vec2(Time * 0.1, Time * 0.06));
        vec3 col = mix(TintColor.rgb * 0.3, mix(TintColor.rgb, vec3(0.95, 0.98, 1.0), 0.6), smoothstep(0.3, 0.75, cloud));
        OutColor = vec4(col, mask * outlineAlpha);
    } else {
        discard;
    }
}
