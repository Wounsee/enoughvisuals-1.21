#version 150

uniform float uTime;
uniform vec2 uResolution;
uniform vec3 uColor;
uniform float uAlpha;
uniform float uSpeed;
uniform float uScale;
uniform float uIntensity;
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

float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash(i + vec3(0.0, 0.0, 0.0)), hash(i + vec3(1.0, 0.0, 0.0)), f.x),
            mix(hash(i + vec3(0.0, 1.0, 0.0)), hash(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
        mix(mix(hash(i + vec3(0.0, 0.0, 1.0)), hash(i + vec3(1.0, 0.0, 1.0)), f.x),
            mix(hash(i + vec3(0.0, 1.0, 1.0)), hash(i + vec3(1.0, 1.0, 1.0)), f.x), f.y),
        f.z
    );
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.52;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amplitude;
        p = p * 2.04 + vec3(2.7, 4.1, 1.8);
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;
    float tanV = tan(radians(uFov) * 0.5);

    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayW = rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV;
    float time = uTime * uSpeed;

    vec3 flow = vec3(time * 0.12, -time * 0.045, time * 0.08);
    vec3 p = rayW * (uScale * 0.24) + flow;
    float broad = fbm(p * 0.72);
    float wisps = fbm(p * 1.28 + vec3(broad * 1.8));
    float folds = 1.0 - abs(fbm(p * 1.55 - flow * 1.15) * 2.0 - 1.0);
    float density = smoothstep(0.48, 0.86, broad * 0.62 + wisps * 0.42 + folds * 0.18);
    float brightFolds = smoothstep(0.62, 0.96, folds * wisps);

    float horizon = clamp(rayW.y * 0.62 + 0.46, 0.0, 1.0);
    vec3 deepSky = vec3(0.008, 0.012, 0.03) + uColor * 0.055;
    vec3 horizonSky = vec3(0.025, 0.035, 0.075) + uColor * 0.11;
    vec3 base = mix(horizonSky, deepSky, smoothstep(0.0, 0.82, horizon));

    vec3 cloudShadow = mix(uColor * 0.11, vec3(0.13, 0.16, 0.25), 0.45);
    vec3 cloudLight = mix(uColor * 0.52, vec3(0.78, 0.83, 0.95), 0.72);
    vec3 clouds = mix(cloudShadow, cloudLight, clamp(wisps * 0.72 + brightFolds * 0.85, 0.0, 1.0));

    vec3 color = mix(base, clouds, density * clamp(0.72 + uIntensity * 1.4, 0.0, 1.25));

    fragColor = vec4(color, uAlpha);
}
