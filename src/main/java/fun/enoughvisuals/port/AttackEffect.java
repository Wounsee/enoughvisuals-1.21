package fun.enoughvisuals.port;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import fun.enoughvisuals.port.api.Module;
import fun.enoughvisuals.port.api.ModuleRegister;

@ModuleRegister(
   name = "AttackEffect",
   description = "Красивые эффекты в точке удара",
   category = Category.Visuals
)
public class AttackEffect extends Module {
   private static final String TORUS = "Торус";
   private static final String PLAZMA = "Плазма";
   private static final int INT_VALUE = 6061311;
   private static final int INT_VALUE_2 = 6748116;
   private static final long TIMESTAMP = 30L;
   private static final double DOUBLE_VALUE = 9.0;
   public final ModeSetting rezhim = new ModeSetting("Режим", "Торус", "Торус", "Плазма");
   public final NumberSetting radiusVolny = new NumberSetting("Радиус волны", 1.8F, 0.1F, 3.5F, 0.05F, false)
      .setVisibilityCondition(() -> !this.rezhim.is("Торус"));
   public final NumberSetting silaPrelomleniya = new NumberSetting("Сила преломления", 0.06F, 0.01F, 0.16F, 0.001F, false)
      .setVisibilityCondition(() -> !this.rezhim.is("Торус"));
   public final NumberSetting dlitelnost = new NumberSetting("Длительность", 420.0F, 200.0F, 1600.0F, 10.0F, false)
      .setVisibilityCondition(() -> !this.rezhim.is("Торус"));
   public final BooleanSetting spektralnayaDispersiya = new BooleanSetting("Спектральная дисперсия", true).visibleWhen(() -> !this.rezhim.is("Торус"));
   public final BooleanSetting neSkvozIgroka = new BooleanSetting("Не сквозь игрока", true).visibleWhen(() -> !this.rezhim.is("Торус"));
   public final NumberSetting radiusPlazmy = new NumberSetting("Радиус плазмы", 1.4F, 1.0F, 3.0F, 0.05F, false)
      .setVisibilityCondition(() -> !this.rezhim.is("Плазма"));
   public final NumberSetting temperaturaPlazmy = new NumberSetting("Температура плазмы", 1.0F, 0.5F, 2.0F, 0.05F, false)
      .setVisibilityCondition(() -> !this.rezhim.is("Плазма"));
   public final NumberSetting yarkostPlazmy = new NumberSetting("Яркость плазмы", 0.8F, 0.3F, 1.6F, 0.02F, false)
      .setVisibilityCondition(() -> !this.rezhim.is("Плазма"));
   public final NumberSetting dlitelnostPlazmy = new NumberSetting("Длительность плазмы", 480.0F, 220.0F, 900.0F, 10.0F, false)
      .setVisibilityCondition(() -> !this.rezhim.is("Плазма"));
   private final List<HitRefractionEffect> items = new CopyOnWriteArrayList<>();
   private final List<PlasmaPinchEffect> items2 = new CopyOnWriteArrayList<>();
   private long timestamp;
   private int intValue = Integer.MIN_VALUE;
   private int intValue2;

   public AttackEffect() {
      this.addSettings(
         new Setting[]{
            this.rezhim,
            this.radiusVolny,
            this.silaPrelomleniya,
            this.dlitelnost,
            this.spektralnayaDispersiya,
            this.neSkvozIgroka,
            this.radiusPlazmy,
            this.temperaturaPlazmy,
            this.yarkostPlazmy,
            this.dlitelnostPlazmy
         }
      );
   }

   @EventHandler
   public void onAttackEntity(AttackEntityEvent attackEntityEvent) {
      if (attackEntityEvent != null) {
         this.invoke(attackEntityEvent.getEntity());
      }
   }

   public void invoke(Entity entity) {
      if (this.enabled && entity != null && CLIENT.player != null && CLIENT.world != null) {
         if (entity != CLIENT.player) {
            long longValue = System.currentTimeMillis();
            if (entity.getId() != this.intValue || longValue - this.timestamp >= 30L) {
               this.intValue = entity.getId();
               this.timestamp = longValue;
               Vec3d vec3d2 = CLIENT.player.getRotationVec(1.0F).normalize();
               if (this.rezhim.is("Плазма")) {
                  this.invoke3(entity, vec3d2, longValue);
               } else {
                  this.invoke2(entity, vec3d2, longValue);
               }
            }
         }
      }
   }

   private void invoke2(Entity entity, Vec3d vec3d, long l) {
      Vec3d vec3d3 = entity.getBoundingBox().getCenter();
      HitRefractionEffect hitRefractionEffect = new HitRefractionEffect(
         vec3d3, vec3d, l, (long)this.dlitelnost.value, this.radiusVolny.value, this.silaPrelomleniya.value, this.compute2()
      );
      this.items.add(hitRefractionEffect);

      while (this.items.size() > 10) {
         this.items.remove(0);
      }
   }

   private void invoke3(Entity entity, Vec3d vec3d, long l) {
      Vec3d vec3d4 = this.resolve(entity, vec3d);
      this.intValue2 = this.intValue2 + 1 & 1023;
      float floatValue = this.intValue2 * 7.31F % 41.0F;
      PlasmaPinchEffect plasmaPinchEffect = new PlasmaPinchEffect(
         vec3d4,
         vec3d,
         l,
         (long)this.dlitelnostPlazmy.value,
         this.radiusPlazmy.value,
         this.temperaturaPlazmy.value,
         this.yarkostPlazmy.value,
         floatValue,
         this.compute()
      );
      this.items2.add(plasmaPinchEffect);

      while (this.items2.size() > 12) {
         this.items2.remove(0);
      }
   }

   private Vec3d resolve(Entity entity, Vec3d vec3d) {
      if (CLIENT.crosshairTarget instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == entity) {
         return entityHitResult.getPos();
      } else {
         Box box = entity.getBoundingBox();
         Vec3d vec3d5 = CLIENT.player.getEyePos();
         Vec3d vec3d6 = vec3d5.add(vec3d.multiply(9.0));
         return box.raycast(vec3d5, vec3d6).orElse(box.getCenter());
      }
   }

   @EventHandler
   public void onWorldRenderContext(WorldRenderContextEvent worldRenderContextEvent) {
      if (this.enabled && worldRenderContextEvent != null) {
         long longValue2 = System.currentTimeMillis();
         this.items.removeIf(hitRefractionEffect2 -> hitRefractionEffect2.check(longValue2));
         this.items2.removeIf(plasmaPinchEffect2 -> plasmaPinchEffect2.check(longValue2));
         if (!this.items.isEmpty() || !this.items2.isEmpty()) {
            Camera camera = worldRenderContextEvent.getWorldRenderCapture().getCamera();
            if (camera != null) {
               Vec3d vec3d7 = camera.getPos();
               Matrix4f matrix4f = worldRenderContextEvent.resolve2();
               Matrix4f matrix4f2 = worldRenderContextEvent.resolve3();
               float floatValue2 = (float)(longValue2 % 100000L) / 1000.0F;
               if (!this.items.isEmpty()) {
                  HitRefractionRenderer.getINSTANCE()
                     .invoke(CLIENT, this.items, matrix4f, matrix4f2, vec3d7, this.spektralnayaDispersiya.isEnabled(), this.neSkvozIgroka.isEnabled(), floatValue2);
               }

               if (!this.items2.isEmpty()) {
                  PlasmaPinchRenderer.getINSTANCE().invoke(CLIENT, this.items2, matrix4f, matrix4f2, vec3d7, floatValue2, false);
               }
            }
         }
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.items.clear();
      this.items2.clear();
   }

   private int compute() {
      return this.compute3(6748116);
   }

   private int compute2() {
      return this.compute3(6061311);
   }

   private int compute3(int i) {
      try {
         return fun.enoughvisuals.systems.modules.modules.client.CustomTheme.hudAccent() & 16777215;
      } catch (Throwable exception) {
      }

      return i;
   }

   private boolean check(int i, int j) {
      int intValue = Math.abs((i >> 16 & 0xFF) - (j >> 16 & 0xFF));
      int intValue2 = Math.abs((i >> 8 & 0xFF) - (j >> 8 & 0xFF));
      int intValue3 = Math.abs((i & 0xFF) - (j & 0xFF));
      return intValue <= 3 && intValue2 <= 3 && intValue3 <= 3;
   }
}
