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
uniform int FlowEnabled;
uniform float FlowSpeed;
uniform float FlowStrength;

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

float cloudVolumeMask(vec2 uv, float radius) {
    vec2 px = radius / vec2(textureSize(Sampler1, 0));
    float farField = 0.0;
    float nearField = handMask(uv) * 2.0;
    float farWeight = 0.0;
    float nearWeight = 2.0;
    for (int i = 0; i < 32; i++) {
        float a = float(i) * 2.39996;
        float ring = 0.22 + fract(float(i) * 0.618034) * 0.78;
        vec2 dir = vec2(cos(a) * 1.18, sin(a) * 1.04);
        float farW = mix(1.05, 0.45, ring);
        farField += handMask(uv + dir * px * ring) * farW;
        farWeight += farW;

        float nearRing = ring * 0.34;
        float nearW = mix(1.0, 0.55, nearRing);
        nearField += handMask(uv + dir * px * nearRing) * nearW;
        nearWeight += nearW;
    }

    float far = smoothstep(0.010, 0.085, farField / max(farWeight, 0.001));
    float repel = smoothstep(0.035, 0.18, nearField / max(nearWeight, 0.001));
    float outside = 1.0 - smoothstep(0.004, 0.045, handMask(uv));
    return smoothstep(0.02, 0.88, far * (1.0 - repel) * outside);
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

float flowBand(vec2 uv) {
    float vertical = fract((1.0 - uv.y) * 1.28 + Time * FlowSpeed);
    float band = 1.0 - abs(vertical * 2.0 - 1.0);
    float softBand = pow(clamp(band, 0.0, 1.0), 4.5);
    float shimmer = fbm(uv * 6.0 + vec2(Time * 0.15, -Time * 0.32));
    return softBand * mix(0.58, 1.0, shimmer);
}

float windCloud(vec2 uv) {
    float speed = max(FlowSpeed, 0.01);
    vec2 orbit = vec2(
        sin(Time * speed * 0.42 + uv.y * 6.2) * 0.045,
        cos(Time * speed * 0.31 + uv.x * 5.4) * 0.030
    );
    vec2 drift = vec2(Time * speed * 0.035, -Time * speed * 0.52);
    vec2 base = (uv + orbit) * vec2(2.35, 1.55) + drift;
    vec2 warp = vec2(
        fbm(base * 0.72 + vec2(2.7, 6.1)),
        fbm(base * 0.64 + vec2(8.4, 1.9))
    );
    vec2 q = base + (warp - 0.5) * 2.65;
    float broad = fbm(q * 0.68);
    float wisps = fbm(q * 1.18 + vec2(broad * 2.0, broad * 1.25));
    float folds = 1.0 - abs(fbm(q * 1.44 - drift * 1.25) * 2.0 - 1.0);
    float verticalFlow = pow(clamp(fract((1.0 - uv.y) * 0.42 + Time * speed * 0.21), 0.0, 1.0), 0.52);
    float density = smoothstep(0.42, 0.82, broad * 0.55 + wisps * 0.48 + folds * 0.26);
    return density * mix(0.42, 1.0, verticalFlow);
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
        float windArea = cloudVolumeMask(uv, BlurRadius * 42.0);
        float wind = FlowEnabled == 1 ? windCloud(uv) * windArea * FlowStrength : 0.0;
        vec3 edgeColor = UseItemColor == 1 ? nearbyItemColor(uv, BlurRadius * 2.0) : TintColor.rgb;
        if (mask > 0.01) {
            vec3 background = blurredBackground(uv, GlassBlur);
            vec3 glass = mix(background, GlassColor.rgb, glassTint);
            OutColor = vec4(glass, clamp(mask * outlineAlpha, 0.0, 1.0));
        } else if (wind > 0.004) {
            float softOutside = 1.0 - smoothstep(0.01, 0.15, mask);
            float cloudAlpha = pow(clamp(wind, 0.0, 1.0), 0.72) * outlineAlpha * softOutside;
            vec3 cloudShadow = mix(edgeColor * 0.12, vec3(0.12, 0.15, 0.25), 0.55);
            vec3 cloudLight = mix(edgeColor * 0.70, vec3(0.82, 0.88, 1.0), 0.70);
            vec3 cloudColor = mix(cloudShadow, cloudLight, smoothstep(0.18, 0.88, wind));
            OutColor = vec4(cloudColor, clamp(cloudAlpha * 1.35, 0.0, 0.72));
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
    } else {
        discard;
    }
}
