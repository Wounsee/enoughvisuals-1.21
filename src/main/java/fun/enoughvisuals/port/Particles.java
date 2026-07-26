package fun.enoughvisuals.port;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Generated;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap.Type;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import fun.enoughvisuals.port.api.Module;
import fun.enoughvisuals.port.api.ModuleRegister;

@ModuleRegister(
   name = "Particles",
   description = "Улучшенные частицы при атаках и бросках",
   category = Category.Visuals
)
public class Particles extends Module {
   public static GroupSetting spavnitPri = new GroupSetting(
      "Спавнить при", new BooleanSetting("Атаке", true), new BooleanSetting("Бросок", true), new BooleanSetting("В мире", false)
   );
   public static ModeSetting tipChastits = new ModeSetting(
      "Тип частиц", "Bloom", "Bloom", "Star", "Snow", "Heart", "Dollar", "Triangle", "Sakura", "Genshin", "Rhombus"
   );
   public static NumberSetting razmer = new NumberSetting("Размер", 0.5F, 0.0F, 1.0F, 0.1F, false);
   public static NumberSetting kolichestvo = new NumberSetting("Количество", 10.0F, 10.0F, 100.0F, 10.0F, false);
   public static NumberSetting vremyaZhizni = new NumberSetting("Время жизни", 2.0F, 0.5F, 10.0F, 0.5F, false);
   public static NumberSetting radiusVMire = new NumberSetting("Радиус в мире", 12.0F, 2.0F, 50.0F, 1.0F, false);
   public static BooleanSetting fizika = new BooleanSetting("Физика", true);
   public static ModeSetting rezhimTsveta = new ModeSetting("Режим цвета", "Клиентовский", "Клиентовский", "Свой");
   public static ColorSetting kastomTsvet = new ColorSetting("Кастом цвет", 15.0F, 1.0F, 1.0F).setVisibilityCondition(() -> !rezhimTsveta.is("Свой"));
   private static final int INT_VALUE = 1024;
   private long timestamp = System.nanoTime();
   private static final String PORTED = "enoughfx";
   private static final Map<Particles.ParticlesState2, RenderLayer> VALUES_BY_KEY = new ConcurrentHashMap<>();
   private final List<Particles.ParticlesState> items = new ArrayList<>();
   private final List<Particles.ParticlesState> items2 = new ArrayList<>();
   private final List<Particles.ParticlesState> items3 = new ArrayList<>();
   private static final Vector3f VECTOR3F = new Vector3f(0.0F, 0.0F, 1.0F);

   public Particles() {
      this.addSettings(
         new Setting[]{spavnitPri, tipChastits, rezhimTsveta, kastomTsvet, razmer, kolichestvo, vremyaZhizni, radiusVMire, fizika}
      );
   }

   private void invoke() {
      this.items.clear();
      this.items3.clear();
      this.items2.clear();
   }

   private void invoke2(List<Particles.ParticlesState> list, Vec3d vec3d, Vec3d vec3d2) {
      float floatValue = 0.05F + razmer.getValue() * 0.2F;
      int intValue = rezhimTsveta.is("Свой") ? kastomTsvet.getColor().getRGB() : ColorUtils.compute40(list.size() * 100);
      String text = tipChastits.getValue();

      Particles.ParticlesState2 particlesState2 = switch (text) {
         case "Heart" -> Particles.ParticlesState2.HEART;
         case "Star" -> Particles.ParticlesState2.STAR;
         case "Snow" -> Particles.ParticlesState2.SNOW;
         case "Bloom" -> Particles.ParticlesState2.BLOOM;
         case "Dollar" -> Particles.ParticlesState2.DOLLAR;
         case "Triangle" -> Particles.ParticlesState2.TRIANGLE;
         case "Sakura" -> Particles.ParticlesState2.SAKURA;
         case "Genshin" -> Particles.ParticlesState2.GEMINI;
         case "Rhombus" -> Particles.ParticlesState2.SIMS;
         default -> Particles.ParticlesState2.BLOOM;
      };
      list.add(
         new Particles.ParticlesState(
            particlesState2,
            vec3d.add(0.0, floatValue, 0.0),
            vec3d2,
            list.size(),
            (int)MathUtils.measure25(MathUtils.measure19(0.0F, 360.0F), 15.0),
            intValue,
            floatValue,
            0.2F
         )
      );
   }

   @EventHandler
   public void onAttackEntity(AttackEntityEvent attackEntityEvent) {
      Entity entity = attackEntityEvent.getEntity();
      float floatValue2 = 6.0F;
      if (spavnitPri.isEnabled("Атаке")) {
         int intValue2 = (int)kolichestvo.getValue();

         for (int intValue3 = 0; intValue3 < intValue2; intValue3++) {
            this.invoke2(
               this.items,
               new Vec3d(entity.getX(), entity.getY() + MathUtils.measure19(0.0F, entity.getHeight()), entity.getZ()),
               new Vec3d(MathUtils.measure19(-floatValue2, floatValue2), MathUtils.measure19(-floatValue2, floatValue2), MathUtils.measure19(-floatValue2, floatValue2))
            );
         }
      }
   }

   @EventHandler
   public void onPlayerMotion(PlayerMotionEvent playerMotionEvent) {
      if (spavnitPri.isEnabled("Бросок")) {
         if (CLIENT.world == null) {
            return;
         }

         for (Entity entity2 : CLIENT.world.getEntities()) {
            if ((entity2 instanceof EnderPearlEntity || entity2 instanceof ArrowEntity || entity2 instanceof TridentEntity)
               && (!(entity2 instanceof TridentEntity tridentEntity) || !tridentEntity.isOnGround())) {
               boolean flag = entity2.prevX != entity2.getX() || entity2.prevY != entity2.getY() || entity2.prevZ != entity2.getZ();
               if (flag) {
                  Vec3d vec3d3 = entity2.getPos();
                  int intValue4 = Math.max(1, (int)(kolichestvo.getValue() / 10.0F));

                  for (int intValue5 = 0; intValue5 < intValue4; intValue5++) {
                     this.invoke2(
                        this.items3,
                        new Vec3d(
                           vec3d3.x + MathHelper.nextDouble(Random.create(), -0.2, 0.2),
                           vec3d3.y + MathHelper.nextDouble(Random.create(), -0.2, 0.2),
                           vec3d3.z + MathHelper.nextDouble(Random.create(), -0.2, 0.2)
                        ),
                        new Vec3d(
                           MathHelper.nextDouble(Random.create(), -1.0, 1.0),
                           MathHelper.nextDouble(Random.create(), -0.3, 0.3),
                           MathHelper.nextDouble(Random.create(), -1.0, 1.0)
                        )
                     );
                  }
               }
            }
         }
      }

      if (spavnitPri.isEnabled("В мире")) {
         if (CLIENT.world == null || CLIENT.player == null) {
            return;
         }

         int intValue6 = (int)radiusVMire.getValue();
         int intValue7 = Math.max(1, (int)(kolichestvo.getValue() / 2.0F));

         for (int intValue8 = 0; intValue8 < intValue7; intValue8++) {
            Vec3d vec3d4 = CLIENT.player
               .getPos()
               .add(MathUtils.measure19((float)(-intValue6), (float)intValue6), 0.0, MathUtils.measure19((float)(-intValue6), (float)intValue6));
            BlockPos blockPos = CLIENT.world.getTopPosition(Type.MOTION_BLOCKING, BlockPos.ofFloored(vec3d4));
            double doubleValue = blockPos.getX() + MathUtils.measure19(0.0F, 1.0F);
            double doubleValue2 = blockPos.getZ() + MathUtils.measure19(0.0F, 1.0F);
            double doubleValue3 = CLIENT.player.getY() + MathUtils.measure19(CLIENT.player.getHeight(), (float)intValue6);
            Vec3d vec3d5 = new Vec3d(doubleValue, doubleValue3, doubleValue2);

            while (!CLIENT.world.isAir(BlockPos.ofFloored(vec3d5)) && vec3d5.y < CLIENT.world.getTopYInclusive()) {
               vec3d5 = vec3d5.add(0.0, 1.0, 0.0);
            }

            this.invoke2(
               this.items2,
               vec3d5,
               new Vec3d(
                  CLIENT.player.getVelocity().x + MathUtils.measure19(-2.0F, 2.0F),
                  MathUtils.measure18(-0.2, 0.2),
                  CLIENT.player.getVelocity().z + MathUtils.measure19(-2.0F, 2.0F)
               )
            );
         }
      }

      long longValue = this.compute();
      this.invoke3(this.items, longValue);
      this.invoke3(this.items3, longValue);
      this.invoke3(this.items2, longValue);
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      MatrixStack matrices = render3DEvent.getMatrixStack();
      Vec3d vec3d6 = CLIENT.gameRenderer.getCamera().getPos();
      long longValue2 = System.nanoTime();
      double doubleValue4 = (longValue2 - this.timestamp) / 1.0E9;
      this.timestamp = longValue2;
      BufferAllocator bufferAllocator = new BufferAllocator(262144);
      Immediate immediate2 = VertexConsumerProvider.immediate(bufferAllocator);

      try {
         long longValue3 = this.compute();
         long longValue4 = Math.min(400L, Math.max(100L, longValue3 / 5L));
         long longValue5 = Math.max(longValue4 + 1L, (long)((float)longValue3 * 0.62F));
         this.invoke4(matrices, immediate2, vec3d6, this.items, longValue4, longValue5, doubleValue4);
         this.invoke4(matrices, immediate2, vec3d6, this.items3, longValue4, longValue5, doubleValue4);
         this.invoke4(matrices, immediate2, vec3d6, this.items2, longValue4, longValue5, doubleValue4);
         immediate2.draw();
      } finally {
         bufferAllocator.close();
      }
   }

   private long compute() {
      return Math.max(250L, (long)(vremyaZhizni.getValue() * 1000.0F));
   }

   private void invoke3(List<Particles.ParticlesState> list, long l) {
      list.removeIf(particlesState -> particlesState.getStopwatch().check((double)l));
   }

   private void invoke4(MatrixStack matrixStack, Immediate immediate, Vec3d vec3d, List<Particles.ParticlesState> list, long l, long m, double d) {
      if (!list.isEmpty()) {
         matrixStack.push();

         for (Particles.ParticlesState particlesState3 : list) {
            particlesState3.invoke(fizika.isEnabled(), d);
            boolean flag2 = !particlesState3.getStopwatch().check((double)l);
            boolean flag3 = particlesState3.getStopwatch().check((double)m);
            if (flag2) {
               particlesState3.getAnimation().resolve4(1.0, 0.4, LegacyEasingFunctions.LEGACY_EASING_FUNCTION_3, true);
            } else if (flag3) {
               particlesState3.getAnimation().resolve4(0.0, 0.4, LegacyEasingFunctions.LEGACY_EASING_FUNCTION_3, true);
            }

            if (particlesState3.animation.check2()) {
               particlesState3.animation.check();
            }

            float floatValue3 = particlesState3.animation.measure3();
            int intValue9 = (int)(floatValue3 * 255.0F);
            if (intValue9 > 0) {
               int intValue10 = ColorUtils.compute29(particlesState3.getIntValue3(), intValue9);
               Vec3d vec3d7 = particlesState3.getVec3d();
               this.invoke5(matrixStack, immediate, particlesState3, (float)vec3d7.x, (float)vec3d7.y, (float)vec3d7.z, particlesState3.floatValue, intValue10, intValue9);
            }
         }

         matrixStack.pop();
      }
   }

   private void invoke5(MatrixStack matrixStack, Immediate immediate, Particles.ParticlesState particlesState4, float f, float g, float h, float i, int j, int k) {
      matrixStack.push();
      RenderManager.invoke76(matrixStack, f, g, h);
      matrixStack.multiply(CLIENT.gameRenderer.getCamera().getRotation());
      RenderLayer renderLayer = VALUES_BY_KEY.computeIfAbsent(
         particlesState4.getParticlesState2(),
         particlesState22 -> {
            Identifier identifier = particlesState22.getIdentifier();
            return ParticlesRenderLayer.resolve(identifier);
         }
      );
      Entry entry = matrixStack.peek();
      Matrix4f matrix4f2 = entry.getPositionMatrix();
      Matrix3f matrix3f2 = entry.getNormalMatrix();
      VertexConsumer vertexConsumer2 = immediate.getBuffer(renderLayer);
      this.invoke6(vertexConsumer2, matrix4f2, matrix3f2, -i, -i, i * 2.0F, i * 2.0F, j, k);
      if (particlesState4.particlesState2 == Particles.ParticlesState2.BLOOM) {
         this.invoke6(vertexConsumer2, matrix4f2, matrix3f2, -i / 2.0F, -i / 2.0F, i, i, j, k);
      }

      matrixStack.pop();
   }

   private void invoke6(VertexConsumer vertexConsumer, Matrix4f matrix4f, Matrix3f matrix3f, float f, float g, float h, float i, int j, int k) {
      int intValue11 = j >> 16 & 0xFF;
      int intValue12 = j >> 8 & 0xFF;
      int intValue13 = j & 0xFF;
      VECTOR3F.set(0.0F, 0.0F, 1.0F);
      matrix3f.transform(VECTOR3F);
      VECTOR3F.normalize();
      float floatValue4 = f + h;
      float floatValue5 = g + i;
      vertexConsumer.vertex(matrix4f, f, g, 0.0F)
         .color(intValue11, intValue12, intValue13, k)
         .texture(0.0F, 1.0F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(VECTOR3F.x, VECTOR3F.y, VECTOR3F.z);
      vertexConsumer.vertex(matrix4f, floatValue4, g, 0.0F)
         .color(intValue11, intValue12, intValue13, k)
         .texture(1.0F, 1.0F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(VECTOR3F.x, VECTOR3F.y, VECTOR3F.z);
      vertexConsumer.vertex(matrix4f, floatValue4, floatValue5, 0.0F)
         .color(intValue11, intValue12, intValue13, k)
         .texture(1.0F, 0.0F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(VECTOR3F.x, VECTOR3F.y, VECTOR3F.z);
      vertexConsumer.vertex(matrix4f, f, floatValue5, 0.0F)
         .color(intValue11, intValue12, intValue13, k)
         .texture(0.0F, 0.0F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(VECTOR3F.x, VECTOR3F.y, VECTOR3F.z);
   }

   @Override
   public void toggle() {
      super.toggle();
      this.invoke();
   }

   @EventHandler
   public void onWorldJoin(WorldJoinEvent worldJoinEvent) {
      this.invoke();
   }

   public static class ParticlesState {
      private Box box;
      final Particles.ParticlesState2 particlesState2;
      private Vec3d vec3d;
      private Vec3d vec3d2;
      private final int intValue;
      private final int intValue2;
      private final int intValue3;
      final float floatValue;
      private static final double DOUBLE_VALUE = 0.05;
      private static final double DOUBLE_VALUE_2 = 0.0035;
      private static final double DOUBLE_VALUE_3 = 0.985;
      private static final double DOUBLE_VALUE_4 = 0.55;
      private static final double DOUBLE_VALUE_5 = 0.72;
      private static final double DOUBLE_VALUE_6 = 0.003;
      private static final double DOUBLE_VALUE_7 = 1.0E-6;
      private final double doubleValue;
      private final Stopwatch stopwatch = new Stopwatch();
      final Animation animation = new Animation();

      public ParticlesState(Particles.ParticlesState2 particlesState23, Vec3d vec3d, Vec3d vec3d2, int i, int j, int k, float f, double d) {
         double doubleValue5 = f / 2.0;
         this.box = new Box(new Vec3d(vec3d.x - doubleValue5, vec3d.y - doubleValue5, vec3d.z - doubleValue5), new Vec3d(vec3d.x + doubleValue5, vec3d.y + doubleValue5, vec3d.z + doubleValue5));
         this.particlesState2 = particlesState23;
         this.vec3d = vec3d;
         this.vec3d2 = vec3d2.multiply(0.05);
         this.intValue = i;
         this.intValue2 = j;
         this.intValue3 = k;
         this.floatValue = f;
         this.doubleValue = d;
         this.stopwatch.invoke();
      }

      public void invoke(boolean bl, double d) {
         double doubleValue6 = d * 60.0 * this.doubleValue;
         if (bl && Module.CLIENT.world != null) {
            this.vec3d2 = this.vec3d2.multiply(Math.pow(0.985, d * 60.0)).subtract(0.0, 0.0035 * d * 60.0, 0.0);
            this.invoke2(this.vec3d2.x * doubleValue6, 0);
            this.invoke2(this.vec3d2.y * doubleValue6, 1);
            this.invoke2(this.vec3d2.z * doubleValue6, 2);
         } else {
            this.vec3d = this.vec3d.add(this.vec3d2.multiply(doubleValue6));
            this.invoke4();
         }
      }

      private void invoke2(double d, int i) {
         if (!(Math.abs(d) <= 1.0E-6)) {
            Box box2 = switch (i) {
               case 0 -> this.box.offset(d, 0.0, 0.0);
               case 1 -> this.box.offset(0.0, d, 0.0);
               default -> this.box.offset(0.0, 0.0, d);
            };
            if (this.check(box2)) {
               this.invoke3(i);
            } else {
               this.box = box2;

               this.vec3d = switch (i) {
                  case 0 -> this.vec3d.add(d, 0.0, 0.0);
                  case 1 -> this.vec3d.add(0.0, d, 0.0);
                  default -> this.vec3d.add(0.0, 0.0, d);
               };
            }
         }
      }

      private void invoke3(int i) {
         double doubleValue7 = this.vec3d2.x;
         double doubleValue8 = this.vec3d2.y;
         double doubleValue9 = this.vec3d2.z;
         switch (i) {
            case 0:
               doubleValue7 = -doubleValue7 * 0.55;
               break;
            case 1:
               if (doubleValue8 < 0.0) {
                  doubleValue7 *= 0.72;
                  doubleValue9 *= 0.72;
               }

               doubleValue8 = -doubleValue8 * 0.55;
               break;
            default:
               doubleValue9 = -doubleValue9 * 0.55;
         }

         this.vec3d2 = new Vec3d(this.measure(doubleValue7), this.measure(doubleValue8), this.measure(doubleValue9));
      }

      private double measure(double d) {
         return Math.abs(d) < 0.003 ? 0.0 : d;
      }

      private boolean check(Box box) {
         int intValue14 = MathHelper.floor(box.minX + 1.0E-6);
         int intValue15 = MathHelper.floor(box.minY + 1.0E-6);
         int intValue16 = MathHelper.floor(box.minZ + 1.0E-6);
         int intValue17 = MathHelper.floor(box.maxX - 1.0E-6);
         int intValue18 = MathHelper.floor(box.maxY - 1.0E-6);
         int intValue19 = MathHelper.floor(box.maxZ - 1.0E-6);
         Mutable mutable = new Mutable();

         for (int intValue20 = intValue14; intValue20 <= intValue17; intValue20++) {
            for (int intValue21 = intValue15; intValue21 <= intValue18; intValue21++) {
               for (int intValue22 = intValue16; intValue22 <= intValue19; intValue22++) {
                  mutable.set(intValue20, intValue21, intValue22);
                  VoxelShape voxelShape = Module.CLIENT.world.getBlockState(mutable).getCollisionShape(Module.CLIENT.world, mutable);
                  if (!voxelShape.isEmpty()) {
                     for (Box box3 : voxelShape.getBoundingBoxes()) {
                        if (box.intersects(box3.offset(intValue20, intValue21, intValue22))) {
                           return true;
                        }
                     }
                  }
               }
            }
         }

         return false;
      }

      private void invoke4() {
         double doubleValue10 = this.floatValue / 2.0;
         this.box = new Box(
            new Vec3d(this.vec3d.x - doubleValue10, this.vec3d.y - doubleValue10, this.vec3d.z - doubleValue10),
            new Vec3d(this.vec3d.x + doubleValue10, this.vec3d.y + doubleValue10, this.vec3d.z + doubleValue10)
         );
      }

      @Generated
      public Box getBox() {
         return this.box;
      }

      @Generated
      public Particles.ParticlesState2 getParticlesState2() {
         return this.particlesState2;
      }

      @Generated
      public Vec3d getVec3d() {
         return this.vec3d;
      }

      @Generated
      public Vec3d getVec3d2() {
         return this.vec3d2;
      }

      @Generated
      public int getIntValue() {
         return this.intValue;
      }

      @Generated
      public int getIntValue2() {
         return this.intValue2;
      }

      @Generated
      public int getIntValue3() {
         return this.intValue3;
      }

      @Generated
      public float getFloatValue() {
         return this.floatValue;
      }

      @Generated
      public double getDoubleValue() {
         return this.doubleValue;
      }

      @Generated
      public Stopwatch getStopwatch() {
         return this.stopwatch;
      }

      @Generated
      public Animation getAnimation() {
         return this.animation;
      }
   }

   static enum ParticlesState2 {
      HEART("heart", false),
      STAR("star", false),
      SNOW("snowflake", false),
      BLOOM("firefly", false),
      DOLLAR("dollar", false),
      TRIANGLE("triangle", false),
      SAKURA("sakura", false),
      GEMINI("genshin", false),
      SIMS("rhombus", false);

      private final Identifier identifier;
      private final boolean flag;

      private ParticlesState2(String string2, boolean bl) {
         this.identifier = Identifier.of("enoughfx", "textures/world/" + string2 + ".png");
         this.flag = bl;
      }

      @Generated
      public Identifier getIdentifier() {
         return this.identifier;
      }

      @Generated
      public boolean isFlag() {
         return this.flag;
      }
   }
}
