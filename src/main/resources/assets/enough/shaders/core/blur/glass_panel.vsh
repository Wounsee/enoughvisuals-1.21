#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 TexCoord;
out vec2 LocalCoord; // 0..1 normalized position within panel

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    TexCoord = UV0;
    // Color.rg encodes normalized local coordinates (0..1)
    LocalCoord = Color.rg;
}