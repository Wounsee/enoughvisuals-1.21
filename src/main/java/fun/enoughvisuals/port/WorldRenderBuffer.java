package fun.enoughvisuals.port;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.Window;

public final class WorldRenderBuffer {
   private static final int INT_VALUE = 262144;
   private static final BufferAllocator BUFFER_ALLOCATOR = new BufferAllocator(262144);
   private static final Immediate IMMEDIATE = VertexConsumerProvider.immediate(BUFFER_ALLOCATOR);

   private WorldRenderBuffer() {
   }

   public static Immediate getIMMEDIATE() {
      return IMMEDIATE;
   }

   public static boolean check(MinecraftClient minecraftClient) {
      if (minecraftClient != null && minecraftClient.getWindow() != null) {
         Window window = minecraftClient.getWindow();
         return !window.hasZeroWidthOrHeight() && window.getFramebufferWidth() > 0 && window.getFramebufferHeight() > 0;
      } else {
         return false;
      }
   }

   public static void invoke() {
      IMMEDIATE.draw();
      BUFFER_ALLOCATOR.clear();
   }
}
