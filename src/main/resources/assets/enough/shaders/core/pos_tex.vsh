#version 150

in vec2 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec2 uScreenSize;

out vec2 vTexCoord; // Передаем UV координаты (0..1) во фрагментный шейдер

void main() {
    // В ShaderManager.drawQuad мы передаем координаты в NDC (-1..1),
    // поэтому умножение на матрицы может быть лишним, если vertices уже готовые.
    // Но если использовать стандартный пайплайн:

    // В твоем ShaderManager vertices уже в NDC.
    // Просто передаем позицию.
    gl_Position = vec4(Position.x, Position.y, 0.0, 1.0);

    // Вычисляем UV из NDC (-1..1 -> 0..1) для удобства в шейдере
    vTexCoord = Position * 0.5 + 0.5;
    // Инвертируем Y, так как в GL 0 снизу, а в UI 0 сверху
    vTexCoord.y = 1.0 - vTexCoord.y;
}