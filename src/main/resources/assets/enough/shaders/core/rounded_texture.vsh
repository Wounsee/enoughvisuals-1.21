#version 150

in vec2 Position;
in vec2 UV;
in vec2 LocalPos;

out vec2 texCoord;
out vec2 localPos;

void main() {
    // Z нам не нужен, порядок отрисовки решает Z-fighting
    gl_Position = vec4(Position, 0.0, 1.0);
    texCoord = UV;
    localPos = LocalPos;
}