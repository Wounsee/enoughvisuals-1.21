#version 330 core

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uScene;
uniform vec2  uResolution;
uniform float uThreshold;
uniform float uSoftness;
uniform float uFlipY;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

vec2 sceneUv(vec2 uv) {
    return mix(uv, vec2(uv.x, 1.0 - uv.y), uFlipY);
}

vec3 toLinear(vec3 c) {
    return pow(max(c, vec3(0.0)), vec3(2.2));
}

void main() {
    vec3 col = texture(uScene, sceneUv(vUv)).rgb;
    vec3 lin = toLinear(col);
    float lum = dot(lin, LUMA);
    float maxC = max(col.r, max(col.g, col.b));
    float minC = min(col.r, min(col.g, col.b));
    float chroma = maxC - minC;
    float neutralHighlight = smoothstep(0.52, 0.92, lum) * (1.0 - smoothstep(0.045, 0.22, chroma));
    float knee = uThreshold * uSoftness;
    float soft = clamp(lum - uThreshold + knee, 0.0, 2.0 * knee);
    soft = soft * soft / (4.0 * knee + 1e-4);
    float contribution = max(soft, lum - uThreshold) / max(lum, 1e-4);
    contribution *= mix(1.0, 0.16, neutralHighlight);
    vec3 bright = lin * contribution;
    fragColor = vec4(bright, 1.0);
}
