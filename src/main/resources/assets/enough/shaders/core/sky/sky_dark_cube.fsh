#version 150

uniform sampler2D Sampler0; // front
uniform sampler2D Sampler1; // right
uniform sampler2D Sampler2; // back
uniform sampler2D Sampler3; // left
uniform sampler2D Sampler4; // up
uniform sampler2D Sampler5; // down
uniform vec2 uResolution;
uniform vec3 uColor;
uniform float uAlpha;
uniform vec2 uCameraDir;
uniform float uFov;

out vec4 fragColor;

mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0,
                0.0,   c,   s,
                0.0,  -s,   c);
}

mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(  c, 0.0,   s,
                0.0, 1.0, 0.0,
                 -s, 0.0,   c);
}

vec2 safeUv(vec2 uv) {
    return clamp(uv * 0.5 + 0.5, vec2(0.0015), vec2(0.9985));
}

vec3 sampleCubeSky(vec3 dir) {
    vec3 ad = abs(dir);
    vec2 uv;

    if (ad.z >= ad.x && ad.z >= ad.y) {
        if (dir.z > 0.0) {
            uv = safeUv(vec2(dir.x / ad.z, -dir.y / ad.z));
            return texture(Sampler0, uv).rgb;
        }
        uv = safeUv(vec2(-dir.x / ad.z, -dir.y / ad.z));
        return texture(Sampler2, uv).rgb;
    }

    if (ad.x >= ad.y) {
        if (dir.x > 0.0) {
            uv = safeUv(vec2(-dir.z / ad.x, -dir.y / ad.x));
            return texture(Sampler1, uv).rgb;
        }
        uv = safeUv(vec2(dir.z / ad.x, -dir.y / ad.x));
        return texture(Sampler3, uv).rgb;
    }

    if (dir.y > 0.0) {
        uv = safeUv(vec2(dir.x / ad.y, dir.z / ad.y));
        return texture(Sampler4, uv).rgb;
    }

    uv = safeUv(vec2(dir.x / ad.y, -dir.z / ad.y));
    return texture(Sampler5, uv).rgb;
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;

    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayW = normalize(rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV);

    vec3 tex = sampleCubeSky(rayW);
    vec3 tint = mix(vec3(1.0), uColor, 0.08);
    vec3 color = tex * tint;

    fragColor = vec4(color, uAlpha);
}
