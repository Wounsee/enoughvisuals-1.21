package fun.enoughvisuals.port;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import fun.enoughvisuals.port.api.Module;
import fun.enoughvisuals.port.api.ModuleRegister;

@ModuleRegister(
   name = "JumpCircle",
   description = "Тонкая гравитационная линза у ног при прыжке",
   category = Category.Visuals
)
public class JumpCircle extends Module {
   private static final long TIMESTAMP = 250L;
   private static final long TIMESTAMP_2 = 1500L;
   private static final long TIMESTAMP_3 = 850L;
   private static final float FLOAT_VALUE = 1.95F;
   private static final float FLOAT_VALUE_2 = 0.04F;
   private final List<JumpCircle.JumpCircleState> items = new ArrayList<>();
   private long timestamp;

   public JumpCircle() {
      JumpCircleRenderLayer.invoke();
   }

   @EventHandler
   public void onLivingEntityTick(LivingEntityTickEvent livingEntityTickEvent) {
   }

   public void onJump() {
      if (CLIENT.player != null) {
         long longValue = System.currentTimeMillis();
         if (longValue - this.timestamp >= 180L) {
            this.timestamp = longValue;
            Vec3d vec3d2 = CLIENT.player.getPos().add(0.0, 0.04F, 0.0);
            this.items.add(new JumpCircle.JumpCircleState(vec3d2, longValue));
         }
      }
   }

   public void clear() {
      this.items.clear();
      this.timestamp = 0L;
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      if (!this.items.isEmpty()) {
         long longValue2 = System.currentTimeMillis();
         this.items.removeIf(jumpCircleState -> longValue2 - jumpCircleState.timestamp > 1500L);
         if (!this.items.isEmpty()) {
            int intValue = fun.enoughvisuals.systems.modules.modules.client.CustomTheme.hudAccent() & 16777215;
            int intValue2 = intValue >> 16 & 0xFF;
            int intValue3 = intValue >> 8 & 0xFF;
            int intValue4 = intValue & 0xFF;
            Immediate immediate = WorldRenderBuffer.getIMMEDIATE();

            try {
               VertexConsumer vertexConsumer2 = immediate.getBuffer(JumpCircleRenderLayer.getRENDER_LAYER());
               MatrixStack matrices = render3DEvent.getMatrixStack();
               Vec3d vec3d3 = CLIENT.gameRenderer.getCamera().getPos();

               for (JumpCircle.JumpCircleState jumpCircleState2 : this.items) {
                  long longValue3 = longValue2 - jumpCircleState2.timestamp;
                  if (longValue3 >= 850L && !jumpCircleState2.flag) {
                     jumpCircleState2.flexibleAnimation3.resolve2(0.0, 0.65, Easings.EASING_FUNCTION_6);
                     jumpCircleState2.flexibleAnimation2.resolve2(1.35, 0.65, Easings.EASING_FUNCTION_6);
                     jumpCircleState2.flag = true;
                  }

                  jumpCircleState2.flexibleAnimation.check();
                  jumpCircleState2.flexibleAnimation2.check();
                  jumpCircleState2.flexibleAnimation3.check();
                  float floatValue = measure((float)jumpCircleState2.flexibleAnimation.getDoubleValue4());
                  float floatValue2 = (float)jumpCircleState2.flexibleAnimation2.getDoubleValue4();
                  float floatValue3 = measure((float)jumpCircleState2.flexibleAnimation3.getDoubleValue4());
                  if (!(floatValue3 <= 0.002F) && !(floatValue2 <= 0.001F)) {
                     int intValue5 = Math.max(0, Math.min(255, Math.round(floatValue3 * floatValue * 255.0F)));
                     float floatValue4 = 1.95F * floatValue2 * floatValue;
                     matrices.push();
                     matrices.translate(jumpCircleState2.vec3d.x - vec3d3.x, jumpCircleState2.vec3d.y - vec3d3.y, jumpCircleState2.vec3d.z - vec3d3.z);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                     Matrix4f matrix4f2 = matrices.peek().getPositionMatrix();
                     this.invoke(vertexConsumer2, matrix4f2, -floatValue4 * 0.5F, -floatValue4 * 0.5F, floatValue4, floatValue4, intValue2, intValue3, intValue4, intValue5);
                     matrices.pop();
                  }
               }
            } finally {
               WorldRenderBuffer.invoke();
            }
         }
      }
   }

   private void invoke(VertexConsumer vertexConsumer, Matrix4f matrix4f, float f, float g, float h, float i, int j, int k, int l, int m) {
      float floatValue5 = f + h;
      float floatValue6 = g + i;
      vertexConsumer.vertex(matrix4f, f, g, 0.0F).texture(0.0F, 0.0F).color(j, k, l, m).normal(0.0F, 0.0F, 1.0F);
      vertexConsumer.vertex(matrix4f, floatValue5, g, 0.0F).texture(1.0F, 0.0F).color(j, k, l, m).normal(0.0F, 0.0F, 1.0F);
      vertexConsumer.vertex(matrix4f, floatValue5, floatValue6, 0.0F).texture(1.0F, 1.0F).color(j, k, l, m).normal(0.0F, 0.0F, 1.0F);
      vertexConsumer.vertex(matrix4f, f, floatValue6, 0.0F).texture(0.0F, 1.0F).color(j, k, l, m).normal(0.0F, 0.0F, 1.0F);
   }

   private static float measure(float f) {
      return f < 0.0F ? 0.0F : (f > 1.0F ? 1.0F : f);
   }

   static final class JumpCircleState {
      final Vec3d vec3d;
      final long timestamp;
      final FlexibleAnimation flexibleAnimation = new FlexibleAnimation();
      final FlexibleAnimation flexibleAnimation2 = new FlexibleAnimation();
      final FlexibleAnimation flexibleAnimation3 = new FlexibleAnimation();
      boolean flag;

      JumpCircleState(Vec3d vec3d, long l) {
         this.vec3d = vec3d;
         this.timestamp = l;
         this.flexibleAnimation.resolve2(1.0, 0.28, Easings.EASING_FUNCTION_6);
         this.flexibleAnimation2.resolve2(1.0, 0.4, Easings.EASE_OUT_QUART);
         this.flexibleAnimation3.resolve2(1.0, 0.18, Easings.EASE_OUT_QUART);
      }
   }
}
