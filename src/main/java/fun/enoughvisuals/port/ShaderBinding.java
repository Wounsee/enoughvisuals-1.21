package fun.enoughvisuals.port;

public interface ShaderBinding {
    ShaderSurface getESP();
    String resolve();
    boolean check();
}
