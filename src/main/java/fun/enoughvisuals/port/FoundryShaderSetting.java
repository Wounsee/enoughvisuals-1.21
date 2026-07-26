package fun.enoughvisuals.port;

public final class FoundryShaderSetting extends Setting {
    public FoundryShaderSetting(String name, ShaderSurface surface) { this.name = name; }
    public String resolve2() { return ""; }
}
