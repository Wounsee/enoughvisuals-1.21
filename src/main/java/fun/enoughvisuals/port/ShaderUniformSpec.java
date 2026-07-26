package fun.enoughvisuals.port;

public record ShaderUniformSpec(String name, String uniformName, ShaderUniformSpecState kind,
                                float[] defaults, float defaultFloat, float minimum, float maximum) {
    public enum ShaderUniformSpecState { COLOR, FLOAT }
}
