#version 150

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform vec2 Resolution;
uniform float Offset;

out vec4 fragColor;

void main() {
    vec2 stepUv = vec2(0.0, Resolution.y * Offset);
    vec3 color = texture(Sampler0, TexCoord).rgb * 0.1668305005;
    color += (texture(Sampler0, TexCoord - stepUv).rgb
            + texture(Sampler0, TexCoord + stepUv).rgb) * 0.1534914701;
    color += (texture(Sampler0, TexCoord - stepUv * 2.0).rgb
            + texture(Sampler0, TexCoord + stepUv * 2.0).rgb) * 0.1195392772;
    color += (texture(Sampler0, TexCoord - stepUv * 3.0).rgb
            + texture(Sampler0, TexCoord + stepUv * 3.0).rgb) * 0.0788051484;
    color += (texture(Sampler0, TexCoord - stepUv * 4.0).rgb
            + texture(Sampler0, TexCoord + stepUv * 4.0).rgb) * 0.0439760425;
    color += (texture(Sampler0, TexCoord - stepUv * 5.0).rgb
            + texture(Sampler0, TexCoord + stepUv * 5.0).rgb) * 0.0207728116;
    fragColor = vec4(color, 1.0);
}
