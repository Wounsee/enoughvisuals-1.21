package fun.enoughvisuals.port;

import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class JumpCircleRenderLayer {
   private static final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(
      Identifier.of("enoughfx", "core/jump_circle"), VertexFormats.POSITION_TEXTURE_COLOR_NORMAL, Defines.EMPTY
   );
   private static final RenderPhase.ShaderProgram PROGRAM = new RenderPhase.ShaderProgram(SHADER_KEY);
   private static final RenderLayer RENDER_LAYER = RenderLayer.of(
      "ported/jump_circle_lens", VertexFormats.POSITION_TEXTURE_COLOR_NORMAL, VertexFormat.DrawMode.QUADS,
      262144, false, true, RenderLayer.MultiPhaseParameters.builder()
         .program(PROGRAM).transparency(RenderPhase.ADDITIVE_TRANSPARENCY)
         .cull(RenderPhase.DISABLE_CULLING).depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
         .writeMaskState(RenderPhase.COLOR_MASK).build(false)
   );
   private JumpCircleRenderLayer() { }
   public static void invoke() { if (RENDER_LAYER == null) throw new IllegalStateException("JumpCircle shader registry failed"); }
   public static RenderLayer getRENDER_LAYER() { return RENDER_LAYER; }
   public static RenderLayer getRENDER_LAYER2() { return RENDER_LAYER; }
}
