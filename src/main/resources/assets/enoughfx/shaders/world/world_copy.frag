#version 330 core

layout(location = 0) out vec4 fragColor;

uniform sampler2D u_ScreenTexture;
uniform vec2 u_Resolution;

void main() {
    vec2 uv = gl_FragCoord.xy / max(u_Resolution, vec2(1.0));
    fragColor = texture(u_ScreenTexture, uv);
}
