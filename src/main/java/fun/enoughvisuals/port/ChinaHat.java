package fun.enoughvisuals.port;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import fun.enoughvisuals.port.api.Module;
import fun.enoughvisuals.port.api.ModuleRegister;

@ModuleRegister(
   name = "ChinaHat",
   description = "Добавляет над головой игроков декоративную шляпу в виде конуса, которая будет повторять цветовую схему выбранной темы.",
   category = Category.Visuals
)
public final class ChinaHat extends Module {
   private static final int INT_VALUE = 256;
   private static final int INT_VALUE_2 = 18;
   private static final float FLOAT_VALUE = 0.0625F;
   private static final float FLOAT_VALUE_2 = (float) (Math.PI * 2);
   private static final float FLOAT_VALUE_3 = 0.296875F;
   private static final float FLOAT_VALUE_4 = (float)Math.sqrt(0.17626953F);
   private static final float FLOAT_VALUE_5 = FLOAT_VALUE_4 + 0.14F;
   private static final float FLOAT_VALUE_6 = 0.286F;
   private static final float FLOAT_VALUE_7 = -0.503125F;
   private static final float FLOAT_VALUE_8 = -0.789125F;
   private static final float FLOAT_VALUE_9 = FLOAT_VALUE_5 * 0.07F;
   private static final float FLOAT_VALUE_10 = -0.769105F;
   private static final float FLOAT_VALUE_11 = 0.0F;
   private static final float FLOAT_VALUE_12 = 1.0F;
   private static final float FLOAT_VALUE_13 = 2.0F;
   private static final SpringSpec SPRING_SPEC = new SpringSpec(0.066F, 0.68F, 0.001F, 0.001F);
   private static final float[] FLOATS = new float[257];
   private static final float[] FLOATS_2 = new float[257];
   private static final float[] FLOATS_3 = new float[19];
   private static final float[] FLOATS_4 = new float[19];
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
   private final SpringAnimation springAnimation = new SpringAnimation(0.0F);
   private final Matrix4f matrix4f = new Matrix4f();
   private final Matrix3f matrix3f = new Matrix3f();
   private final Quaternionf quaternionf = new Quaternionf();
   private final ChinaHat.ChinaHatState chinaHatState = new ChinaHat.ChinaHatState();

   public ChinaHat() {
      ChinaHatRenderLayer.invoke();
   }

   @Override
   public void onEnable() {
      this.springAnimation.invoke(0.0F);
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.springAnimation.invoke(0.0F);
      super.onDisable();
   }

   public static void renderForPlayer(
      PlayerEntityRenderState playerEntityRenderState,
      PlayerEntityModel playerEntityModel,
      MatrixStack matrixStack,
      VertexConsumerProvider vertexConsumerProvider,
      int i
   ) {
      ChinaHat chinaHat = VisualPortRegistry.chinaHat;
      if (chinaHat != null) {
         chinaHat.invoke(playerEntityRenderState, playerEntityModel, matrixStack, vertexConsumerProvider);
      }
   }

   private void invoke(
      PlayerEntityRenderState playerEntityRenderState,
      PlayerEntityModel playerEntityModel,
      MatrixStack matrixStack,
      VertexConsumerProvider vertexConsumerProvider
   ) {
      if (this.enabled
         && CLIENT != null
         && CLIENT.world != null
         && CLIENT.player != null
         && playerEntityRenderState != null
         && playerEntityModel != null
         && matrixStack != null
         && vertexConsumerProvider != null) {
         if (playerEntityRenderState.id == CLIENT.player.getId()) {
            if (CLIENT.options.getPerspective() != Perspective.FIRST_PERSON) {
               if (!playerEntityRenderState.spectator && !playerEntityRenderState.invisible && !playerEntityRenderState.invisibleToPlayer) {
                  float floatValue = this.springAnimation.measure(1.0F, SPRING_SPEC);
                  if (!(floatValue <= 0.001F)) {
                     this.chinaHatState.invoke(playerEntityRenderState.age);
                     Entry entry = matrixStack.peek();
                     this.matrix4f.set(entry.getPositionMatrix());
                     this.matrix3f.set(entry.getNormalMatrix());

                     try {
                        this.invoke2(playerEntityModel.head, matrixStack);
                        Entry entry2 = matrixStack.peek();
                        Matrix4f matrix4f2 = entry2.getPositionMatrix();
                        Matrix3f matrix3f2 = entry2.getNormalMatrix();
                        VertexConsumer vertexConsumer2 = vertexConsumerProvider.getBuffer(ChinaHatRenderLayer.getRENDER_LAYER());
                        this.invoke3(vertexConsumer2, matrix4f2, matrix3f2, floatValue);
                        this.invoke4(vertexConsumer2, matrix4f2, matrix3f2, floatValue, false);
                        this.invoke5(vertexConsumer2, matrix4f2, matrix3f2, floatValue);
                        VertexConsumer vertexConsumer3 = vertexConsumerProvider.getBuffer(ChinaHatRenderLayer.getRENDER_LAYER_2());
                        this.invoke6(vertexConsumer3, matrix4f2, matrix3f2, floatValue);
                        this.invoke4(vertexConsumer3, matrix4f2, matrix3f2, floatValue, true);
                        this.invoke7(vertexConsumer3, matrix4f2, matrix3f2, floatValue);
                     } finally {
                        entry.getPositionMatrix().set(this.matrix4f);
                        entry.getNormalMatrix().set(this.matrix3f);
                     }
                  }
               }
            }
         }
      }
   }

   private void invoke2(ModelPart modelPart, MatrixStack matrixStack) {
      matrixStack.translate(modelPart.pivotX * 0.0625F, modelPart.pivotY * 0.0625F, modelPart.pivotZ * 0.0625F);
      if (modelPart.pitch != 0.0F || modelPart.yaw != 0.0F || modelPart.roll != 0.0F) {
         this.quaternionf.rotationZYX(modelPart.roll, modelPart.yaw, modelPart.pitch);
         matrixStack.multiply(this.quaternionf);
      }

      if (modelPart.xScale != 1.0F || modelPart.yScale != 1.0F || modelPart.zScale != 1.0F) {
         matrixStack.scale(modelPart.xScale, modelPart.yScale, modelPart.zScale);
      }
   }

   private void invoke3(VertexConsumer vertexConsumer, Matrix4f matrix4f, Matrix3f matrix3f, float f) {
      int intValue = this.chinaHatState.intValue;
      int intValue2 = this.chinaHatState.intValue2;
      int intValue3 = compute2(intValue);
      int intValue4 = compute3(intValue);
      int intValue5 = compute4(intValue);
      int intValue6 = compute2(intValue2);
      int intValue7 = compute3(intValue2);
      int intValue8 = compute4(intValue2);
      float floatValue2 = f;

      for (int intValue9 = 0; intValue9 < 256; intValue9++) {
         int intValue10 = intValue9 + 1;
         float floatValue3 = intValue9 / 256.0F;
         float floatValue4 = intValue10 / 256.0F;
         float floatValue5 = FLOATS_2[intValue9];
         float floatValue6 = FLOATS[intValue9];
         float floatValue7 = FLOATS_2[intValue10];
         float floatValue8 = FLOATS[intValue10];
         this.invoke9(
            vertexConsumer,
            matrix4f,
            matrix3f,
            floatValue5 * FLOAT_VALUE_9,
            -0.769105F,
            floatValue6 * FLOAT_VALUE_9,
            floatValue5 * 0.286F,
            -FLOAT_VALUE_5,
            floatValue6 * 0.286F,
            floatValue3,
            0.035F,
            intValue3,
            intValue4,
            intValue5,
            floatValue2 * 0.58F,
            floatValue7 * FLOAT_VALUE_9,
            -0.769105F,
            floatValue8 * FLOAT_VALUE_9,
            floatValue7 * 0.286F,
            -FLOAT_VALUE_5,
            floatValue8 * 0.286F,
            floatValue4,
            0.035F,
            intValue3,
            intValue4,
            intValue5,
            floatValue2 * 0.58F,
            floatValue7 * FLOAT_VALUE_5,
            -0.503125F,
            floatValue8 * FLOAT_VALUE_5,
            floatValue7 * 0.286F,
            -FLOAT_VALUE_5,
            floatValue8 * 0.286F,
            floatValue4,
            0.985F,
            intValue6,
            intValue7,
            intValue8,
            floatValue2,
            floatValue5 * FLOAT_VALUE_5,
            -0.503125F,
            floatValue6 * FLOAT_VALUE_5,
            floatValue5 * 0.286F,
            -FLOAT_VALUE_5,
            floatValue6 * 0.286F,
            floatValue3,
            0.985F,
            intValue6,
            intValue7,
            intValue8,
            floatValue2
         );
      }
   }

   private void invoke4(VertexConsumer vertexConsumer, Matrix4f matrix4f, Matrix3f matrix3f, float f, boolean bl) {
      int intValue11 = this.chinaHatState.intValue;
      int intValue12 = this.chinaHatState.intValue2;
      int intValue13 = compute2(intValue11);
      int intValue14 = compute3(intValue11);
      int intValue15 = compute4(intValue11);
      int intValue16 = compute2(intValue12);
      int intValue17 = compute3(intValue12);
      int intValue18 = compute4(intValue12);
      float floatValue9 = bl ? 0.055F : 0.085F;
      float floatValue10 = bl ? 0.235F : 0.155F;
      float floatValue11 = FLOAT_VALUE_5 * floatValue9;
      float floatValue12 = FLOAT_VALUE_5 * floatValue10;
      float floatValue13 = -0.789125F + 0.286F * floatValue9;
      float floatValue14 = -0.789125F + 0.286F * floatValue10;
      float floatValue15 = bl ? 2.0F : 1.0F;
      float floatValue16 = f * (bl ? 0.42F : 0.68F);

      for (int intValue19 = 0; intValue19 < 256; intValue19++) {
         int intValue20 = intValue19 + 1;
         float floatValue17 = intValue19 / 256.0F;
         float floatValue18 = intValue20 / 256.0F;
         float floatValue19 = FLOATS_2[intValue19];
         float floatValue20 = FLOATS[intValue19];
         float floatValue21 = FLOATS_2[intValue20];
         float floatValue22 = FLOATS[intValue20];
         this.invoke9(
            vertexConsumer,
            matrix4f,
            matrix3f,
            floatValue19 * floatValue11,
            floatValue13,
            floatValue20 * floatValue11,
            floatValue19 * 0.286F,
            -FLOAT_VALUE_5,
            floatValue20 * 0.286F,
            floatValue17,
            floatValue15 + 0.055F,
            intValue13,
            intValue14,
            intValue15,
            floatValue16 * 0.48F,
            floatValue21 * floatValue11,
            floatValue13,
            floatValue22 * floatValue11,
            floatValue21 * 0.286F,
            -FLOAT_VALUE_5,
            floatValue22 * 0.286F,
            floatValue18,
            floatValue15 + 0.055F,
            intValue13,
            intValue14,
            intValue15,
            floatValue16 * 0.48F,
            floatValue21 * floatValue12,
            floatValue14,
            floatValue22 * floatValue12,
            floatValue21 * 0.286F,
            -FLOAT_VALUE_5,
            floatValue22 * 0.286F,
            floatValue18,
            floatValue15 + 0.82F,
            intValue16,
            intValue17,
            intValue18,
            floatValue16,
            floatValue19 * floatValue12,
            floatValue14,
            floatValue20 * floatValue12,
            floatValue19 * 0.286F,
            -FLOAT_VALUE_5,
            floatValue20 * 0.286F,
            floatValue17,
            floatValue15 + 0.82F,
            intValue16,
            intValue17,
            intValue18,
            floatValue16
         );
      }
   }

   private void invoke5(VertexConsumer vertexConsumer, Matrix4f matrix4f, Matrix3f matrix3f, float f) {
      int intValue21 = this.chinaHatState.intValue;
      int intValue22 = this.chinaHatState.intValue2;
      int intValue23 = compute2(intValue21);
      int intValue24 = compute3(intValue21);
      int intValue25 = compute4(intValue21);
      int intValue26 = compute2(intValue22);
      int intValue27 = compute3(intValue22);
      int intValue28 = compute4(intValue22);
      float floatValue23 = FLOAT_VALUE_5 * 0.972F;
      float floatValue24 = FLOAT_VALUE_5 * 1.052F;
      float floatValue25 = -0.500625F;
      float floatValue26 = f;

      for (int intValue29 = 0; intValue29 < 256; intValue29++) {
         int intValue30 = intValue29 + 1;
         float floatValue27 = intValue29 / 256.0F;
         float floatValue28 = intValue30 / 256.0F;
         float floatValue29 = FLOATS_2[intValue29];
         float floatValue30 = FLOATS[intValue29];
         float floatValue31 = FLOATS_2[intValue30];
         float floatValue32 = FLOATS[intValue30];
         this.invoke9(
            vertexConsumer,
            matrix4f,
            matrix3f,
            floatValue29 * floatValue23,
            floatValue25,
            floatValue30 * floatValue23,
            0.0F,
            -1.0F,
            0.0F,
            floatValue27,
            1.1800001F,
            intValue23,
            intValue24,
            intValue25,
            floatValue26 * 0.2F,
            floatValue31 * floatValue23,
            floatValue25,
            floatValue32 * floatValue23,
            0.0F,
            -1.0F,
            0.0F,
            floatValue28,
            1.1800001F,
            intValue23,
            intValue24,
            intValue25,
            floatValue26 * 0.2F,
            floatValue31 * floatValue24,
            floatValue25,
            floatValue32 * floatValue24,
            floatValue31,
            -0.045F,
            floatValue32,
            floatValue28,
            1.9200001F,
            intValue26,
            intValue27,
            intValue28,
            floatValue26,
            floatValue29 * floatValue24,
            floatValue25,
            floatValue30 * floatValue24,
            floatValue29,
            -0.045F,
            floatValue30,
            floatValue27,
            1.9200001F,
            intValue26,
            intValue27,
            intValue28,
            floatValue26
         );
      }
   }

   private void invoke6(VertexConsumer vertexConsumer, Matrix4f matrix4f, Matrix3f matrix3f, float f) {
      int intValue31 = this.chinaHatState.intValue;
      int intValue32 = this.chinaHatState.intValue2;
      int intValue33 = compute2(intValue31);
      int intValue34 = compute3(intValue31);
      int intValue35 = compute4(intValue31);
      int intValue36 = compute2(intValue32);
      int intValue37 = compute3(intValue32);
      int intValue38 = compute4(intValue32);
      float floatValue33 = FLOAT_VALUE_5 * 1.072F;
      float floatValue34 = FLOAT_VALUE_9 * 1.36F;
      float floatValue35 = -0.77210504F;
      float floatValue36 = -0.504625F;
      float floatValue37 = f * 0.38F;

      for (int intValue39 = 0; intValue39 < 256; intValue39++) {
         int intValue40 = intValue39 + 1;
         float floatValue38 = intValue39 / 256.0F;
         float floatValue39 = intValue40 / 256.0F;
         float floatValue40 = FLOATS_2[intValue39];
         float floatValue41 = FLOATS[intValue39];
         float floatValue42 = FLOATS_2[intValue40];
         float floatValue43 = FLOATS[intValue40];
         this.invoke9(
            vertexConsumer,
            matrix4f,
            matrix3f,
            floatValue40 * floatValue34,
            floatValue35,
            floatValue41 * floatValue34,
            floatValue40 * 0.286F,
            -floatValue33,
            floatValue41 * 0.286F,
            floatValue38,
            2.035F,
            intValue33,
            intValue34,
            intValue35,
            floatValue37 * 0.1F,
            floatValue42 * floatValue34,
            floatValue35,
            floatValue43 * floatValue34,
            floatValue42 * 0.286F,
            -floatValue33,
            floatValue43 * 0.286F,
            floatValue39,
            2.035F,
            intValue33,
            intValue34,
            intValue35,
            floatValue37 * 0.1F,
            floatValue42 * floatValue33,
            floatValue36,
            floatValue43 * floatValue33,
            floatValue42 * 0.286F,
            -floatValue33,
            floatValue43 * 0.286F,
            floatValue39,
            2.325F,
            intValue36,
            intValue37,
            intValue38,
            floatValue37,
            floatValue40 * floatValue33,
            floatValue36,
            floatValue41 * floatValue33,
            floatValue40 * 0.286F,
            -floatValue33,
            floatValue41 * 0.286F,
            floatValue38,
            2.325F,
            intValue36,
            intValue37,
            intValue38,
            floatValue37
         );
      }
   }

   private void invoke7(VertexConsumer vertexConsumer, Matrix4f matrix4f, Matrix3f matrix3f, float f) {
      int intValue41 = this.chinaHatState.intValue;
      int intValue42 = this.chinaHatState.intValue2;
      int intValue43 = compute2(intValue41);
      int intValue44 = compute3(intValue41);
      int intValue45 = compute4(intValue41);
      int intValue46 = compute2(intValue42);
      int intValue47 = compute3(intValue42);
      int intValue48 = compute4(intValue42);
      float floatValue44 = FLOAT_VALUE_5 * 0.97F;
      float floatValue45 = FLOAT_VALUE_5 * 1.075F;
      float floatValue46 = FLOAT_VALUE_5 * 1.02F;
      float floatValue47 = FLOAT_VALUE_5 * 0.44F;
      float floatValue48 = 0.122F;
      float floatValue49 = -0.500125F;
      float floatValue50 = f * 0.94F;
      float floatValue51 = f * 0.34F;

      for (int intValue49 = 0; intValue49 < 256; intValue49++) {
         int intValue50 = intValue49 + 1;
         float floatValue52 = intValue49 / 256.0F;
         float floatValue53 = intValue50 / 256.0F;
         float floatValue54 = FLOATS_2[intValue49];
         float floatValue55 = FLOATS[intValue49];
         float floatValue56 = FLOATS_2[intValue50];
         float floatValue57 = FLOATS[intValue50];
         this.invoke9(
            vertexConsumer,
            matrix4f,
            matrix3f,
            floatValue54 * floatValue44,
            floatValue49 - 0.0015F,
            floatValue55 * floatValue44,
            0.0F,
            -1.0F,
            0.0F,
            floatValue52,
            2.02F,
            intValue43,
            intValue44,
            intValue45,
            floatValue50 * 0.42F,
            floatValue56 * floatValue44,
            floatValue49 - 0.0015F,
            floatValue57 * floatValue44,
            0.0F,
            -1.0F,
            0.0F,
            floatValue53,
            2.02F,
            intValue43,
            intValue44,
            intValue45,
            floatValue50 * 0.42F,
            floatValue56 * floatValue45,
            floatValue49 - 0.0015F,
            floatValue57 * floatValue45,
            floatValue56,
            -0.045F,
            floatValue57,
            floatValue53,
            2.72F,
            intValue46,
            intValue47,
            intValue48,
            floatValue50,
            floatValue54 * floatValue45,
            floatValue49 - 0.0015F,
            floatValue55 * floatValue45,
            floatValue54,
            -0.045F,
            floatValue55,
            floatValue52,
            2.72F,
            intValue46,
            intValue47,
            intValue48,
            floatValue50
         );

         for (int intValue51 = 0; intValue51 < 18; intValue51++) {
            float floatValue58 = FLOATS_4[intValue51];
            float floatValue59 = FLOATS_3[intValue51];
            float floatValue60 = floatValue46 + floatValue58 * floatValue47;
            float floatValue61 = floatValue49 + floatValue59 * floatValue48;
            float floatValue62 = floatValue54 * floatValue58;
            float floatValue63 = floatValue55 * floatValue58;
            float floatValue64 = floatValue56 * floatValue58;
            float floatValue65 = floatValue57 * floatValue58;
            this.invoke9(
               vertexConsumer,
               matrix4f,
               matrix3f,
               floatValue54 * floatValue46,
               floatValue49,
               floatValue55 * floatValue46,
               floatValue54,
               0.0F,
               floatValue55,
               floatValue52,
               2.035F,
               intValue43,
               intValue44,
               intValue45,
               floatValue51,
               floatValue56 * floatValue46,
               floatValue49,
               floatValue57 * floatValue46,
               floatValue56,
               0.0F,
               floatValue57,
               floatValue53,
               2.035F,
               intValue43,
               intValue44,
               intValue45,
               floatValue51,
               floatValue56 * floatValue60,
               floatValue61,
               floatValue57 * floatValue60,
               floatValue64,
               floatValue59,
               floatValue65,
               floatValue53,
               2.995F,
               intValue46,
               intValue47,
               intValue48,
               floatValue51 * 0.18F,
               floatValue54 * floatValue60,
               floatValue61,
               floatValue55 * floatValue60,
               floatValue62,
               floatValue59,
               floatValue63,
               floatValue52,
               2.995F,
               intValue46,
               intValue47,
               intValue48,
               floatValue51 * 0.18F
            );
         }
      }
   }

   private void invoke8(
      VertexConsumer vertexConsumer,
      Matrix4f matrix4f,
      Matrix3f matrix3f,
      float f,
      float g,
      float h,
      float i,
      float j,
      float k,
      float l,
      float m,
      int n,
      int o,
      int p,
      float q,
      float r,
      float s,
      float t,
      float u,
      float v,
      float w,
      float x,
      float y,
      int z,
      int aa,
      int ab,
      float ac,
      float ad,
      float ae,
      float af,
      float ag,
      float ah,
      float ai,
      float aj,
      float ak,
      int al,
      int am,
      int an,
      float ao
   ) {
      invoke10(vertexConsumer, matrix4f, matrix3f, f, g, h, i, j, k, l, m, n, o, p, q);
      invoke10(vertexConsumer, matrix4f, matrix3f, r, s, t, u, v, w, x, y, z, aa, ab, ac);
      invoke10(vertexConsumer, matrix4f, matrix3f, ad, ae, af, ag, ah, ai, aj, ak, al, am, an, ao);
   }

   private void invoke9(
      VertexConsumer vertexConsumer,
      Matrix4f matrix4f,
      Matrix3f matrix3f,
      float f,
      float g,
      float h,
      float i,
      float j,
      float k,
      float l,
      float m,
      int n,
      int o,
      int p,
      float q,
      float r,
      float s,
      float t,
      float u,
      float v,
      float w,
      float x,
      float y,
      int z,
      int aa,
      int ab,
      float ac,
      float ad,
      float ae,
      float af,
      float ag,
      float ah,
      float ai,
      float aj,
      float ak,
      int al,
      int am,
      int an,
      float ao,
      float ap,
      float aq,
      float ar,
      float as,
      float at,
      float au,
      float av,
      float aw,
      int ax,
      int ay,
      int az,
      float ba
   ) {
      this.invoke8(
         vertexConsumer,
         matrix4f,
         matrix3f,
         f,
         g,
         h,
         i,
         j,
         k,
         l,
         m,
         n,
         o,
         p,
         q,
         r,
         s,
         t,
         u,
         v,
         w,
         x,
         y,
         z,
         aa,
         ab,
         ac,
         ad,
         ae,
         af,
         ag,
         ah,
         ai,
         aj,
         ak,
         al,
         am,
         an,
         ao
      );
      this.invoke8(
         vertexConsumer,
         matrix4f,
         matrix3f,
         f,
         g,
         h,
         i,
         j,
         k,
         l,
         m,
         n,
         o,
         p,
         q,
         ad,
         ae,
         af,
         ag,
         ah,
         ai,
         aj,
         ak,
         al,
         am,
         an,
         ao,
         ap,
         aq,
         ar,
         as,
         at,
         au,
         av,
         aw,
         ax,
         ay,
         az,
         ba
      );
   }

   private static void invoke10(
      VertexConsumer vertexConsumer,
      Matrix4f matrix4f,
      Matrix3f matrix3f,
      float f,
      float g,
      float h,
      float i,
      float j,
      float k,
      float l,
      float m,
      int n,
      int o,
      int p,
      float q
   ) {
      float floatValue66 = matrix4f.m00() * f + matrix4f.m10() * g + matrix4f.m20() * h + matrix4f.m30();
      float floatValue67 = matrix4f.m01() * f + matrix4f.m11() * g + matrix4f.m21() * h + matrix4f.m31();
      float floatValue68 = matrix4f.m02() * f + matrix4f.m12() * g + matrix4f.m22() * h + matrix4f.m32();
      float floatValue69 = matrix3f.m00() * i + matrix3f.m10() * j + matrix3f.m20() * k;
      float floatValue70 = matrix3f.m01() * i + matrix3f.m11() * j + matrix3f.m21() * k;
      float floatValue71 = matrix3f.m02() * i + matrix3f.m12() * j + matrix3f.m22() * k;
      float floatValue72 = measure(floatValue69 * floatValue69 + floatValue70 * floatValue70 + floatValue71 * floatValue71);
      vertexConsumer.vertex(floatValue66, floatValue67, floatValue68)
         .texture(l, m)
         .color(n, o, p, compute(Math.round(measure2(q, 0.0F, 1.0F) * 255.0F)))
         .normal(floatValue69 * floatValue72, floatValue70 * floatValue72, floatValue71 * floatValue72);
   }

   private static float measure(float f) {
      return f <= 1.0E-6F ? 1.0F : (float)(1.0 / Math.sqrt(f));
   }

   private static float measure2(float f, float g, float h) {
      if (f < g) {
         return g;
      } else {
         return f > h ? h : f;
      }
   }

   private static int compute(int i) {
      if (i < 0) {
         return 0;
      } else {
         return i > 255 ? 255 : i;
      }
   }

   private static int compute2(int i) {
      return i >> 16 & 0xFF;
   }

   private static int compute3(int i) {
      return i >> 8 & 0xFF;
   }

   private static int compute4(int i) {
      return i & 0xFF;
   }

   static int compute5(int i, float f) {
      float floatValue73 = measure2(f, 0.0F, 1.0F);
      int intValue52 = compute2(i);
      int intValue53 = compute3(i);
      int intValue54 = compute4(i);
      int intValue55 = Math.min(255, Math.round(intValue52 + (255 - intValue52) * floatValue73));
      int intValue56 = Math.min(255, Math.round(intValue53 + (255 - intValue53) * floatValue73));
      int intValue57 = Math.min(255, Math.round(intValue54 + (255 - intValue54) * floatValue73));
      return intValue55 << 16 | intValue56 << 8 | intValue57;
   }

   private static int compute6(int i, int j, float f) {
      float floatValue74 = measure2(f, 0.0F, 1.0F);
      int intValue58 = Math.round(compute2(i) + (compute2(j) - compute2(i)) * floatValue74);
      int intValue59 = Math.round(compute3(i) + (compute3(j) - compute3(i)) * floatValue74);
      int intValue60 = Math.round(compute4(i) + (compute4(j) - compute4(i)) * floatValue74);
      return intValue58 << 16 | intValue59 << 8 | intValue60;
   }

   static int compute7(int[] is, float f) {
      float floatValue75 = f - (float)Math.floor(f);
      float floatValue76 = floatValue75 * (is.length - 1);
      int intValue61 = Math.min(is.length - 2, Math.max(0, (int)Math.floor(floatValue76)));
      return compute6(is[intValue61] & 16777215, is[intValue61 + 1] & 16777215, floatValue76 - intValue61);
   }

   static int[] resolve(Theme theme) {
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

   static {
      for (int intValue62 = 0; intValue62 <= 256; intValue62++) {
         float floatValue77 = (float) (Math.PI * 2) * intValue62 / 256.0F;
         FLOATS[intValue62] = (float)Math.sin(floatValue77);
         FLOATS_2[intValue62] = (float)Math.cos(floatValue77);
      }

      for (int intValue63 = 0; intValue63 <= 18; intValue63++) {
         float floatValue78 = (float) (Math.PI * 2) * intValue63 / 18.0F;
         FLOATS_3[intValue63] = (float)Math.sin(floatValue78);
         FLOATS_4[intValue63] = (float)Math.cos(floatValue78);
      }
   }

   static final class ChinaHatState {
      int intValue;
      int intValue2;

      void invoke(float f) {
         int intValue64 = fun.enoughvisuals.systems.modules.modules.client.CustomTheme.hudAccent() & 16777215;
         this.intValue = ChinaHat.compute5(intValue64, 0.18F);
         this.intValue2 = intValue64;
      }
   }
}
