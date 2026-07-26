#version 150

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

layout(std140) uniform PrismaticChams {
    vec4 u_AccentTop;
    vec4 u_AccentBottom;
    vec4 u_CameraAndTime;
    vec4 u_Params;
    vec4 u_State;
    vec4 u_ResolutionInfo;
    ivec4 u_ModeFlags;
};

#define u_Time u_CameraAndTime.w
#define u_Animation u_State.x
#define u_Resolution u_ResolutionInfo.xy

out vec3 v_Normal;
out vec3 v_ViewDir;
out vec3 v_ViewPos;
out vec3 v_Position;
out vec4 v_Color;
out vec2 v_Uv;
out vec2 v_ScreenPos;
out vec2 v_EntityScreenPos;

vec2 screenFromClip(vec4 clip) {
    float w = abs(clip.w) < 0.00001 ? (clip.w < 0.0 ? -0.00001 : 0.00001) : clip.w;
    vec2 ndc = clip.xy / w;
    return (ndc * 0.5 + 0.5) * u_Resolution;
}

void main() {
    float entry = smoothstep(0.0, 1.0, clamp(u_Animation, 0.0, 1.0));
    float pull = 1.0 - entry;
    vec3 animatedPosition = Position + Normal * pull * (0.038 + 0.014 * sin(u_Time * 6.1 + Position.y * 9.0));
    vec4 viewPosition = ModelViewMat * vec4(animatedPosition, 1.0);
    vec4 clipPosition = ProjMat * viewPosition;
    vec4 centerView = ModelViewMat * vec4(0.0, 1.0, 0.0, 1.0);
    vec4 centerClip = ProjMat * centerView;
    gl_Position = clipPosition;
    v_Normal = normalize(mat3(ModelViewMat) * Normal);
    v_ViewDir = -viewPosition.xyz;
    v_ViewPos = viewPosition.xyz;
    v_Position = animatedPosition;
    v_Color = Color;
    v_Uv = UV0;
    v_ScreenPos = screenFromClip(clipPosition);
    v_EntityScreenPos = screenFromClip(centerClip);
}
