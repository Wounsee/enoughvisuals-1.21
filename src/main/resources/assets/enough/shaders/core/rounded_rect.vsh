#version 150

in vec2 Position;

uniform vec2 uScreenSize;

void main() {
    gl_Position = vec4(Position, 0.0, 1.0);
}