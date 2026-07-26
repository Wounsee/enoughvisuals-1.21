package fun.enoughvisuals.port;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

public final class RenderCapabilities {
   private RenderCapabilities() {
   }

   public static void invoke(int i, int j, int k, int l, int m) {
      int intValue = GL11.glGetInteger(35055);
      if (intValue != 0) {
         GL15.glBindBuffer(35052, 0);
      }

      try {
         GL11.glTexImage2D(3553, 0, i, j, k, 0, l, m, (ByteBuffer)null);
      } finally {
         if (intValue != 0) {
            GL15.glBindBuffer(35052, intValue);
         }
      }
   }
}
