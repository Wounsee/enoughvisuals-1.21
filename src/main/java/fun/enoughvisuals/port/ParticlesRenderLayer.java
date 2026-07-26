package fun.enoughvisuals.port;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;

public final class ParticlesRenderLayer {
    private static final Map<Identifier, RenderLayer> CACHE = new ConcurrentHashMap<>();
    private ParticlesRenderLayer() { }
    public static RenderLayer resolve(Identifier id) {
        return CACHE.computeIfAbsent(id, key -> RenderLayer.of(
                key.toString(), VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS,
                1024, false, true, RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(key, TriState.FALSE, false))
                        .program(RenderPhase.POSITION_TEXTURE_COLOR_PROGRAM)
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                        .writeMaskState(RenderPhase.COLOR_MASK)
                        .build(false)));
    }
}
