package fun.enoughvisuals.port;

import java.util.List;
import java.util.Map;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import fun.enoughvisuals.port.api.Module;
import fun.enoughvisuals.port.api.ModuleRegister;

@ModuleRegister(
   name = "Trails",
   description = "Оставляет за игроком красивый след.",
   category = Category.Visuals
)
public final class Trails extends Module implements ShaderBinding {
   private static final int INT_VALUE = 64;
   private static final int INT_VALUE_2 = 65;
   private static final int INT_VALUE_3 = 4;
   private static final int INT_VALUE_4 = 14;
   private static final int INT_VALUE_5 = 12;
   private static final int INT_VALUE_6 = 926;
   private static final double DOUBLE_VALUE = 0.0036;
   private static final double DOUBLE_VALUE_2 = 0.00108;
   private static final double DOUBLE_VALUE_3 = 36.0;
   private static final float FLOAT_VALUE = 0.3F;
   private static final float FLOAT_VALUE_2 = 1.45F;
   private static final float FLOAT_VALUE_3 = 0.42F;
   private static final float FLOAT_VALUE_4 = 0.95F;
   private static final float FLOAT_VALUE_5 = 0.3F;
   private static final float FLOAT_VALUE_6 = 0.3F;
   private static final float FLOAT_VALUE_7 = 0.18F;
   private static final float FLOAT_VALUE_8 = 0.4F;
   private static final float FLOAT_VALUE_9 = 0.16F;
   private static final float FLOAT_VALUE_10 = 3.8F;
   private static final float FLOAT_VALUE_11 = 11.0F;
   private static final float FLOAT_VALUE_12 = 5.5F;
   private static final double DOUBLE_VALUE_4 = 0.36;
   private static final float FLOAT_VALUE_13 = 12.0F;
   private static final float FLOAT_VALUE_14 = 0.5F;
   private static final long TIMESTAMP = System.nanoTime();
   public static final FoundryShaderSetting FOUNDRY_SHADER = new FoundryShaderSetting("Foundry Shader", ShaderSurface.TRAILS);
   private static final float[] FLOATS = new float[13];
   private static final float[] FLOATS_2 = new float[13];
   private static final float[] FLOATS_3 = new float[13];
   private final double[] doubles = new double[64];
   private final double[] doubles2 = new double[64];
   private final double[] doubles3 = new double[64];
   private final float[] floats = new float[64];
   private int intValue;
   private final double[] doubles4 = new double[65];
   private final double[] doubles5 = new double[65];
   private final double[] doubles6 = new double[65];
   private final float[] floats2 = new float[65];
   private final double[] doubles7 = new double[926];
   private final double[] doubles8 = new double[926];
   private final double[] doubles9 = new double[926];
   private final double[] doubles10 = new double[926];
   private final double[] doubles11 = new double[926];
   private final double[] doubles12 = new double[926];
   private final double[] doubles13 = new double[926];
   private final double[] doubles14 = new double[926];
   private final double[] doubles15 = new double[926];
   private final float[] floats3 = new float[926];
   private final float[] floats4 = new float[926];
   private int intValue2;
   private long timestamp;
   private float floatValue = 0.18F;
   private double doubleValue;
   private double doubleValue2;
   private double doubleValue3;
   private float floatValue2;
   private boolean flag;
   private float floatValue3 = 0.95F;
   private final Trails.TrailsState trailsState = new Trails.TrailsState();

   public Trails() {
      TrailsShaderRegistry.invoke();
      this.addSettings(new Setting[]{FOUNDRY_SHADER});
   }

   @Override
   public void onEnable() {
      this.invoke3();
      super.onEnable();
      ShaderBindingRegistry.getINSTANCE().invoke(this, this);
   }

   @Override
   public void onDisable() {
      ShaderBindingRegistry.getINSTANCE().invoke4(this);
      this.invoke3();
      super.onDisable();
   }

   @Override
   public ShaderSurface getESP() {
      return ShaderSurface.TRAILS;
   }

   @Override
   public String resolve() {
      String text = resolve2();
      return text != null && !text.isBlank() ? text : null;
   }

   @Override
   public boolean check() {
      return true;
   }

   public static String resolve2() {
      String text2 = FOUNDRY_SHADER.resolve2();
      return text2 == null ? "" : text2;
   }

   @EventHandler
   public void onWorldJoin(WorldJoinEvent worldJoinEvent) {
      this.invoke3();
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      ShaderBindingRegistry.getINSTANCE().invoke7(this, this);
      if (CLIENT.world != null && CLIENT.player != null) {
         if (CLIENT.options != null && CLIENT.options.getPerspective() != null && !CLIENT.options.getPerspective().isFirstPerson()) {
            long longValue = System.nanoTime();
            float floatValue = this.timestamp == 0L ? 0.0F : Math.min((float)(longValue - this.timestamp) / 1.0E9F, 0.1F);
            this.timestamp = longValue;
            float floatValue2;
            if (CLIENT.player.isGliding()) {
               floatValue2 = 0.3F;
            } else if (CLIENT.player.isSwimming()) {
               floatValue2 = 0.3F;
            } else {
               floatValue2 = 0.95F;
            }

            float floatValue3 = 1.0F - (float)Math.exp(-8.0F * floatValue);
            this.floatValue3 = this.floatValue3 + (floatValue2 - this.floatValue3) * floatValue3;
            Vec3d vec3d = CLIENT.player.getLerpedPos(render3DEvent.getFloatValue());
            double doubleValue = vec3d.x;
            double doubleValue2 = vec3d.y + this.floatValue3;
            double doubleValue3 = vec3d.z;
            Vec3d vec3d2 = CLIENT.player.getVelocity();
            double doubleValue4 = Math.sqrt(vec3d2.x * vec3d2.x + vec3d2.y * vec3d2.y + vec3d2.z * vec3d2.z);
            double doubleValue5 = Math.sqrt(vec3d2.x * vec3d2.x + vec3d2.z * vec3d2.z);
            boolean flag = doubleValue5 > 0.02 || doubleValue4 > 0.045;
            float floatValue4 = (float)Math.min(1.0, doubleValue4 / 0.36);
            float floatValue5 = flag ? 0.18F + 0.4F * floatValue4 : 0.16F;
            float floatValue6 = 1.0F - (float)Math.exp(-3.8F * floatValue);
            this.floatValue = this.floatValue + (floatValue5 - this.floatValue) * floatValue6;
            if (flag) {
               this.doubleValue = doubleValue;
               this.doubleValue2 = doubleValue2;
               this.doubleValue3 = doubleValue3;
               this.flag = true;
               float floatValue7 = 1.0F - (float)Math.exp(-11.0F * floatValue);
               this.floatValue2 = this.floatValue2 + (1.0F - this.floatValue2) * floatValue7;
               this.invoke(doubleValue, doubleValue2, doubleValue3);
            } else {
               this.floatValue2 = this.floatValue2 * (float)Math.exp(-5.5F * floatValue);
               if (this.floatValue2 < 0.01F) {
                  this.floatValue2 = 0.0F;
                  this.flag = false;
               }
            }

            this.invoke2(floatValue);
            if (this.intValue >= 1) {
               Camera camera = CLIENT.gameRenderer.getCamera();
               double doubleValue6 = camera.getPos().x;
               double doubleValue7 = camera.getPos().y;
               double doubleValue8 = camera.getPos().z;
               this.trailsState.invoke(this.measure2(), resolve2());
               this.invoke4();
               if (this.intValue2 >= 2) {
                  this.invoke5();
                  MatrixStack matrices = render3DEvent.getMatrixStack();
                  Matrix4f matrix4f2 = matrices.peek().getPositionMatrix();
                  Immediate immediate = WorldRenderBuffer.getIMMEDIATE();

                  try {
                     VertexConsumer vertexConsumer2 = immediate.getBuffer(TrailsShaderRegistry.getRENDER_LAYER());
                     this.invoke6(vertexConsumer2, matrix4f2, doubleValue6, doubleValue7, doubleValue8, 1.0F, 1.0F);
                     VertexConsumer vertexConsumer3 = immediate.getBuffer(TrailsShaderRegistry.getRENDER_LAYER_2());
                     this.invoke6(vertexConsumer3, matrix4f2, doubleValue6, doubleValue7, doubleValue8, 1.45F, 0.42F);
                  } finally {
                     WorldRenderBuffer.invoke();
                  }
               }
            }
         } else {
            this.invoke3();
         }
      }
   }

   private void invoke(double d, double e, double f) {
      if (this.intValue == 0) {
         this.doubles[0] = d;
         this.doubles2[0] = e;
         this.doubles3[0] = f;
         this.floats[0] = 0.0F;
         this.intValue = 1;
      } else {
         int intValue = this.intValue - 1;
         double doubleValue9 = d - this.doubles[intValue];
         double doubleValue10 = e - this.doubles2[intValue];
         double doubleValue11 = f - this.doubles3[intValue];
         double doubleValue12 = doubleValue9 * doubleValue9 + doubleValue10 * doubleValue10 + doubleValue11 * doubleValue11;
         if (doubleValue12 > 36.0) {
            this.invoke3();
            this.invoke(d, e, f);
         } else if (!(doubleValue12 < 0.0036)) {
            if (this.intValue == 64) {
               System.arraycopy(this.doubles, 1, this.doubles, 0, 63);
               System.arraycopy(this.doubles2, 1, this.doubles2, 0, 63);
               System.arraycopy(this.doubles3, 1, this.doubles3, 0, 63);
               System.arraycopy(this.floats, 1, this.floats, 0, 63);
               this.intValue = 63;
            }

            this.doubles[this.intValue] = d;
            this.doubles2[this.intValue] = e;
            this.doubles3[this.intValue] = f;
            this.floats[this.intValue] = 0.0F;
            this.intValue++;
         }
      }
   }

   private void invoke2(float f) {
      int intValue2 = 0;

      for (int intValue3 = 0; intValue3 < this.intValue; intValue3++) {
         float floatValue8 = this.floats[intValue3] + f;
         if (floatValue8 < this.floatValue) {
            if (intValue2 != intValue3) {
               this.doubles[intValue2] = this.doubles[intValue3];
               this.doubles2[intValue2] = this.doubles2[intValue3];
               this.doubles3[intValue2] = this.doubles3[intValue3];
            }

            this.floats[intValue2] = floatValue8;
            intValue2++;
         }
      }

      this.intValue = intValue2;
   }

   private void invoke3() {
      this.intValue = 0;
      this.intValue2 = 0;
      this.floatValue2 = 0.0F;
      this.flag = false;
   }

   private void invoke4() {
      this.intValue2 = 0;

      for (int intValue4 = 0; intValue4 < this.intValue; intValue4++) {
         this.doubles4[intValue4] = this.doubles[intValue4];
         this.doubles5[intValue4] = this.doubles2[intValue4];
         this.doubles6[intValue4] = this.doubles3[intValue4];
         float floatValue9 = this.floats[intValue4] / Math.max(0.001F, this.floatValue);
         if (floatValue9 < 0.0F) {
            floatValue9 = 0.0F;
         }

         if (floatValue9 > 1.0F) {
            floatValue9 = 1.0F;
         }

         this.floats2[intValue4] = 1.0F - floatValue9 * floatValue9 * (3.0F - 2.0F * floatValue9);
      }

      int intValue5 = this.intValue;
      if (this.intValue > 0 && this.flag && this.floatValue2 > 0.02F) {
         int intValue6 = this.intValue - 1;
         double doubleValue13 = this.doubleValue - this.doubles[intValue6];
         double doubleValue14 = this.doubleValue2 - this.doubles2[intValue6];
         double doubleValue15 = this.doubleValue3 - this.doubles3[intValue6];
         if (doubleValue13 * doubleValue13 + doubleValue14 * doubleValue14 + doubleValue15 * doubleValue15 > 0.00108) {
            this.doubles4[this.intValue] = this.doubleValue;
            this.doubles5[this.intValue] = this.doubleValue2;
            this.doubles6[this.intValue] = this.doubleValue3;
            this.floats2[this.intValue] = this.floatValue2;
            intValue5 = this.intValue + 1;
         }
      }

      if (intValue5 >= 2) {
         this.doubles7[0] = this.doubles4[0];
         this.doubles8[0] = this.doubles5[0];
         this.doubles9[0] = this.doubles6[0];
         this.floats3[0] = this.floats2[0];
         this.intValue2 = 1;

         for (int intValue7 = 0; intValue7 < intValue5 - 1; intValue7++) {
            int intValue8 = Math.max(0, intValue7 - 1);
            int intValue9 = intValue7;
            int intValue10 = intValue7 + 1;
            int intValue11 = Math.min(intValue5 - 1, intValue7 + 2);
            int intValue12 = this.compute(intValue8, intValue7, intValue10, intValue11);

            for (int intValue13 = 1; intValue13 <= intValue12; intValue13++) {
               if (this.intValue2 >= 926) {
                  return;
               }

               float floatValue10 = (float)intValue13 / intValue12;
               this.doubles7[this.intValue2] = measure3(
                  this.doubles4[intValue8], this.doubles4[intValue9], this.doubles4[intValue10], this.doubles4[intValue11], floatValue10
               );
               this.doubles8[this.intValue2] = measure3(
                  this.doubles5[intValue8], this.doubles5[intValue9], this.doubles5[intValue10], this.doubles5[intValue11], floatValue10
               );
               this.doubles9[this.intValue2] = measure3(
                  this.doubles6[intValue8], this.doubles6[intValue9], this.doubles6[intValue10], this.doubles6[intValue11], floatValue10
               );
               this.floats3[this.intValue2] = measure4(this.floats2[intValue9], this.floats2[intValue10], floatValue10);
               this.intValue2++;
            }
         }

         this.floats4[0] = 0.0F;
         float floatValue11 = 0.0F;

         for (int intValue14 = 1; intValue14 < this.intValue2; intValue14++) {
            double doubleValue16 = this.doubles7[intValue14] - this.doubles7[intValue14 - 1];
            double doubleValue17 = this.doubles8[intValue14] - this.doubles8[intValue14 - 1];
            double doubleValue18 = this.doubles9[intValue14] - this.doubles9[intValue14 - 1];
            floatValue11 += (float)Math.sqrt(doubleValue16 * doubleValue16 + doubleValue17 * doubleValue17 + doubleValue18 * doubleValue18);
            this.floats4[intValue14] = floatValue11;
         }

         if (floatValue11 > 1.0E-4F) {
            float floatValue12 = 1.0F / floatValue11;

            for (int intValue15 = 0; intValue15 < this.intValue2; intValue15++) {
               this.floats4[intValue15] = this.floats4[intValue15] * floatValue12;
            }
         } else {
            for (int intValue16 = 0; intValue16 < this.intValue2; intValue16++) {
               this.floats4[intValue16] = this.intValue2 > 1 ? (float)intValue16 / (this.intValue2 - 1) : 0.0F;
            }
         }
      }
   }

   private void invoke5() {
      if (this.intValue2 >= 1) {
         int intValue17 = Math.min(this.intValue2 - 1, 1);
         double doubleValue19 = this.doubles7[intValue17] - this.doubles7[0];
         double doubleValue20 = this.doubles8[intValue17] - this.doubles8[0];
         double doubleValue21 = this.doubles9[intValue17] - this.doubles9[0];
         double doubleValue22 = Math.sqrt(doubleValue19 * doubleValue19 + doubleValue20 * doubleValue20 + doubleValue21 * doubleValue21);
         double doubleValue23;
         double doubleValue24;
         double doubleValue25;
         if (doubleValue22 > 1.0E-6) {
            doubleValue23 = doubleValue19 / doubleValue22;
            doubleValue24 = doubleValue20 / doubleValue22;
            doubleValue25 = doubleValue21 / doubleValue22;
         } else {
            doubleValue23 = 1.0;
            doubleValue24 = 0.0;
            doubleValue25 = 0.0;
         }

         double doubleValue26 = Math.sqrt(doubleValue19 * doubleValue19 + doubleValue21 * doubleValue21);
         double doubleValue27;
         double doubleValue28;
         double doubleValue29;
         if (doubleValue26 > 1.0E-6) {
            doubleValue27 = -doubleValue21 / doubleValue26;
            doubleValue28 = 0.0;
            doubleValue29 = doubleValue19 / doubleValue26;
         } else {
            doubleValue27 = 1.0;
            doubleValue28 = 0.0;
            doubleValue29 = 0.0;
         }

         this.doubles10[0] = doubleValue27;
         this.doubles11[0] = doubleValue28;
         this.doubles12[0] = doubleValue29;
         double doubleValue30 = doubleValue24 * doubleValue29 - doubleValue25 * doubleValue28;
         double doubleValue31 = doubleValue25 * doubleValue27 - doubleValue23 * doubleValue29;
         double doubleValue32 = doubleValue23 * doubleValue28 - doubleValue24 * doubleValue27;
         double doubleValue33 = Math.sqrt(doubleValue30 * doubleValue30 + doubleValue31 * doubleValue31 + doubleValue32 * doubleValue32);
         if (doubleValue33 > 1.0E-6) {
            doubleValue30 /= doubleValue33;
            doubleValue31 /= doubleValue33;
            doubleValue32 /= doubleValue33;
         } else {
            doubleValue30 = 0.0;
            doubleValue31 = 1.0;
            doubleValue32 = 0.0;
         }

         this.doubles13[0] = doubleValue30;
         this.doubles14[0] = doubleValue31;
         this.doubles15[0] = doubleValue32;

         for (int intValue18 = 1; intValue18 < this.intValue2; intValue18++) {
            int intValue19 = intValue18 - 1;
            int intValue20 = Math.min(this.intValue2 - 1, intValue18 + 1);
            double doubleValue34 = this.doubles7[intValue20] - this.doubles7[intValue19];
            double doubleValue35 = this.doubles8[intValue20] - this.doubles8[intValue19];
            double doubleValue36 = this.doubles9[intValue20] - this.doubles9[intValue19];
            double doubleValue37 = Math.sqrt(doubleValue34 * doubleValue34 + doubleValue35 * doubleValue35 + doubleValue36 * doubleValue36);
            if (doubleValue37 < 1.0E-6) {
               this.doubles10[intValue18] = this.doubles10[intValue18 - 1];
               this.doubles11[intValue18] = this.doubles11[intValue18 - 1];
               this.doubles12[intValue18] = this.doubles12[intValue18 - 1];
               this.doubles13[intValue18] = this.doubles13[intValue18 - 1];
               this.doubles14[intValue18] = this.doubles14[intValue18 - 1];
               this.doubles15[intValue18] = this.doubles15[intValue18 - 1];
            } else {
               double doubleValue38 = doubleValue34 / doubleValue37;
               double doubleValue39 = doubleValue35 / doubleValue37;
               double doubleValue40 = doubleValue36 / doubleValue37;
               double doubleValue41 = this.doubles10[intValue18 - 1];
               double doubleValue42 = this.doubles11[intValue18 - 1];
               double doubleValue43 = this.doubles12[intValue18 - 1];
               double doubleValue44 = doubleValue41 * doubleValue38 + doubleValue42 * doubleValue39 + doubleValue43 * doubleValue40;
               doubleValue41 -= doubleValue38 * doubleValue44;
               doubleValue42 -= doubleValue39 * doubleValue44;
               doubleValue43 -= doubleValue40 * doubleValue44;
               double doubleValue45 = Math.sqrt(doubleValue41 * doubleValue41 + doubleValue42 * doubleValue42 + doubleValue43 * doubleValue43);
               if (doubleValue45 > 1.0E-6) {
                  doubleValue41 /= doubleValue45;
                  doubleValue42 /= doubleValue45;
                  doubleValue43 /= doubleValue45;
               } else {
                  double doubleValue46 = Math.sqrt(doubleValue34 * doubleValue34 + doubleValue36 * doubleValue36);
                  if (doubleValue46 > 1.0E-6) {
                     doubleValue41 = -doubleValue36 / doubleValue46;
                     doubleValue42 = 0.0;
                     doubleValue43 = doubleValue34 / doubleValue46;
                  } else {
                     doubleValue41 = 1.0;
                     doubleValue42 = 0.0;
                     doubleValue43 = 0.0;
                  }
               }

               this.doubles10[intValue18] = doubleValue41;
               this.doubles11[intValue18] = doubleValue42;
               this.doubles12[intValue18] = doubleValue43;
               double doubleValue47 = doubleValue39 * doubleValue43 - doubleValue40 * doubleValue42;
               double doubleValue48 = doubleValue40 * doubleValue41 - doubleValue38 * doubleValue43;
               double doubleValue49 = doubleValue38 * doubleValue42 - doubleValue39 * doubleValue41;
               double doubleValue50 = Math.sqrt(doubleValue47 * doubleValue47 + doubleValue48 * doubleValue48 + doubleValue49 * doubleValue49);
               if (doubleValue50 > 1.0E-6) {
                  doubleValue47 /= doubleValue50;
                  doubleValue48 /= doubleValue50;
                  doubleValue49 /= doubleValue50;
               } else {
                  doubleValue47 = 0.0;
                  doubleValue48 = 1.0;
                  doubleValue49 = 0.0;
               }

               this.doubles13[intValue18] = doubleValue47;
               this.doubles14[intValue18] = doubleValue48;
               this.doubles15[intValue18] = doubleValue49;
            }
         }
      }
   }

   private int compute(int i, int j, int k, int l) {
      double doubleValue51 = this.doubles4[j] - this.doubles4[i];
      double doubleValue52 = this.doubles5[j] - this.doubles5[i];
      double doubleValue53 = this.doubles6[j] - this.doubles6[i];
      double doubleValue54 = this.doubles4[k] - this.doubles4[j];
      double doubleValue55 = this.doubles5[k] - this.doubles5[j];
      double doubleValue56 = this.doubles6[k] - this.doubles6[j];
      double doubleValue57 = this.doubles4[l] - this.doubles4[k];
      double doubleValue58 = this.doubles5[l] - this.doubles5[k];
      double doubleValue59 = this.doubles6[l] - this.doubles6[k];
      double doubleValue60 = Math.sqrt(doubleValue51 * doubleValue51 + doubleValue52 * doubleValue52 + doubleValue53 * doubleValue53);
      double doubleValue61 = Math.sqrt(doubleValue54 * doubleValue54 + doubleValue55 * doubleValue55 + doubleValue56 * doubleValue56);
      double doubleValue62 = Math.sqrt(doubleValue57 * doubleValue57 + doubleValue58 * doubleValue58 + doubleValue59 * doubleValue59);
      double doubleValue63 = doubleValue60 > 1.0E-6 && doubleValue61 > 1.0E-6 ? (doubleValue51 * doubleValue54 + doubleValue52 * doubleValue55 + doubleValue53 * doubleValue56) / (doubleValue60 * doubleValue61) : 1.0;
      double doubleValue64 = doubleValue61 > 1.0E-6 && doubleValue62 > 1.0E-6 ? (doubleValue54 * doubleValue57 + doubleValue55 * doubleValue58 + doubleValue56 * doubleValue59) / (doubleValue61 * doubleValue62) : 1.0;
      double doubleValue65 = Math.max(1.0 - doubleValue63, 1.0 - doubleValue64);
      if (doubleValue65 < 0.0) {
         doubleValue65 = 0.0;
      }

      if (doubleValue65 > 2.0) {
         doubleValue65 = 2.0;
      }

      double doubleValue66 = Math.min(1.0, Math.pow(doubleValue65 * 12.0, 0.5));
      int fps = CLIENT.getCurrentFps();
      double quality = fps > 0 && fps < 42 ? 0.48 : fps > 0 && fps < 70 ? 0.72 : 1.0;
      int intValue21 = (int)Math.round(10.0 * doubleValue66 * quality);
      return 3 + intValue21;
   }

   private void invoke6(VertexConsumer vertexConsumer, Matrix4f matrix4f, double d, double e, double f, float g, float h) {
      for (int intValue22 = 0; intValue22 < this.intValue2 - 1; intValue22++) {
         float floatValue13 = this.floats4[intValue22];
         float floatValue14 = this.floats4[intValue22 + 1];
         float floatValue15 = this.floats3[intValue22] * h * this.trailsState.floatValue3;
         float floatValue16 = this.floats3[intValue22 + 1] * h * this.trailsState.floatValue3;
         float floatValue17 = 0.3F * g * this.trailsState.floatValue2 * measure(floatValue13);
         float floatValue18 = 0.3F * g * this.trailsState.floatValue2 * measure(floatValue14);
         int intValue23 = compute3(floatValue15);
         int intValue24 = compute3(floatValue16);
         if ((intValue23 | intValue24) != 0 && (!(floatValue17 <= 1.0E-4F) || !(floatValue18 <= 1.0E-4F))) {
            int intValue25 = this.trailsState.compute(floatValue13);
            int intValue26 = this.trailsState.compute(floatValue14);
            double doubleValue67 = this.doubles7[intValue22] - d;
            double doubleValue68 = this.doubles8[intValue22] - e;
            double doubleValue69 = this.doubles9[intValue22] - f;
            double doubleValue70 = this.doubles7[intValue22 + 1] - d;
            double doubleValue71 = this.doubles8[intValue22 + 1] - e;
            double doubleValue72 = this.doubles9[intValue22 + 1] - f;
            double doubleValue73 = this.doubles10[intValue22];
            double doubleValue74 = this.doubles11[intValue22];
            double doubleValue75 = this.doubles12[intValue22];
            double doubleValue76 = this.doubles10[intValue22 + 1];
            double doubleValue77 = this.doubles11[intValue22 + 1];
            double doubleValue78 = this.doubles12[intValue22 + 1];
            double doubleValue79 = this.doubles13[intValue22];
            double doubleValue80 = this.doubles14[intValue22];
            double doubleValue81 = this.doubles15[intValue22];
            double doubleValue82 = this.doubles13[intValue22 + 1];
            double doubleValue83 = this.doubles14[intValue22 + 1];
            double doubleValue84 = this.doubles15[intValue22 + 1];

            for (int intValue27 = 0; intValue27 < 12; intValue27++) {
               float floatValue19 = FLOATS[intValue27];
               float floatValue20 = FLOATS_2[intValue27];
               float floatValue21 = FLOATS[intValue27 + 1];
               float floatValue22 = FLOATS_2[intValue27 + 1];
               float floatValue23 = FLOATS_3[intValue27];
               float floatValue24 = FLOATS_3[intValue27 + 1];
               double doubleValue85 = floatValue19 * doubleValue73 + floatValue20 * doubleValue79;
               double doubleValue86 = floatValue19 * doubleValue74 + floatValue20 * doubleValue80;
               double doubleValue87 = floatValue19 * doubleValue75 + floatValue20 * doubleValue81;
               double doubleValue88 = floatValue21 * doubleValue73 + floatValue22 * doubleValue79;
               double doubleValue89 = floatValue21 * doubleValue74 + floatValue22 * doubleValue80;
               double doubleValue90 = floatValue21 * doubleValue75 + floatValue22 * doubleValue81;
               double doubleValue91 = floatValue19 * doubleValue76 + floatValue20 * doubleValue82;
               double doubleValue92 = floatValue19 * doubleValue77 + floatValue20 * doubleValue83;
               double doubleValue93 = floatValue19 * doubleValue78 + floatValue20 * doubleValue84;
               double doubleValue94 = floatValue21 * doubleValue76 + floatValue22 * doubleValue82;
               double doubleValue95 = floatValue21 * doubleValue77 + floatValue22 * doubleValue83;
               double doubleValue96 = floatValue21 * doubleValue78 + floatValue22 * doubleValue84;
               this.invoke7(
                  vertexConsumer,
                  matrix4f,
                  doubleValue67 + doubleValue85 * floatValue17,
                  doubleValue68 + doubleValue86 * floatValue17,
                  doubleValue69 + doubleValue87 * floatValue17,
                  floatValue13,
                  floatValue23,
                  intValue25,
                  intValue23,
                  doubleValue85,
                  doubleValue86,
                  doubleValue87
               );
               this.invoke7(
                  vertexConsumer,
                  matrix4f,
                  doubleValue67 + doubleValue88 * floatValue17,
                  doubleValue68 + doubleValue89 * floatValue17,
                  doubleValue69 + doubleValue90 * floatValue17,
                  floatValue13,
                  floatValue24,
                  intValue25,
                  intValue23,
                  doubleValue88,
                  doubleValue89,
                  doubleValue90
               );
               this.invoke7(
                  vertexConsumer,
                  matrix4f,
                  doubleValue70 + doubleValue94 * floatValue18,
                  doubleValue71 + doubleValue95 * floatValue18,
                  doubleValue72 + doubleValue96 * floatValue18,
                  floatValue14,
                  floatValue24,
                  intValue26,
                  intValue24,
                  doubleValue94,
                  doubleValue95,
                  doubleValue96
               );
               this.invoke7(
                  vertexConsumer,
                  matrix4f,
                  doubleValue70 + doubleValue91 * floatValue18,
                  doubleValue71 + doubleValue92 * floatValue18,
                  doubleValue72 + doubleValue93 * floatValue18,
                  floatValue14,
                  floatValue23,
                  intValue26,
                  intValue24,
                  doubleValue91,
                  doubleValue92,
                  doubleValue93
               );
            }
         }
      }
   }

   private void invoke7(
      VertexConsumer vertexConsumer, Matrix4f matrix4f, double d, double e, double f, float g, float h, int i, int j, double k, double l, double m
   ) {
      vertexConsumer.vertex(matrix4f, (float)d, (float)e, (float)f)
         .texture(g, h)
         .color(compute4(i), compute5(i), compute6(i), j)
         .normal((float)k, (float)l, (float)m);
   }

   private static float measure(float f) {
      float floatValue25 = measure5(0.0F, 0.42F, f);
      float floatValue26 = 1.0F - measure5(0.92F, 1.0F, f) * 0.5F;
      return floatValue25 * floatValue26;
   }

   private float measure2() {
      return (float)(System.nanoTime() - TIMESTAMP) / 1.0E9F;
   }

   private static double measure3(double d, double e, double f, double g, float h) {
      double doubleValue97 = h * h;
      double doubleValue98 = doubleValue97 * h;
      return 0.5 * (2.0 * e + (-d + f) * h + (2.0 * d - 5.0 * e + 4.0 * f - g) * doubleValue97 + (-d + 3.0 * e - 3.0 * f + g) * doubleValue98);
   }

   private static float measure4(float f, float g, float h) {
      return f + (g - f) * h;
   }

   private static float measure5(float f, float g, float h) {
      float floatValue27 = measure6((h - f) / (g - f), 0.0F, 1.0F);
      return floatValue27 * floatValue27 * (3.0F - 2.0F * floatValue27);
   }

   static float measure6(float f, float g, float h) {
      return f < g ? g : (f > h ? h : f);
   }

   private static int compute2(int i, int j, int k) {
      return i < j ? j : (i > k ? k : i);
   }

   private static int compute3(float f) {
      return compute2(Math.round(measure6(f, 0.0F, 1.0F) * 255.0F), 0, 255);
   }

   private static int compute4(int i) {
      return i >> 16 & 0xFF;
   }

   private static int compute5(int i) {
      return i >> 8 & 0xFF;
   }

   private static int compute6(int i) {
      return i & 0xFF;
   }

   static int compute7(int i, int j, float f) {
      float floatValue28 = measure6(f, 0.0F, 1.0F);
      int intValue28 = Math.round(compute4(i) + (compute4(j) - compute4(i)) * floatValue28);
      int intValue29 = Math.round(compute5(i) + (compute5(j) - compute5(i)) * floatValue28);
      int intValue30 = Math.round(compute6(i) + (compute6(j) - compute6(i)) * floatValue28);
      return intValue28 << 16 | intValue29 << 8 | intValue30;
   }

   static {
      for (int intValue31 = 0; intValue31 <= 12; intValue31++) {
         double doubleValue99 = (Math.PI * 2) * intValue31 / 12.0;
         FLOATS[intValue31] = (float)Math.cos(doubleValue99);
         FLOATS_2[intValue31] = (float)Math.sin(doubleValue99);
         FLOATS_3[intValue31] = intValue31 / 12.0F;
      }
   }

   static final class TrailsState {
      private static final int[] INTS = new int[]{16747247, 16754396, 8648959, 11141102, 16747247};
      private static final int[] INTS_2 = new int[]{6750183, 6014975, 4688895, 11730932, 6750183};
      private static final int[] INTS_3 = new int[]{16773227, 16751954, 16736157, 9304063, 16773227};
      private static final int[] INTS_4 = new int[]{11141048, 6485458, 8228095, 16755188, 11141048};
      private static final int[] INTS_5 = new int[]{8257383, 3405823, 16773210, 16727538, 8257383};
      private static final int[] INTS_6 = new int[]{16754632, 16769167, 11000063, 14067711, 16754632};
      private static final int[] INTS_7 = new int[]{8033279, 11561983, 5963734, 16736142, 8033279};
      private static final int[] INTS_8 = new int[]{16757594, 16739146, 16732041, 13995263, 16757594};
      private static final int[] INTS_9 = new int[]{14089215, 9169663, 9149951, 16777215, 14089215};
      private static final int[] INTS_10 = new int[]{16736109, 16770140, 6160312, 7179519, 16736109};
      private static final int[] INTS_11 = new int[]{14001919, 16752603, 7733222, 16773260, 14001919};
      private static final int[] INTS_12 = new int[]{13172552, 16773466, 3732223, 16735457, 13172552};
      private Theme theme = Theme.PORTED;
      private int[] ints;
      private int intValue = 7316991;
      private float floatValue;
      float floatValue2 = 1.0F;
      float floatValue3 = 1.0F;

      void invoke(float f, String string) {
         this.floatValue2 = 1.0F;
         this.floatValue3 = 1.0F;
         if (!this.check(f, string)) {
            this.theme = Theme.CUSTOM;
            this.ints = null;
            this.floatValue = f * 0.06F;
            if (this.ints == null) {
               this.intValue = fun.enoughvisuals.systems.modules.modules.client.CustomTheme.hudAccent() & 16777215;
            }
         }
      }

      int compute(float f) {
         return this.ints != null ? compute6(this.ints, this.floatValue + (1.0F - f) * 0.42F) : this.intValue;
      }

      private boolean check(float f, String string) {
         if (string != null && !string.isBlank()) {
            ShaderPresetRegistry shaderPresetRegistry = ShaderPresetRegistry.getINSTANCE();
            if (!shaderPresetRegistry.check2(string)) {
               return false;
            } else {
               ShaderNode shaderNode = shaderPresetRegistry.resolve4(string);
               ShaderBuildResult shaderBuildResult = shaderPresetRegistry.resolve2(string);
               List items = shaderPresetRegistry.resolve11(string);
               Map valuesByKey = shaderPresetRegistry.resolve13(string);
               int[] intValues = new int[4];
               int intValue32 = 0;

               for (ShaderUniformSpec shaderUniformSpec : (List<ShaderUniformSpec>)items) {
                  if (shaderUniformSpec.kind() == ShaderUniformSpec.ShaderUniformSpecState.COLOR && intValue32 < intValues.length) {
                     float[] floatValues = (float[])valuesByKey.get(shaderUniformSpec.uniformName());
                     intValues[intValue32++] = compute2(floatValues == null ? shaderUniformSpec.defaults() : floatValues);
                  }
               }

               if (intValue32 == 0) {
                  int intValue33 = ((shaderNode == null ? string : shaderNode.getPreview() + string) + (shaderBuildResult == null ? "" : shaderBuildResult.hash())).hashCode();
                  intValues[intValue32++] = compute4(intValue33, 0.0F);
                  intValues[intValue32++] = compute4(intValue33, 0.31F);
                  intValues[intValue32++] = compute4(intValue33, 0.63F);
               }

               if (intValue32 == 1) {
                  int intValue34 = intValues[0];
                  this.ints = new int[]{
                     Trails.compute7(intValue34, 16777215, 0.44F),
                     intValue34,
                     Trails.compute7(intValue34, 7796735, 0.36F),
                     Trails.compute7(intValue34, 16741065, 0.3F),
                     Trails.compute7(intValue34, 16777215, 0.44F)
                  };
               } else if (intValue32 == 2) {
                  this.ints = new int[]{
                     intValues[0], Trails.compute7(intValues[0], intValues[1], 0.42F), intValues[1], Trails.compute7(intValues[1], 16777215, 0.34F), intValues[0]
                  };
               } else if (intValue32 == 3) {
                  this.ints = new int[]{intValues[0], intValues[1], intValues[2], Trails.compute7(intValues[2], 16777215, 0.32F), intValues[0]};
               } else {
                  this.ints = new int[]{intValues[0], intValues[1], intValues[2], intValues[3], intValues[0]};
               }

               float floatValue29 = measure(items, valuesByKey, 0.5F, "width", "radius", "size", "thick");
               float floatValue30 = measure(items, valuesByKey, 0.66F, "opacity", "alpha", "power", "glow", "intensity");
               float floatValue31 = measure(items, valuesByKey, 0.5F, "flow", "speed", "phase", "time");
               this.floatValue2 = Trails.measure6(floatValue29 == 0.5F ? 1.0F : 0.68F + floatValue29 * 1.22F, 0.58F, 1.9F);
               this.floatValue3 = Trails.measure6(0.62F + floatValue30 * 0.82F, 0.52F, 1.48F);
               this.floatValue = f * (0.034F + floatValue31 * 0.09F);
               return true;
            }
         } else {
            return false;
         }
      }

      private static float measure(List<ShaderUniformSpec> list, Map<String, float[]> map, float f, String... strings) {
         if (list != null && !list.isEmpty()) {
            for (ShaderUniformSpec shaderUniformSpec2 : list) {
               if (shaderUniformSpec2.kind() == ShaderUniformSpec.ShaderUniformSpecState.FLOAT) {
                  String text3 = (shaderUniformSpec2.name() + " " + shaderUniformSpec2.uniformName()).toLowerCase();
                  boolean flag2 = false;

                  for (String text4 : strings) {
                     if (text4 != null && text3.contains(text4)) {
                        flag2 = true;
                        break;
                     }
                  }

                  if (flag2) {
                     float[] floatValues2 = map == null ? null : (float[])map.get(shaderUniformSpec2.uniformName());
                     float floatValue32 = floatValues2 != null && floatValues2.length != 0 ? floatValues2[0] : shaderUniformSpec2.defaultFloat();
                     float floatValue33 = shaderUniformSpec2.maximum() - shaderUniformSpec2.minimum();
                     if (Float.isFinite(floatValue32) && !(floatValue33 <= 1.0E-6F)) {
                        return Trails.measure6((floatValue32 - shaderUniformSpec2.minimum()) / floatValue33, 0.0F, 1.0F);
                     }

                     return f;
                  }
               }
            }

            return f;
         } else {
            return f;
         }
      }

      private static int compute2(float[] fs) {
         float floatValue34 = fs != null && fs.length > 0 ? fs[0] : 1.0F;
         float floatValue35 = fs != null && fs.length > 1 ? fs[1] : 1.0F;
         float floatValue36 = fs != null && fs.length > 2 ? fs[2] : 1.0F;
         return compute3(floatValue34) << 16 | compute3(floatValue35) << 8 | compute3(floatValue36);
      }

      private static int compute3(float f) {
         return !Float.isFinite(f) ? 0 : Math.max(0, Math.min(255, Math.round(f * 255.0F)));
      }

      private static int compute4(int i, float f) {
         int intValue35 = i ^ -1640531527;
         intValue35 ^= intValue35 >>> 16;
         intValue35 *= 2146121005;
         intValue35 ^= intValue35 >>> 15;
         intValue35 *= -2073254261;
         intValue35 ^= intValue35 >>> 16;
         float floatValue37 = ((intValue35 & 16777215) / 1.6777215E7F + f) % 1.0F;
         return compute5(floatValue37, 0.62F, 1.0F);
      }

      private static int compute5(float f, float g, float h) {
         f -= (float)Math.floor(f);
         float floatValue38 = f * 6.0F;
         int intValue36 = (int)Math.floor(floatValue38);
         float floatValue39 = h * (1.0F - g);
         float floatValue40 = h * (1.0F - g * (floatValue38 - intValue36));
         float floatValue41 = h * (1.0F - g * (1.0F - (floatValue38 - intValue36)));
         float floatValue42;
         float floatValue43;
         float floatValue44;
         switch (intValue36 % 6) {
            case 0:
               floatValue42 = h;
               floatValue43 = floatValue41;
               floatValue44 = floatValue39;
               break;
            case 1:
               floatValue42 = floatValue40;
               floatValue43 = h;
               floatValue44 = floatValue39;
               break;
            case 2:
               floatValue42 = floatValue39;
               floatValue43 = h;
               floatValue44 = floatValue41;
               break;
            case 3:
               floatValue42 = floatValue39;
               floatValue43 = floatValue40;
               floatValue44 = h;
               break;
            case 4:
               floatValue42 = floatValue41;
               floatValue43 = floatValue39;
               floatValue44 = h;
               break;
            default:
               floatValue42 = h;
               floatValue43 = floatValue39;
               floatValue44 = floatValue40;
         }

         return compute3(floatValue42) << 16 | compute3(floatValue43) << 8 | compute3(floatValue44);
      }

      private static int compute6(int[] is, float f) {
         float floatValue45 = f - (float)Math.floor(f);
         float floatValue46 = floatValue45 * (is.length - 1);
         int intValue37 = Math.min(is.length - 2, Math.max(0, (int)Math.floor(floatValue46)));
         return Trails.compute7(is[intValue37] & 16777215, is[intValue37 + 1] & 16777215, floatValue46 - intValue37);
      }

      private static int[] resolve(Theme theme) {
         return switch (theme) {
            case ASTOLFO_RAINBOW -> INTS;
            case LAGUNE_RAINBOW -> INTS_2;
            case HALF_RAINBOW -> INTS_3;
            case AURORA_RAINBOW -> INTS_4;
            case NEON_RAINBOW -> INTS_5;
            case BLOSSOM_RAINBOW -> INTS_6;
            case ABYSS_RAINBOW -> INTS_7;
            case SUNSET_RAINBOW -> INTS_8;
            case GLACIER_RAINBOW -> INTS_9;
            case CHROMA_RAINBOW -> INTS_10;
            case DREAM_RAINBOW -> INTS_11;
            case TOXIC_RAINBOW -> INTS_12;
            default -> null;
         };
      }
   }
}
