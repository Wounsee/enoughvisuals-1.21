package fun.enoughvisuals.port;

import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/** 1.21.4 adapter for Ported's two ChinaHat render pipelines. */
public final class ChinaHatRenderLayer {
   private static final int BUFFER_SIZE = 2097152;
   private static final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(
      Identifier.of("enoughfx", "core/chinahat"), VertexFormats.POSITION_TEXTURE_COLOR_NORMAL, Defines.EMPTY
   );
   private static final RenderPhase.ShaderProgram PROGRAM = new RenderPhase.ShaderProgram(SHADER_KEY);
   private static final RenderLayer RENDER_LAYER = RenderLayer.of(
      "ported/chinahat_glass",
      VertexFormats.POSITION_TEXTURE_COLOR_NORMAL,
      VertexFormat.DrawMode.TRIANGLES,
      BUFFER_SIZE,
      false,
      true,
      RenderLayer.MultiPhaseParameters.builder()
         .program(PROGRAM)
         .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
         .cull(RenderPhase.DISABLE_CULLING)
         .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
         .writeMaskState(RenderPhase.ALL_MASK)
         .build(false)
   );
   private static final RenderLayer RENDER_LAYER_2 = RenderLayer.of(
      "ported/chinahat_bloom",
      VertexFormats.POSITION_TEXTURE_COLOR_NORMAL,
      VertexFormat.DrawMode.TRIANGLES,
      BUFFER_SIZE,
      false,
      true,
      RenderLayer.MultiPhaseParameters.builder()
         .program(PROGRAM)
         .transparency(RenderPhase.ADDITIVE_TRANSPARENCY)
         .cull(RenderPhase.DISABLE_CULLING)
         .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
         .writeMaskState(RenderPhase.COLOR_MASK)
         .build(false)
   );

   private ChinaHatRenderLayer() {
   }

   public static void invoke() {
      if (RENDER_LAYER == null || RENDER_LAYER_2 == null) {
         throw new IllegalStateException("ChinaHat shader registry failed");
      }
   }

   public static RenderLayer getRENDER_LAYER() {
      return RENDER_LAYER;
   }

   public static RenderLayer getRENDER_LAYER_2() {
      return RENDER_LAYER_2;
   }
}
