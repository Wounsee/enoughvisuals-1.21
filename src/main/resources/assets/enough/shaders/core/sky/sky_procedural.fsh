#version 330

uniform float uTime;
uniform vec2 uResolution;
uniform vec4 uColor1;
uniform vec4 uColor2;
uniform vec4 uAccent;
uniform float uAlpha;
uniform float uMode;
uniform float uSpeed;
uniform float uScale;
uniform float uIntensity;
uniform float uStars;
uniform float uVanillaBlend;
uniform vec2 uCameraDir;
uniform float uFov;

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    float n000 = hash(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash(i + vec3(1.0, 1.0, 1.0));
    float nx00 = mix(n000, n100, u.x);
    float nx10 = mix(n010, n110, u.x);
    float nx01 = mix(n001, n101, u.x);
    float nx11 = mix(n011, n111, u.x);
    float nxy0 = mix(nx00, nx10, u.y);
    float nxy1 = mix(nx01, nx11, u.y);
    return mix(nxy0, nxy1, u.z);
}

float fbm(vec3 p) {
    float value = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        value += noise(p) * amp;
        p = mat3(
            1.53, 0.72, 0.21,
           -0.64, 1.39, 0.57,
            0.31,-0.48, 1.71
        ) * p;
        amp *= 0.5;
    }
    return value;
}

vec2 domeUv(vec3 dir) {
    dir = normalize(dir);
    float k = 1.0 / (1.0 + max(dir.y, -0.35));
    return dir.xz * k * 0.55 + 0.5;
}

float hash2(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash2(i);
    float b = hash2(i + vec2(1.0, 0.0));
    float c = hash2(i + vec2(0.0, 1.0));
    float d = hash2(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm2(vec2 p) {
    float value = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 6; i++) {
        value += noise2(p) * amp;
        p = mat2(1.6, 1.2, -1.2, 1.6) * p;
        amp *= 0.5;
    }
    return value;
}

float starsFunc(vec3 dir, float amount, float time) {
    vec3 grid = dir * 130.0;
    vec3 cell = floor(grid);
    vec3 local = fract(grid) - 0.5;
    float h = hash(cell);
    float sparkle = smoothstep(0.997 - amount * 0.045, 1.0, h);
    float shape = smoothstep(0.18, 0.0, length(local));
    return sparkle * shape * (0.65 + 0.35 * sin(time * 2.0 + h * 12.0));
}

vec3 aurora(vec3 dir, float time, float scale, float intensity) {
    vec3 p = dir * scale * 2.6;
    float flow = fbm(p + vec3(time * 0.08, -time * 0.03, time * 0.04));
    float bands = 0.0;
    for (int i = 0; i < 4; i++) {
        float fi = float(i);
        float center = -0.15 + fi * 0.18 + sin((dir.x + dir.z) * (2.2 + fi) + time * (0.35 + fi * 0.08)) * 0.10;
        bands += smoothstep(0.18, 0.0, abs(dir.y - center - flow * 0.16));
    }
    vec3 base = mix(uColor1.rgb * 0.16, uColor2.rgb * 0.32, clamp(dir.y * 0.5 + 0.5, 0.0, 1.0));
    vec3 glow = mix(uColor1.rgb, uColor2.rgb, flow);
    return base + glow * bands * 0.55 * intensity + uAccent.rgb * pow(bands, 3.0) * 0.24;
}

vec3 night(vec3 dir, float time, float scale, float intensity) {
    vec2 uv = domeUv(dir) * scale;
    vec2 drift = vec2(time * 0.0062379, cos(time * 0.0962379)) * (sin(time * 0.0041839) + 1.1);
    float n1 = fbm2(uv * 2.0 + drift);
    float n2 = fbm2(uv * 6.0 + drift * 1.15 + 12.523);
    float cloud = pow(max(0.8 - n1, 0.0), 2.0);
    float core = pow(max((0.9 - n2) * cloud, 0.0), 1.1);
    vec2 starCell = floor(uv * 420.0);
    float r1 = hash2(starCell);
    float r2 = hash2(starCell.yx + 17.0);
    float twinkle = sin((time + 10.0) * r1 * 2.7) * 0.8 + 0.5;
    float star = r1 * pow(r2, 20.0) * twinkle * 0.9;
    float h = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 base = mix(uColor1.rgb * 0.08, uColor2.rgb * 0.22, h);
    return base + (uColor1.rgb * cloud * 0.55 + uColor2.rgb * core * 0.95 + uAccent.rgb * star) * intensity;
}

vec3 snow(vec3 dir, float time, float scale, float intensity) {
    vec2 uv = domeUv(dir) * 2.0 - 1.0;
    float h = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 base = mix(uColor1.rgb * 0.10, uColor2.rgb * 0.18, h);
    float c = smoothstep(1.0, 0.3, clamp(uv.y * 0.3 + 0.8, 0.0, 0.75));
    for (int i = 0; i < 7; i++) {
        float fi = float(i);
        float sc = (6.0 + fi * 2.5) * scale;
        vec2 p = uv;
        p += time / sc;
        p.y += time * 2.0 / sc;
        p.x += sin(p.y + time * 0.5) / sc;
        p *= sc;
        vec2 cell = floor(p);
        vec2 local = fract(p);
        vec2 flake = 0.5 + 0.35 * sin(11.0 * fract(sin((cell + sc) * mat2(7.0, 3.0, 6.0, 5.0)) * 5.0)) - local;
        c += smoothstep(0.035, 0.0, length(flake)) * (0.45 + fi * 0.08);
    }
    return base + mix(uColor1.rgb, uAccent.rgb, 0.75) * c * intensity;
}

vec3 sky(vec3 dir, float time, float scale, float intensity) {
    vec2 p = domeUv(dir) * scale * 1.1;
    float h = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    float q = fbm2(p * 0.5);
    float r = 0.0;
    vec2 uv = p - q + time * 0.03;
    float weight = 0.8;
    for (int i = 0; i < 8; i++) {
        r += abs(weight * noise2(uv));
        uv = mat2(1.6, 1.2, -1.2, 1.6) * uv + time * 0.03;
        weight *= 0.7;
    }
    float f = 0.0;
    uv = p - q + time * 0.03;
    weight = 0.2;
    for (int i = 0; i < 8; i++) {
        f += weight * noise2(uv);
        uv = mat2(1.6, 1.2, -1.2, 1.6) * uv + time * 0.03;
        weight *= 0.6;
    }
    float clouds = clamp(0.2 + 8.0 * f * r + r * 0.25, 0.0, 1.0);
    vec3 base = mix(uColor1.rgb * 0.30, uColor2.rgb * 0.50, h);
    vec3 cloud = vec3(1.1, 1.1, 0.9) * mix(uAccent.rgb, uColor2.rgb, 0.18);
    return mix(base, cloud, clouds * intensity);
}

vec3 starMode(vec3 dir, float time, float scale, float intensity) {
    vec2 uv = (domeUv(dir) - 0.5) * vec2(2.0, 2.0) * scale;
    uv.x += sin(0.2 + uv.y * 0.8) * 0.5;
    uv.x *= 50.0;
    float dx = fract(uv.x);
    uv.x = floor(uv.x);
    uv.y *= 0.15;
    float o = sin(uv.x * 215.4);
    float s = cos(uv.x * 33.1) * 0.3 + 0.7;
    float trail = mix(95.0, 35.0, s);
    float yv = fract(uv.y + time * 0.4 * s + o) * trail;
    yv = 1.0 / max(yv, 0.001);
    yv = smoothstep(0.0, 1.0, yv * yv);
    yv = sin(yv * 3.14159) * (s * 5.0);
    float d2 = sin(dx * 3.14159);
    yv *= d2 * d2;
    float h = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 base = mix(uColor1.rgb * 0.05, uColor2.rgb * 0.18, h);
    return base + mix(uColor1.rgb, uColor2.rgb, s) * sqrt(max(yv, 0.0)) * intensity;
}

vec3 glow(vec3 dir, float time, float scale, float intensity) {
    vec2 uv = (domeUv(dir) - 0.5) * 2.0 * scale;
    for (float i = 1.0; i < 10.0; i++) {
        uv.x += 0.6 / i * cos(i * 2.5 * uv.y + time);
        uv.y += 0.6 / i * cos(i * 1.5 * uv.x + time);
    }
    float wave = 0.1 / max(abs(sin(time - uv.y - uv.x)), 0.035);
    float h = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 base = mix(uColor1.rgb * 0.04, uColor2.rgb * 0.10, h);
    return base + mix(uColor1.rgb, uColor2.rgb, smoothstep(0.0, 1.2, wave)) * wave * intensity;
}

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

vec3 getRayDirection(vec2 screenPos) {
    vec2 uv = screenPos / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;
    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    return rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV;
}

void main() {
    vec3 dir = getRayDirection(gl_FragCoord.xy);
    float time = uTime * uSpeed;
    float scale = max(uScale, 0.01);
    float intensity = uIntensity;
    float starAmount = uStars;
    vec3 color;

    if (uMode < 0.5) {
        color = aurora(dir, time, scale, intensity);
    } else if (uMode < 1.5) {
        color = night(dir, time, scale, intensity);
    } else if (uMode < 2.5) {
        color = snow(dir, time, scale, intensity);
    } else if (uMode < 3.5) {
        color = sky(dir, time, scale, intensity);
    } else if (uMode < 4.5) {
        color = starMode(dir, time, scale, intensity);
    } else {
        color = glow(dir, time, scale, intensity);
    }

    float starValue = starsFunc(dir, starAmount, time) * smoothstep(-0.15, 0.65, dir.y);
    color += uAccent.rgb * starValue;
    color *= 0.72 + smoothstep(-0.65, 0.85, dir.y) * 0.34;

    float alpha = uAlpha * mix(1.0, 0.72, clamp(uVanillaBlend, 0.0, 1.0));
    fragColor = vec4(color, alpha);
}
