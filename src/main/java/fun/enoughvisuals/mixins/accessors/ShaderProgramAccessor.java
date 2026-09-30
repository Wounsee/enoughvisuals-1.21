package fun.enoughvisuals.mixins.accessors;

import java.util.Map;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={ShaderProgram.class})
public interface ShaderProgramAccessor {
    @Accessor
    public Map<String, GlUniform> getUniformsByName();
}
