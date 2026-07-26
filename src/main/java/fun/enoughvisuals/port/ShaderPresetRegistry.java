package fun.enoughvisuals.port;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ShaderPresetRegistry {
    private static final ShaderPresetRegistry INSTANCE = new ShaderPresetRegistry();
    public static ShaderPresetRegistry getINSTANCE() { return INSTANCE; }
    public boolean check2(String name) { return false; }
    public ShaderNode resolve4(String name) { return null; }
    public ShaderBuildResult resolve2(String name) { return null; }
    public List<ShaderUniformSpec> resolve11(String name) { return Collections.emptyList(); }
    public Map<String, float[]> resolve13(String name) { return Collections.emptyMap(); }
}
