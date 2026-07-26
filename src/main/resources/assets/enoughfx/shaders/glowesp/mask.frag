#version 330 core

in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uSource;
uniform sampler2D uDepthSource;
uniform sampler2D uTagged;
uniform sampler2D uTaggedDepth;
uniform float uThreshold;
uniform int uHasDepth;
uniform int uTagMode;

void main() {
    vec4 source = texture(uSource, vUv);
    float colorCoverage = max(source.a, max(source.r, max(source.g, source.b)));
    float present = step(uThreshold, colorCoverage);
    float mainDepth = 1.0;
    if (uHasDepth != 0) {
        mainDepth = texture(uDepthSource, vUv).r;
        present = max(present, 1.0 - step(0.999999, mainDepth));
    }
    if (uTagMode != 0) {
        vec4 tagged = texture(uTagged, vUv);
        float taggedCoverage = max(tagged.a, max(tagged.r, max(tagged.g, tagged.b)));
        float taggedPresent = step(uThreshold, taggedCoverage);
        float taggedDepth = texture(uTaggedDepth, vUv).r;
        taggedPresent = max(taggedPresent, 1.0 - step(0.999999, taggedDepth));
        float taggedFront = taggedPresent;
        if (uHasDepth != 0) {
            taggedFront *= step(taggedDepth, mainDepth + 0.000001);
        }
        present *= uTagMode == 1 ? 1.0 - taggedFront : taggedFront;
    }
    FragColor = vec4(present, present, present, present);
}
