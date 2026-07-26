#version 330 core
in vec2 vUv;
in vec2 vLocalPx;
in vec2 vPosPx;
uniform sampler2D uSource;
uniform vec2 uTextureSize;
uniform vec2 uSize;
uniform vec4 uRadii;
uniform vec4 uClipRect;
uniform vec4 uClipRadii;
uniform float uFadePx;
uniform float uEdgeBlurPx;
uniform float uMotionBlurPx;
uniform float uMotionStrength;
uniform float uFocusStrength;
uniform float uDirection;
uniform float uAlpha;
out vec4 fragColor;

float radiusAt(vec2 p, vec4 r) {
    return p.x > 0.0 ? (p.y > 0.0 ? r.z : r.y) : (p.y > 0.0 ? r.w : r.x);
}

float sdRoundBox(vec2 p, vec2 halfSize, vec4 radii) {
    vec4 safeRadii = min(max(radii, vec4(0.0)), min(halfSize.x, halfSize.y));
    float rad = radiusAt(p, safeRadii);
    vec2 q = abs(p) - halfSize + rad;
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - rad;
}

float coverage(float distanceValue) {
    float px = max(fwidth(distanceValue) * 0.7071, 0.0001);
    return smoothstep(px, -px, distanceValue);
}

float smootherstep01(float t) {
    t = clamp(t, 0.0, 1.0);
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}

float interleavedGradientNoise(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}

vec4 sampleLayer(vec2 uv) {
    return texture(uSource, clamp(uv, vec2(0.0), vec2(1.0)));
}

void main() {
    if (uClipRect.z <= 0.0 || uClipRect.w <= 0.0) {
        discard;
    }
    if (vPosPx.x < uClipRect.x || vPosPx.y < uClipRect.y ||
        vPosPx.x >= uClipRect.x + uClipRect.z ||
        vPosPx.y >= uClipRect.y + uClipRect.w) {
        discard;
    }

    float motion = smootherstep01(clamp(uMotionStrength, 0.0, 1.0));
    float dirSign = uDirection < 0.0 ? -1.0 : (uDirection > 0.0 ? 1.0 : 0.0);

    float baseBand = max(uFadePx, 1.0);
    float topBand = baseBand * (1.0 + 0.45 * max(dirSign, 0.0) * motion);
    float bottomBand = baseBand * (1.0 + 0.45 * max(-dirSign, 0.0) * motion);
    float topT = clamp(vLocalPx.y / topBand, 0.0, 1.0);
    float bottomT = clamp((uSize.y - vLocalPx.y) / bottomBand, 0.0, 1.0);
    float edgeMask = smootherstep01(topT) * smootherstep01(bottomT);

    float edgeNear = 1.0 - min(topT, bottomT);
    float meltPx = max(uEdgeBlurPx, 0.0) * edgeNear * edgeNear;
    float blurPx = max(uMotionBlurPx, 0.0) + meltPx;

    vec4 color;
    if (blurPx <= 0.25) {
        color = sampleLayer(vUv);
    } else {
        const int TAPS = 17;
        const float INV_2SIG2 = 12.5;
        float invH = 1.0 / max(uTextureSize.y, 1.0);
        float jitter = (interleavedGradientNoise(gl_FragCoord.xy) - 0.5) / float(TAPS);

        vec4 sum = vec4(0.0);
        float weightSum = 0.0;
        for (int i = 0; i < TAPS; i++) {
            float o = (float(i) + 0.5) / float(TAPS) - 0.5 + jitter;
            float w = exp(-o * o * INV_2SIG2);
            sum += sampleLayer(vUv + vec2(0.0, o * blurPx * invH)) * w;
            weightSum += w;
        }
        color = sum / max(weightSum, 1e-4);
    }

    float roundMask = coverage(sdRoundBox(vLocalPx - uSize * 0.5, uSize * 0.5, uRadii));
    float clipMask = 1.0;
    if (uClipRadii.x + uClipRadii.y + uClipRadii.z + uClipRadii.w > 0.0001) {
        vec2 clipHalf = uClipRect.zw * 0.5;
        vec2 clipCenter = uClipRect.xy + clipHalf;
        clipMask = coverage(sdRoundBox(vPosPx - clipCenter, clipHalf, uClipRadii));
    }

    if (uFocusStrength > 0.001) {
        vec2 toCenter = (vLocalPx - uSize * 0.5) / max(uSize * 0.5, vec2(1.0));
        float r2 = clamp(dot(toCenter, toCenter), 0.0, 1.0);
        color.rgb *= 1.0 - smootherstep01(r2) * (0.18 * uFocusStrength);
    }

    color *= edgeMask * roundMask * clipMask * clamp(uAlpha, 0.0, 1.0);
    if (color.a <= 0.001 && max(max(color.r, color.g), color.b) <= 0.002) {
        discard;
    }
    fragColor = color;
}
