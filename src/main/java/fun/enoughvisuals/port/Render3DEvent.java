package fun.enoughvisuals.port;

import lombok.Generated;
import net.minecraft.client.util.math.MatrixStack;

public class Render3DEvent extends Event {
   private final MatrixStack matrixStack;
   private final float floatValue;

   public Render3DEvent(MatrixStack matrixStack, float f) {
      this.matrixStack = matrixStack;
      this.floatValue = f;
   }

   @Generated
   public MatrixStack getMatrixStack() {
      return this.matrixStack;
   }

   @Generated
   public float getFloatValue() {
      return this.floatValue;
   }
}
