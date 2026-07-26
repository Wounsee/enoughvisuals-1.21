package fun.enoughvisuals.port;

public final class ShaderBindingRegistry {
    private static final ShaderBindingRegistry INSTANCE = new ShaderBindingRegistry();
    public static ShaderBindingRegistry getINSTANCE() { return INSTANCE; }
    public void invoke(Object owner, ShaderBinding binding) { }
    public void invoke4(Object owner) { }
    public void invoke7(Object owner, ShaderBinding binding) { }
}
