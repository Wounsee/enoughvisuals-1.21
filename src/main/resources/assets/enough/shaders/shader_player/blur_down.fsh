#version 150

uniform sampler2D CurrentInput;
uniform sampler2D DepthTexture;
uniform vec2 HalfPixel;
uniform int FirstPass;

in vec2 texCoord;
out vec4 fragColor;

const float ALPHA_EPSILON = 0.001;

vec4 readSample(vec2 uv) {
    vec4 source = texture(CurrentInput, uv);
    if (FirstPass == 0) {
        return source;
    }

    float alpha = step(ALPHA_EPSILON, source.a);
    float depth = alpha > 0.0 ? texture(DepthTexture, uv).r : 1.0;
    return vec4(depth * alpha, alpha, depth, alpha);
}

void includeDepth(inout float nearestDepth, vec4 sampleValue) {
    if (sampleValue.a > ALPHA_EPSILON) {
        nearestDepth = min(nearestDepth, sampleValue.b);
    }
}

void main() {
    vec4 center = readSample(texCoord);
    vec4 diagonalA = readSample(texCoord - HalfPixel);
    vec4 diagonalB = readSample(texCoord + HalfPixel);
    vec4 diagonalC = readSample(texCoord + vec2(HalfPixel.x, -HalfPixel.y));
    vec4 diagonalD = readSample(texCoord - vec2(HalfPixel.x, -HalfPixel.y));

    float alpha = (center.a * 4.0
            + diagonalA.a + diagonalB.a + diagonalC.a + diagonalD.a) * 0.125;
    float nearestDepth = 1.0;
    includeDepth(nearestDepth, center);
    includeDepth(nearestDepth, diagonalA);
    includeDepth(nearestDepth, diagonalB);
    includeDepth(nearestDepth, diagonalC);
    includeDepth(nearestDepth, diagonalD);

    fragColor = vec4(nearestDepth * alpha, alpha, nearestDepth, alpha);
}
