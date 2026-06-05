#version 150

uniform sampler2D Sampler0;
uniform vec2 uResolution;
uniform vec3 uColor;
uniform float uAlpha;
uniform vec2 uCameraDir;
uniform float uFov;

out vec4 fragColor;

const float PI = 3.14159265358979323846;

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

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;

    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayW = normalize(rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV);

    float rawPanoU = atan(rayW.x, rayW.z) / (2.0 * PI) + 0.5;
    float panoV = 0.5 - asin(clamp(rayW.y, -1.0, 1.0)) / PI;

    // Mirror the panorama horizontally so the wrap point never exposes a hard left/right texture seam.
    float wrappedU = fract(rawPanoU + 0.25);
    float panoU = 1.0 - abs(wrappedU * 2.0 - 1.0);
    vec3 tex = texture(Sampler0, vec2(panoU, clamp(panoV, 0.0, 1.0))).rgb;
    vec3 tint = mix(vec3(1.0), uColor, 0.18);
    vec3 color = tex * tint;

    fragColor = vec4(color, uAlpha);
}

