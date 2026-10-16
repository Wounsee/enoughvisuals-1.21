#version 150

uniform sampler2D CurrentInput;
uniform vec2 HalfPixel;
uniform float Offset;

in vec2 texCoord;
out vec4 fragColor;

const float ALPHA_EPSILON = 0.001;

void includeSample(inout float weightedAlpha, inout float nearestDepth,
                   vec2 uv, float weight) {
    vec4 sampleValue = texture(CurrentInput, uv);
    weightedAlpha += sampleValue.a * weight;
    if (sampleValue.a > ALPHA_EPSILON) {
        nearestDepth = min(nearestDepth, sampleValue.b);
    }
}

void main() {
    vec2 hp = HalfPixel * Offset;
    float weightedAlpha = 0.0;
    float nearestDepth = 1.0;

    includeSample(weightedAlpha, nearestDepth, texCoord + vec2(-hp.x * 2.0, 0.0), 1.0);
    includeSample(weightedAlpha, nearestDepth, texCoord + vec2(-hp.x, hp.y), 2.0);
    includeSample(weightedAlpha, nearestDepth, texCoord + vec2(0.0, hp.y * 2.0), 1.0);
    includeSample(weightedAlpha, nearestDepth, texCoord + vec2(hp.x, hp.y), 2.0);
    includeSample(weightedAlpha, nearestDepth, texCoord + vec2(hp.x * 2.0, 0.0), 1.0);
    includeSample(weightedAlpha, nearestDepth, texCoord + vec2(hp.x, -hp.y), 2.0);
    includeSample(weightedAlpha, nearestDepth, texCoord + vec2(0.0, -hp.y * 2.0), 1.0);
    includeSample(weightedAlpha, nearestDepth, texCoord + vec2(-hp.x, -hp.y), 2.0);

    float alpha = weightedAlpha * 0.0833;
    fragColor = vec4(nearestDepth * alpha, alpha, nearestDepth, alpha);
}
